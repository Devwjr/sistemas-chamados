package br.com.empresa.suporte.config;

import br.com.empresa.suporte.model.*;
import br.com.empresa.suporte.model.Enums.Perfil;
import br.com.empresa.suporte.repository.*;
import br.com.empresa.suporte.service.PasswordPolicy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final UsuarioRepository users;
    private final SetorRepository setores;
    private final CategoriaRepository categorias;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public BootstrapAdmin(
            UsuarioRepository users,
            SetorRepository setores,
            CategoriaRepository categorias,
            PasswordEncoder encoder,
            @Value("${app.bootstrap.email}") String email,
            @Value("${app.bootstrap.password}") String password) {
        this.users = users;
        this.setores = setores;
        this.categorias = categorias;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0 || email.isBlank()) return;
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 200)
            throw new IllegalArgumentException("E-mail de bootstrap inválido.");
        PasswordPolicy.validate(password);
        var setor = new Setor();
        setor.nome = "Tecnologia da Informação";
        setores.save(setor);
        var u = new Usuario();
        u.nome = "Administrador";
        u.email = email.strip().toLowerCase(Locale.ROOT);
        u.senhaHash = encoder.encode(password);
        u.perfil = Perfil.ADMIN;
        u.setor = setor;
        users.save(u);
        for (String nome :
                new String[] {
                    "Rede e conectividade",
                    "Acessos e senhas",
                    "Equipamentos",
                    "Impressão",
                    "Sistemas"
                }) {
            var c = new Categoria();
            c.nome = nome;
            categorias.save(c);
        }
    }
}
