package br.com.empresa.suporte.service;

import br.com.empresa.suporte.model.*;
import br.com.empresa.suporte.model.Enums.*;
import br.com.empresa.suporte.repository.*;
import br.com.empresa.suporte.web.*;
import br.com.empresa.suporte.web.Dtos.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ChamadoService {
    private final ChamadoRepository chamados;
    private final CategoriaRepository categorias;
    private final UsuarioRepository users;
    private final ComentarioRepository comentarios;
    private final HistoricoRepository historicos;
    private final CurrentUser current;
    private final Clock clock;

    public ChamadoService(
            ChamadoRepository chamados,
            CategoriaRepository categorias,
            UsuarioRepository users,
            ComentarioRepository comentarios,
            HistoricoRepository historicos,
            CurrentUser current,
            Clock clock) {
        this.chamados = chamados;
        this.categorias = categorias;
        this.users = users;
        this.comentarios = comentarios;
        this.historicos = historicos;
        this.current = current;
        this.clock = clock;
    }

    private Specification<Chamado> visible(Usuario u) {
        return (root, q, b) ->
                Workflow.ti(u.perfil)
                        ? b.conjunction()
                        : b.equal(root.get("solicitante").get("id"), u.id);
    }

    private Chamado load(Long id, Usuario u) {
        var c = chamados.findDetalheById(id).orElseThrow(ApiException::notFound);
        if (!Workflow.ti(u.perfil) && !c.solicitante.id.equals(u.id)) throw ApiException.notFound();
        return c;
    }

    private void version(Chamado c, long version) {
        if (c.versao != version)
            throw new ApiException(
                    409, "STALE_VERSION", "O chamado mudou. Recarregue antes de continuar.");
    }

    private void mutable(Chamado c) {
        if (Workflow.terminal(c.status))
            throw ApiException.conflict("Chamados encerrados não podem ser alterados.");
    }

    private void responsible(Chamado c, Usuario u) {
        if (u.perfil != Perfil.ADMIN
                && !(Workflow.ti(u.perfil)
                        && c.responsavel != null
                        && c.responsavel.id.equals(u.id))) throw ApiException.forbidden();
    }

    private void event(
            Chamado c, Usuario u, Evento type, String oldValue, String newValue, String reason) {
        var h = new Historico();
        h.chamado = c;
        h.autor = u;
        h.tipoEvento = type;
        h.valorAnterior = oldValue;
        h.valorNovo = newValue;
        h.justificativa = reason;
        h.criadoEm = clock.instant();
        historicos.save(h);
    }

    private void comment(Chamado c, Usuario u, String text, Visibilidade visibility) {
        var m = new Comentario();
        m.chamado = c;
        m.autor = u;
        m.conteudo = text;
        m.visibilidade = visibility;
        m.criadoEm = clock.instant();
        comentarios.save(m);
    }

    private ChamadoView save(Chamado c) {
        // Sempre altera o pai, inclusive em comentário, para participar do bloqueio otimista.
        Instant now = clock.instant();
        c.atualizadoEm = now.isAfter(c.atualizadoEm) ? now : c.atualizadoEm.plusNanos(1000);
        chamados.saveAndFlush(c);
        return ChamadoView.of(c);
    }

    public ChamadoView detail(Long id) {
        return ChamadoView.of(load(id, current.get()));
    }

    public Pagina<ChamadoView> list(
            String busca,
            Status status,
            Prioridade prioridade,
            Long categoriaId,
            Long responsavelId,
            Long setorId,
            LocalDate de,
            LocalDate ate,
            Pageable page) {
        var u = current.get();
        if (!Workflow.ti(u.perfil) && (responsavelId != null || setorId != null))
            throw ApiException.forbidden();
        if (de != null && ate != null && de.isAfter(ate))
            throw ApiException.invalid("O início do período deve ser anterior ao fim.");
        Specification<Chamado> spec = visible(u);
        if (status != null) spec = spec.and((r, q, b) -> b.equal(r.get("status"), status));
        if (prioridade != null)
            spec = spec.and((r, q, b) -> b.equal(r.get("prioridade"), prioridade));
        if (categoriaId != null)
            spec = spec.and((r, q, b) -> b.equal(r.get("categoria").get("id"), categoriaId));
        if (responsavelId != null)
            spec = spec.and((r, q, b) -> b.equal(r.get("responsavel").get("id"), responsavelId));
        if (setorId != null)
            spec = spec.and((r, q, b) -> b.equal(r.get("setorOrigem").get("id"), setorId));
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        if (de != null)
            spec =
                    spec.and(
                            (r, q, b) ->
                                    b.greaterThanOrEqualTo(
                                            r.get("criadoEm"), de.atStartOfDay(zone).toInstant()));
        if (ate != null)
            spec =
                    spec.and(
                            (r, q, b) ->
                                    b.lessThan(
                                            r.get("criadoEm"),
                                            ate.plusDays(1).atStartOfDay(zone).toInstant()));
        if (busca != null && !busca.isBlank()) {
            if (busca.length() > 150) throw ApiException.invalid("Busca muito longa.");
            String raw = busca.strip();
            String pattern =
                    "%"
                            + raw.toLowerCase(Locale.ROOT)
                                    .replace("\\", "\\\\")
                                    .replace("%", "\\%")
                                    .replace("_", "\\_")
                            + "%";
            Long number = null;
            try {
                number = Long.parseLong(raw.toUpperCase(Locale.ROOT).replaceFirst("^CH-", ""));
            } catch (NumberFormatException ignored) {
            }
            Long protocolId = number;
            spec =
                    spec.and(
                            (r, q, b) ->
                                    protocolId == null
                                            ? b.like(b.lower(r.get("titulo")), pattern, '\\')
                                            : b.or(
                                                    b.like(b.lower(r.get("titulo")), pattern, '\\'),
                                                    b.equal(r.get("id"), protocolId)));
        }
        return Pagina.of(chamados.findAll(spec, page).map(ChamadoView::of));
    }

    @Transactional
    public ChamadoView create(CriarChamadoRequest req) {
        var u = current.get();
        if (!u.setor.ativo)
            throw ApiException.conflict("Seu setor está inativo. Contate a administração.");
        var c = new Chamado();
        c.titulo = Workflow.required(req.titulo(), 5, "Título");
        c.descricao = Workflow.required(req.descricao(), 10, "Descrição");
        c.impacto = req.impacto();
        c.categoria =
                categorias
                        .findById(req.categoriaId())
                        .filter(x -> x.ativo)
                        .orElseThrow(() -> ApiException.invalid("Categoria indisponível."));
        c.solicitante = u;
        c.setorOrigem = u.setor;
        c.criadoEm = clock.instant();
        c.atualizadoEm = c.criadoEm;
        chamados.saveAndFlush(c);
        event(c, u, Evento.ABERTURA, null, Status.ABERTO.name(), null);
        return ChamadoView.of(c);
    }

    @Transactional
    public ChamadoView assume(Long id, VersaoRequest req) {
        var u = current.get();
        if (!Workflow.ti(u.perfil)) throw ApiException.forbidden();
        var c = load(id, u);
        version(c, req.versao());
        if (c.status != Status.ABERTO || c.responsavel != null)
            throw ApiException.conflict("O chamado já foi assumido.");
        assign(c, u, u);
        return save(c);
    }

    private void assign(Chamado c, Usuario target, Usuario actor) {
        String before = c.responsavel == null ? null : c.responsavel.nome;
        c.responsavel = target;
        event(c, actor, Evento.RESPONSAVEL, before, target.nome, null);
        if (c.status == Status.ABERTO) {
            c.status = Status.EM_ATENDIMENTO;
            event(c, actor, Evento.STATUS, Status.ABERTO.name(), c.status.name(), null);
        }
    }

    @Transactional
    public ChamadoView assign(Long id, ResponsavelRequest req) {
        var u = current.get();
        if (u.perfil != Perfil.ADMIN) throw ApiException.forbidden();
        var c = load(id, u);
        version(c, req.versao());
        mutable(c);
        var target =
                users.findById(req.responsavelId())
                        .filter(x -> x.ativo && Workflow.ti(x.perfil))
                        .orElseThrow(() -> ApiException.invalid("Responsável indisponível."));
        assign(c, target, u);
        return save(c);
    }

    @Transactional
    public ChamadoView classify(Long id, ClassificacaoRequest req) {
        var u = current.get();
        var c = load(id, u);
        version(c, req.versao());
        mutable(c);
        responsible(c, u);
        if (req.prioridade() == null && req.categoriaId() == null)
            throw ApiException.invalid("Informe prioridade ou categoria.");
        if (req.prioridade() != null && req.prioridade() != c.prioridade) {
            event(c, u, Evento.PRIORIDADE, c.prioridade.name(), req.prioridade().name(), null);
            c.prioridade = req.prioridade();
        }
        if (req.categoriaId() != null && !req.categoriaId().equals(c.categoria.id)) {
            var target =
                    categorias
                            .findById(req.categoriaId())
                            .filter(x -> x.ativo)
                            .orElseThrow(() -> ApiException.invalid("Categoria indisponível."));
            event(c, u, Evento.CATEGORIA, c.categoria.nome, target.nome, null);
            c.categoria = target;
        }
        return save(c);
    }

    @Transactional
    public ChamadoView transition(Long id, TransicaoRequest req) {
        var u = current.get();
        var c = load(id, u);
        version(c, req.versao());
        boolean owner = c.solicitante.id.equals(u.id),
                assigned =
                        Workflow.ti(u.perfil)
                                && c.responsavel != null
                                && c.responsavel.id.equals(u.id);
        Workflow.checkTransition(
                c.status, req.destino(), u.perfil == Perfil.ADMIN, owner, assigned);
        Status old = c.status;
        String reason = null;
        if (req.destino() == Status.RESOLVIDO) {
            reason = Workflow.required(req.solucao(), 10, "Solução");
            c.solucao = reason;
            c.resolvidoEm = clock.instant();
        } else if (req.solucao() != null)
            throw ApiException.invalid("Solução só é aceita ao resolver.");
        if (req.destino() == Status.CANCELADO
                || (old == Status.RESOLVIDO && req.destino() == Status.EM_ATENDIMENTO))
            reason = Workflow.required(req.justificativa(), 10, "Justificativa");
        else if (req.justificativa() != null)
            throw ApiException.invalid("Justificativa não se aplica a esta transição.");
        if (req.destino() == Status.AGUARDANDO_USUARIO)
            comment(
                    c,
                    u,
                    Workflow.required(req.mensagemPublica(), 10, "Mensagem pública"),
                    Visibilidade.PUBLICO);
        else if (req.mensagemPublica() != null)
            throw ApiException.invalid("Mensagem pública só se aplica à espera pelo usuário.");
        if (old == Status.RESOLVIDO && req.destino() == Status.EM_ATENDIMENTO) {
            if (c.responsavel == null || !c.responsavel.ativo || !Workflow.ti(c.responsavel.perfil))
                throw ApiException.conflict(
                        "Solicite ao administrador um responsável ativo antes de reabrir.");
            c.solucao = null;
            c.resolvidoEm = null;
        }
        c.status = req.destino();
        if (c.status == Status.FECHADO) c.fechadoEm = clock.instant();
        if (c.status == Status.CANCELADO) c.canceladoEm = clock.instant();
        event(c, u, Evento.STATUS, old.name(), c.status.name(), reason);
        return save(c);
    }

    public Pagina<ComentarioView> comments(Long id, Pageable page) {
        var u = current.get();
        load(id, u);
        var result =
                Workflow.ti(u.perfil)
                        ? comentarios.findByChamadoId(id, page)
                        : comentarios.findByChamadoIdAndVisibilidade(
                                id, Visibilidade.PUBLICO, page);
        return Pagina.of(result.map(ComentarioView::of));
    }

    @Transactional
    public ChamadoView addComment(Long id, ComentarioRequest req) {
        var u = current.get();
        var c = load(id, u);
        version(c, req.versao());
        mutable(c);
        if (req.visibilidade() == Visibilidade.INTERNO && !Workflow.ti(u.perfil))
            throw ApiException.forbidden();
        comment(c, u, Workflow.required(req.conteudo(), 1, "Comentário"), req.visibilidade());
        return save(c);
    }

    public Pagina<HistoricoView> history(Long id, Pageable page) {
        load(id, current.get());
        return Pagina.of(historicos.findByChamadoId(id, page).map(HistoricoView::of));
    }

    public Map<String, Object> dashboard() {
        var spec = visible(current.get());
        Map<Status, Long> status = new EnumMap<>(Status.class);
        Map<Prioridade, Long> priorities = new EnumMap<>(Prioridade.class);
        for (var s : Status.values())
            status.put(s, chamados.count(spec.and((r, q, b) -> b.equal(r.get("status"), s))));
        for (var p : Prioridade.values())
            priorities.put(
                    p, chamados.count(spec.and((r, q, b) -> b.equal(r.get("prioridade"), p))));
        long unassigned =
                chamados.count(
                        spec.and(
                                (r, q, b) ->
                                        b.and(
                                                b.isNull(r.get("responsavel")),
                                                b.equal(r.get("status"), Status.ABERTO))));
        return Map.of(
                "porStatus",
                status,
                "porPrioridade",
                priorities,
                "semResponsavel",
                unassigned,
                "total",
                status.values().stream().mapToLong(Long::longValue).sum());
    }
}
