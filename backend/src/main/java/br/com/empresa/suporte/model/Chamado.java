package br.com.empresa.suporte.model;

import br.com.empresa.suporte.model.Enums.*;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chamados")
public class Chamado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 150)
    public String titulo;

    @Column(nullable = false, length = 5000)
    public String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public Impacto impacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public Status status = Status.ABERTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Prioridade prioridade = Prioridade.MEDIA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id")
    public Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setor_origem_id")
    public Setor setorOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    public Usuario responsavel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    public Categoria categoria;

    @Column(length = 5000)
    public String solucao;

    @Column(name = "criado_em", nullable = false)
    public Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    public Instant atualizadoEm = Instant.now();

    @Column(name = "resolvido_em")
    public Instant resolvidoEm;

    @Column(name = "fechado_em")
    public Instant fechadoEm;

    @Column(name = "cancelado_em")
    public Instant canceladoEm;

    @Version public long versao;

    public String protocolo() {
        return "CH-%06d".formatted(id);
    }
}
