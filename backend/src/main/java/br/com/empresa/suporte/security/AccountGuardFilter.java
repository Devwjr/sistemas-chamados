package br.com.empresa.suporte.security;

import br.com.empresa.suporte.repository.UsuarioRepository;
import br.com.empresa.suporte.web.ApiErrors;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

public class AccountGuardFilter extends OncePerRequestFilter {
    private final UsuarioRepository users;
    private final ObjectMapper mapper;
    private static final Set<String> ALLOWED=Set.of("/api/auth/me","/api/auth/csrf","/api/auth/senha","/api/auth/logout");
    public AccountGuardFilter(UsuarioRepository users,ObjectMapper mapper) { this.users=users; this.mapper=mapper; }
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SessionUser p) {
            var u=users.findById(p.id()).orElse(null);
            if (u == null || !u.ativo || u.authVersion != p.authVersion()) {
                var session=req.getSession(false); if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                ApiErrors.write(mapper,req,res,401,"SESSION_EXPIRED","Sua sessão expirou. Entre novamente."); return;
            }
            if (u.trocaSenhaObrigatoria && !ALLOWED.contains(req.getServletPath())) {
                ApiErrors.write(mapper,req,res,403,"PASSWORD_CHANGE_REQUIRED","Altere a senha temporária para continuar."); return;
            }
        }
        chain.doFilter(req,res);
    }
}
