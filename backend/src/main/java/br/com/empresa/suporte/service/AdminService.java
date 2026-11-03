package br.com.empresa.suporte.service;

import br.com.empresa.suporte.model.*;
import br.com.empresa.suporte.model.Enums.*;
import br.com.empresa.suporte.repository.*;
import br.com.empresa.suporte.web.*;
import br.com.empresa.suporte.web.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Clock;
import java.util.*;

@Service @Transactional(readOnly=true)
public class AdminService {
    private final UsuarioRepository users; private final SetorRepository setores; private final CategoriaRepository categorias; private final ChamadoRepository chamados; private final PasswordEncoder encoder; private final CurrentUser current; private final Clock clock;
    public AdminService(UsuarioRepository users,SetorRepository setores,CategoriaRepository categorias,ChamadoRepository chamados,PasswordEncoder encoder,CurrentUser current,Clock clock) {
        this.users=users; this.setores=setores; this.categorias=categorias; this.chamados=chamados; this.encoder=encoder; this.current=current; this.clock=clock;
    }
    public Pagina<UsuarioView> users(Pageable page) { return Pagina.of(users.findAll(page).map(UsuarioView::of)); }
    public Pagina<Ref> eligible(Pageable page) {
        return Pagina.of(users.findAll((r,q,b)->b.and(b.isTrue(r.get("ativo")),r.get("perfil").in(Perfil.TECNICO,Perfil.ADMIN)),page).map(u->new Ref(u.id,u.nome)));
    }
    public Pagina<CadastroView> categories(Boolean ativo,Pageable page) {
        var u=current.get(); if (u.perfil!=Perfil.ADMIN && !Boolean.TRUE.equals(ativo)) throw ApiException.forbidden();
        return Pagina.of(categorias.findAll((r,q,b)->ativo==null ? b.conjunction() : b.equal(r.get("ativo"),ativo),page).map(c->new CadastroView(c.id,c.nome,c.descricao,c.ativo)));
    }
    public Pagina<CadastroView> sectors(Pageable page) { return Pagina.of(setores.findAll(page).map(s->new CadastroView(s.id,s.nome,null,s.ativo))); }
    @Transactional public CadastroView category(Long id,CadastroRequest req) {
        var c=id==null ? new Categoria() : categorias.findById(id).orElseThrow(ApiException::notFound);
        c.nome=req.nome().strip(); if (c.nome.length()<2) throw ApiException.invalid("Nome muito curto."); c.descricao=req.descricao(); c.ativo=req.ativo(); c.atualizadoEm=clock.instant(); categorias.saveAndFlush(c); return new CadastroView(c.id,c.nome,c.descricao,c.ativo);
    }
    @Transactional public CadastroView sector(Long id,CadastroRequest req) {
        var s=id==null ? new Setor() : setores.findById(id).orElseThrow(ApiException::notFound);
        s.nome=req.nome().strip(); if (s.nome.length()<2) throw ApiException.invalid("Nome muito curto."); s.ativo=req.ativo(); s.atualizadoEm=clock.instant(); setores.saveAndFlush(s); return new CadastroView(s.id,s.nome,null,s.ativo);
    }
    @Transactional public UsuarioView user(Long id,UsuarioRequest req) {
        var actor=current.get(); var u=id==null ? new Usuario() : users.findById(id).orElseThrow(ApiException::notFound);
        if (id!=null && actor.id.equals(id) && (!req.ativo() || req.perfil()!=Perfil.ADMIN)) throw ApiException.conflict("Você não pode desativar ou remover seu próprio perfil administrativo.");
        boolean removingTi=Workflow.ti(u.perfil==null ? Perfil.SOLICITANTE : u.perfil) && (!req.ativo() || !Workflow.ti(req.perfil()));
        if (id!=null && removingTi && chamados.existsByResponsavelIdAndStatusIn(id,List.of(Status.EM_ATENDIMENTO,Status.AGUARDANDO_USUARIO))) throw ApiException.conflict("Redistribua os chamados em atendimento antes desta alteração.");
        var setor=setores.findById(req.setorId()).orElseThrow(()->ApiException.invalid("Setor inválido."));
        if (!setor.ativo && (id==null || !u.setor.id.equals(setor.id))) throw ApiException.invalid("Selecione um setor ativo.");
        String email=req.email().strip().toLowerCase(Locale.ROOT);
        var duplicate=users.findByEmail(email); if (duplicate.isPresent() && !Objects.equals(duplicate.get().id,id)) throw ApiException.conflict("E-mail já cadastrado.");
        if (id==null) { PasswordPolicy.validate(req.senhaTemporaria()); u.senhaHash=encoder.encode(req.senhaTemporaria()); }
        else if (req.senhaTemporaria()!=null) throw ApiException.invalid("Use a operação de redefinição de senha.");
        if (id!=null && (u.perfil!=req.perfil() || u.ativo!=req.ativo() || !u.email.equals(email))) u.authVersion++;
        u.nome=req.nome().strip(); if (u.nome.length()<2) throw ApiException.invalid("Nome muito curto.");
        u.email=email; u.perfil=req.perfil(); u.setor=setor; u.ativo=req.ativo(); u.atualizadoEm=clock.instant(); users.saveAndFlush(u); return UsuarioView.of(u);
    }
    @Transactional public void reset(Long id,ResetSenhaRequest req) {
        if (current.get().id.equals(id)) throw ApiException.conflict("Para sua própria conta, use a troca de senha.");
        PasswordPolicy.validate(req.senhaTemporaria()); var u=users.findById(id).orElseThrow(ApiException::notFound);
        u.senhaHash=encoder.encode(req.senhaTemporaria()); u.trocaSenhaObrigatoria=true; u.authVersion++; u.atualizadoEm=clock.instant();
    }
}
