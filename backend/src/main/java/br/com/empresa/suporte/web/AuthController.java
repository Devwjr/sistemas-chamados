package br.com.empresa.suporte.web;

import br.com.empresa.suporte.service.AuthService;
import br.com.empresa.suporte.web.Dtos.*;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/me")
    public UsuarioView me() {
        return service.me();
    }

    @PatchMapping("/senha")
    public ResponseEntity<Void> password(
            @Valid @RequestBody SenhaRequest body,
            HttpServletRequest req,
            HttpServletResponse res) {
        var p = service.change(body);
        p.eraseCredentials();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, req, res);
        return ResponseEntity.noContent().build();
    }
}
