package br.com.empresa.suporte.model;

import br.com.empresa.suporte.model.Enums.Evento;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "historico")
public class Historico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id")
    public Chamado chamado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    public Usuario autor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 20)
    public Evento tipoEvento;

    @Column(name = "valor_anterior", length = 200)
    public String valorAnterior;

    @Column(name = "valor_novo", length = 200)
    public String valorNovo;

    @Column(length = 5000)
    public String justificativa;

    @Column(name = "criado_em", nullable = false)
    public Instant criadoEm = Instant.now();
}
