package br.com.empresa.suporte.service;

import br.com.empresa.suporte.model.Usuario;
import br.com.empresa.suporte.repository.UsuarioRepository;
import br.com.empresa.suporte.security.SessionUser;
import br.com.empresa.suporte.web.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UsuarioRepository users;
    public CurrentUser(UsuarioRepository users) { this.users=users; }
    public Usuario get() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SessionUser p)) throw new ApiException(401,"UNAUTHENTICATED","Entre para continuar.");
        return users.findById(p.id()).filter(u->u.ativo).orElseThrow(()->new ApiException(401,"SESSION_EXPIRED","Entre novamente."));
    }
}
