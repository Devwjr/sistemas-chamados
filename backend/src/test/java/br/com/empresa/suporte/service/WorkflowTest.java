package br.com.empresa.suporte.service;

import static org.junit.jupiter.api.Assertions.*;

import br.com.empresa.suporte.model.Enums.Perfil;
import br.com.empresa.suporte.model.Enums.Status;
import br.com.empresa.suporte.web.ApiException;

import org.junit.jupiter.api.Test;

class WorkflowTest {
    @Test
    void solicitantePodeCancelarSeuChamadoAberto() {
        assertDoesNotThrow(() -> Workflow.checkTransition(Status.ABERTO, Status.CANCELADO, false, true, false));
    }

    @Test
    void tecnicoNaoPodeCancelarChamadoEmAtendimento() {
        ApiException error = assertThrows(ApiException.class,
                () -> Workflow.checkTransition(Status.EM_ATENDIMENTO, Status.CANCELADO, false, false, true));
        assertEquals(409, error.status);
    }

    @Test
    void responsavelPodeResolverMasOutroTecnicoNao() {
        assertDoesNotThrow(() -> Workflow.checkTransition(Status.EM_ATENDIMENTO, Status.RESOLVIDO, false, false, true));
        assertThrows(ApiException.class,
                () -> Workflow.checkTransition(Status.EM_ATENDIMENTO, Status.RESOLVIDO, false, false, false));
    }

    @Test
    void solicitantePodeReabrirResolvidoMasEstadoTerminalNaoTemSaida() {
        assertDoesNotThrow(() -> Workflow.checkTransition(Status.RESOLVIDO, Status.EM_ATENDIMENTO, false, true, false));
        assertThrows(ApiException.class,
                () -> Workflow.checkTransition(Status.FECHADO, Status.EM_ATENDIMENTO, true, false, false));
        assertThrows(ApiException.class,
                () -> Workflow.checkTransition(Status.CANCELADO, Status.EM_ATENDIMENTO, true, false, false));
    }

    @Test
    void administracaoNaoConferePermissaoDeSolicitanteComoTi() {
        assertFalse(Workflow.ti(Perfil.SOLICITANTE));
        assertTrue(Workflow.ti(Perfil.TECNICO));
        assertTrue(Workflow.ti(Perfil.ADMIN));
    }

    @Test
    void camposObrigatoriosRespeitamComprimentoMinimoEMaximo() {
        assertEquals("texto válido", Workflow.required(" texto válido ", 5, "Campo"));
        assertThrows(ApiException.class, () -> Workflow.required("curto", 10, "Campo"));
        assertThrows(ApiException.class, () -> Workflow.required("x".repeat(5001), 1, "Campo"));
    }
}
