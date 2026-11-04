package br.com.empresa.suporte.service;

import br.com.empresa.suporte.model.Enums.*;
import br.com.empresa.suporte.web.ApiException;

/** Regras puras: não depende de HTTP nem de persistência. */
public final class Workflow {
    private Workflow() {}

    public static boolean terminal(Status status) {
        return status == Status.FECHADO || status == Status.CANCELADO;
    }

    public static boolean ti(Perfil perfil) {
        return perfil == Perfil.TECNICO || perfil == Perfil.ADMIN;
    }

    public static void checkTransition(
            Status origem, Status destino, boolean admin, boolean owner, boolean assigned) {
        boolean allowed =
                switch (origem) {
                    case ABERTO -> destino == Status.CANCELADO && (admin || owner);
                    case EM_ATENDIMENTO ->
                            ((destino == Status.AGUARDANDO_USUARIO || destino == Status.RESOLVIDO)
                                            && (admin || assigned))
                                    || (destino == Status.CANCELADO && admin);
                    case AGUARDANDO_USUARIO ->
                            (destino == Status.EM_ATENDIMENTO && (admin || assigned))
                                    || (destino == Status.CANCELADO && admin);
                    case RESOLVIDO ->
                            (destino == Status.FECHADO || destino == Status.EM_ATENDIMENTO)
                                    && (admin || owner);
                    case FECHADO, CANCELADO -> false;
                };
        if (!allowed)
            throw ApiException.conflict("Transição não permitida para o estado e usuário atuais.");
    }

    public static String required(String value, int min, String field) {
        if (value == null || value.strip().length() < min || value.length() > 5000)
            throw ApiException.invalid(field + ": informe de " + min + " a 5.000 caracteres.");
        return value.strip();
    }
}
