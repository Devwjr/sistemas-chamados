package br.com.empresa.suporte.web;

import br.com.empresa.suporte.model.*;
import br.com.empresa.suporte.model.Enums.*;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}
    public record Ref(Long id, String nome) {}
    public record UsuarioView(Long id, String nome, String email, Perfil perfil, Ref setor, boolean ativo, boolean trocaSenhaObrigatoria) {
        public static UsuarioView of(Usuario u) { return new UsuarioView(u.id,u.nome,u.email,u.perfil,new Ref(u.setor.id,u.setor.nome),u.ativo,u.trocaSenhaObrigatoria); }
    }
    public record CadastroView(Long id, String nome, String descricao, boolean ativo) {}
    public record CadastroRequest(@NotBlank @Size(min=2,max=100) String nome, @Size(max=500) String descricao, @NotNull Boolean ativo) {}
    public record UsuarioRequest(@NotBlank @Size(min=2,max=120) String nome, @NotBlank @Email @Size(max=200) String email,
        @NotNull Perfil perfil, @NotNull Long setorId, @NotNull Boolean ativo, @Size(min=12,max=72) String senhaTemporaria) {}
    public record SenhaRequest(@NotBlank String senhaAtual, @NotBlank @Size(min=12,max=72) String novaSenha) {}
    public record ResetSenhaRequest(@NotBlank @Size(min=12,max=72) String senhaTemporaria) {}
    public record CriarChamadoRequest(@NotBlank @Size(min=5,max=150) String titulo, @NotBlank @Size(min=10,max=5000) String descricao,
        @NotNull Long categoriaId, @NotNull Impacto impacto) {}
    public record VersaoRequest(@NotNull @PositiveOrZero Long versao) {}
    public record ResponsavelRequest(@NotNull @PositiveOrZero Long versao, @NotNull Long responsavelId) {}
    public record ClassificacaoRequest(@NotNull @PositiveOrZero Long versao, Prioridade prioridade, Long categoriaId) {}
    public record TransicaoRequest(@NotNull @PositiveOrZero Long versao, @NotNull Status destino,
        @Size(max=5000) String solucao, @Size(max=5000) String justificativa, @Size(max=5000) String mensagemPublica) {}
    public record ComentarioRequest(@NotNull @PositiveOrZero Long versao, @NotBlank @Size(max=5000) String conteudo, @NotNull Visibilidade visibilidade) {}
    public record ChamadoView(Long id, String protocolo, String titulo, String descricao, Impacto impacto, Status status,
        Prioridade prioridade, Ref solicitante, Ref setorOrigem, Ref responsavel, Ref categoria, String solucao,
        Instant criadoEm, Instant atualizadoEm, Instant resolvidoEm, Instant fechadoEm, Instant canceladoEm, long versao) {
        public static ChamadoView of(Chamado c) {
            return new ChamadoView(c.id,c.protocolo(),c.titulo,c.descricao,c.impacto,c.status,c.prioridade,
                new Ref(c.solicitante.id,c.solicitante.nome),new Ref(c.setorOrigem.id,c.setorOrigem.nome),
                c.responsavel == null ? null : new Ref(c.responsavel.id,c.responsavel.nome),new Ref(c.categoria.id,c.categoria.nome),
                c.solucao,c.criadoEm,c.atualizadoEm,c.resolvidoEm,c.fechadoEm,c.canceladoEm,c.versao);
        }
    }
    public record ComentarioView(Long id, Ref autor, String conteudo, Visibilidade visibilidade, Instant criadoEm) {
        public static ComentarioView of(Comentario c) { return new ComentarioView(c.id,new Ref(c.autor.id,c.autor.nome),c.conteudo,c.visibilidade,c.criadoEm); }
    }
    public record HistoricoView(Long id, Ref autor, Evento tipoEvento, String valorAnterior, String valorNovo, String justificativa, Instant criadoEm) {
        public static HistoricoView of(Historico h) { return new HistoricoView(h.id,new Ref(h.autor.id,h.autor.nome),h.tipoEvento,h.valorAnterior,h.valorNovo,h.justificativa,h.criadoEm); }
    }
    public record Pagina<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
        public static <T> Pagina<T> of(Page<T> p) { return new Pagina<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages()); }
    }
}
