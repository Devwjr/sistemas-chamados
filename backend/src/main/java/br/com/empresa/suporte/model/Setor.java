package br.com.empresa.suporte.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "setores")
public class Setor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String nome;

    public boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    public Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    public Instant atualizadoEm = Instant.now();
}
