package br.com.empresa.suporte.model;

public final class Enums {
    private Enums() {}

    public enum Perfil {
        SOLICITANTE,
        TECNICO,
        ADMIN
    }

    public enum Status {
        ABERTO,
        EM_ATENDIMENTO,
        AGUARDANDO_USUARIO,
        RESOLVIDO,
        FECHADO,
        CANCELADO
    }

    public enum Prioridade {
        BAIXA,
        MEDIA,
        ALTA,
        CRITICA
    }

    public enum Impacto {
        INDIVIDUAL,
        SETOR,
        ORGANIZACAO
    }

    public enum Visibilidade {
        PUBLICO,
        INTERNO
    }

    public enum Evento {
        ABERTURA,
        STATUS,
        RESPONSAVEL,
        PRIORIDADE,
        CATEGORIA
    }
}
