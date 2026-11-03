package br.com.empresa.suporte.model;
import jakarta.persistence.*;
import java.time.Instant;
import br.com.empresa.suporte.model.Enums.Visibilidade;

@Entity @Table(name="comentarios")
public class Comentario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="chamado_id") public Chamado chamado;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="autor_id") public Usuario autor;
    @Column(nullable=false, length=5000) public String conteudo;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=10) public Visibilidade visibilidade;
    @Column(name="criado_em", nullable=false) public Instant criadoEm = Instant.now();
}
