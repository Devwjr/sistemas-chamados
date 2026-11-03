package br.com.empresa.suporte.model;
import jakarta.persistence.*;
import java.time.Instant;
import br.com.empresa.suporte.model.Enums.Perfil;

@Entity @Table(name="usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false, length=120) public String nome;
    @Column(nullable=false, unique=true, length=200) public String email;
    @Column(name="senha_hash", nullable=false) public String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) public Perfil perfil;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="setor_id") public Setor setor;
    public boolean ativo = true;
    @Column(name="troca_senha_obrigatoria") public boolean trocaSenhaObrigatoria = true;
    @Column(name="auth_version") public long authVersion;
    @Column(name="criado_em", nullable=false) public Instant criadoEm = Instant.now();
    @Column(name="atualizado_em", nullable=false) public Instant atualizadoEm = Instant.now();
}
