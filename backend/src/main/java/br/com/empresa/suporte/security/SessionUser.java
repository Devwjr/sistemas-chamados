package br.com.empresa.suporte.security;

import br.com.empresa.suporte.model.Usuario;
import br.com.empresa.suporte.model.Enums.Perfil;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;

public final class SessionUser implements UserDetails, org.springframework.security.core.CredentialsContainer {
    private final Long id;
    private final String email;
    private String password;
    private final Perfil perfil;
    private final long authVersion;
    private final boolean enabled;
    public SessionUser(Usuario u) { id=u.id; email=u.email; password=u.senhaHash; perfil=u.perfil; authVersion=u.authVersion; enabled=u.ativo; }
    public Long id() { return id; }
    public long authVersion() { return authVersion; }
    public Perfil perfil() { return perfil; }
    public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_"+perfil.name())); }
    public void eraseCredentials() { password=null; }
    public String getPassword() { return password; }
    public String getUsername() { return email; }
    public boolean isEnabled() { return enabled; }
}
