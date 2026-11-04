package br.com.empresa.suporte.config;

import br.com.empresa.suporte.repository.UsuarioRepository;
import br.com.empresa.suporte.security.*;
import br.com.empresa.suporte.web.ApiErrors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;

import java.time.Clock;
import java.util.Locale;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository users) {
        return email ->
                users.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                        .map(SessionUser::new)
                        .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, UsuarioRepository users, ObjectMapper mapper)
            throws Exception {
        http.authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/api/auth/csrf",
                                                "/api/auth/login",
                                                "/actuator/health")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .csrf(c -> c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
                .requestCache(c -> c.disable())
                .formLogin(
                        f ->
                                f.loginProcessingUrl("/api/auth/login")
                                        .successHandler(
                                                (req, res, auth) -> {
                                                    res.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    res.getWriter()
                                                            .write("{\"authenticated\":true}");
                                                })
                                        .failureHandler(
                                                (req, res, e) ->
                                                        ApiErrors.write(
                                                                mapper,
                                                                req,
                                                                res,
                                                                401,
                                                                "INVALID_CREDENTIALS",
                                                                "E-mail ou senha inválidos.")))
                .logout(
                        l ->
                                l.logoutUrl("/api/auth/logout")
                                        .deleteCookies("JSESSIONID")
                                        .logoutSuccessHandler(
                                                (req, res, auth) -> res.setStatus(204)))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (req, res, ex) ->
                                                        ApiErrors.write(
                                                                mapper,
                                                                req,
                                                                res,
                                                                401,
                                                                "UNAUTHENTICATED",
                                                                "Entre para continuar."))
                                        .accessDeniedHandler(
                                                (req, res, ex) ->
                                                        ApiErrors.write(
                                                                mapper,
                                                                req,
                                                                res,
                                                                403,
                                                                ex instanceof CsrfException
                                                                        ? "CSRF_INVALID"
                                                                        : "FORBIDDEN",
                                                                ex instanceof CsrfException
                                                                        ? "Sessão de formulário"
                                                                                + " expirada."
                                                                                + " Recarregue a"
                                                                                + " página."
                                                                        : "Acesso não permitido.")))
                .addFilterBefore(
                        new LoginThrottleFilter(mapper), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new AccountGuardFilter(users, mapper), AuthorizationFilter.class);
        return http.build();
    }
}
