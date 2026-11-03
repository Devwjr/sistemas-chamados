package br.com.empresa.suporte.security;

import br.com.empresa.suporte.web.ApiErrors;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.*;
import java.util.concurrent.*;

// Limite por IP na instância. Em produção com proxy, aplicar limite também no proxy.
public class LoginThrottleFilter extends OncePerRequestFilter {
    private record Window(long start, java.util.concurrent.atomic.AtomicInteger count) {}
    private final ConcurrentHashMap<String,Window> attempts=new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    public LoginThrottleFilter(ObjectMapper mapper) { this.mapper=mapper; }
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if ("POST".equals(req.getMethod()) && "/api/auth/login".equals(req.getServletPath())) {
            long now=Instant.now().getEpochSecond();
            attempts.entrySet().removeIf(e->now-e.getValue().start() >= 60);
            if (attempts.size() >= 10000) { ApiErrors.write(mapper,req,res,429,"LOGIN_RATE_LIMIT","Aguarde um minuto antes de tentar novamente."); return; }
            var w=attempts.compute(req.getRemoteAddr(),(k,v)->v==null || now-v.start()>=60 ? new Window(now,new java.util.concurrent.atomic.AtomicInteger()) : v);
            if (w.count().incrementAndGet()>10) { res.setHeader("Retry-After","60"); ApiErrors.write(mapper,req,res,429,"LOGIN_RATE_LIMIT","Aguarde um minuto antes de tentar novamente."); return; }
        }
        chain.doFilter(req,res);
    }
}
