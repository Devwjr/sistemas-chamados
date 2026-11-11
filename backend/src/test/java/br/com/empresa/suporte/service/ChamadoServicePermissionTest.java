package br.com.empresa.suporte.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.empresa.suporte.model.Chamado;
import br.com.empresa.suporte.model.Categoria;
import br.com.empresa.suporte.model.Setor;
import br.com.empresa.suporte.model.Usuario;
import br.com.empresa.suporte.model.Enums.Perfil;
import br.com.empresa.suporte.model.Enums.Status;
import br.com.empresa.suporte.model.Enums.Visibilidade;
import br.com.empresa.suporte.repository.*;
import br.com.empresa.suporte.web.ApiException;
import br.com.empresa.suporte.web.Dtos.ComentarioRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ChamadoServicePermissionTest {
    @Mock ChamadoRepository chamados;
    @Mock CategoriaRepository categorias;
    @Mock UsuarioRepository usuarios;
    @Mock ComentarioRepository comentarios;
    @Mock HistoricoRepository historicos;
    @Mock CurrentUser current;

    ChamadoService service;

    @BeforeEach
    void setUp() {
        service = new ChamadoService(
                chamados,
                categorias,
                usuarios,
                comentarios,
                historicos,
                current,
                Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void solicitanteNaoConsultaChamadoDeOutraPessoa() {
        Usuario caller = user(7L, Perfil.SOLICITANTE);
        Chamado ticket = ticket(41L, user(8L, Perfil.SOLICITANTE));
        when(current.get()).thenReturn(caller);
        when(chamados.findDetalheById(41L)).thenReturn(Optional.of(ticket));

        ApiException error = assertThrows(ApiException.class, () -> service.detail(41L));

        assertEquals(404, error.status);
    }

    @Test
    void solicitanteRecebeSomenteComentariosPublicos() {
        Usuario caller = user(7L, Perfil.SOLICITANTE);
        when(current.get()).thenReturn(caller);
        when(chamados.findDetalheById(41L)).thenReturn(Optional.of(ticket(41L, caller)));
        when(comentarios.findByChamadoIdAndVisibilidade(41L, Visibilidade.PUBLICO, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        service.comments(41L, PageRequest.of(0, 20));

        verify(comentarios).findByChamadoIdAndVisibilidade(41L, Visibilidade.PUBLICO, PageRequest.of(0, 20));
        verify(comentarios, never()).findByChamadoId(eq(41L), any());
    }

    @Test
    void solicitanteNaoPodeCriarNotaInterna() {
        Usuario caller = user(7L, Perfil.SOLICITANTE);
        when(current.get()).thenReturn(caller);
        when(chamados.findDetalheById(41L)).thenReturn(Optional.of(ticket(41L, caller)));
        ComentarioRequest request = new ComentarioRequest(0L, "nota que deve ficar interna", Visibilidade.INTERNO);

        ApiException error = assertThrows(ApiException.class, () -> service.addComment(41L, request));

        assertEquals(403, error.status);
        verify(comentarios, never()).save(any());
    }

    private Usuario user(Long id, Perfil perfil) {
        Usuario user = new Usuario();
        user.id = id;
        user.nome = "Usuário " + id;
        user.perfil = perfil;
        user.ativo = true;
        return user;
    }

    private Chamado ticket(Long id, Usuario requester) {
        Setor sector = new Setor();
        sector.id = 2L;
        sector.nome = "TI";
        Categoria category = new Categoria();
        category.id = 3L;
        category.nome = "Acesso";
        Chamado ticket = new Chamado();
        ticket.id = id;
        ticket.titulo = "Problema de acesso";
        ticket.descricao = "Não consigo acessar o sistema interno.";
        ticket.status = Status.ABERTO;
        ticket.solicitante = requester;
        ticket.setorOrigem = sector;
        ticket.categoria = category;
        ticket.criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        ticket.atualizadoEm = ticket.criadoEm;
        return ticket;
    }
}
