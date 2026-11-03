package br.com.empresa.suporte.service;
import br.com.empresa.suporte.security.SessionUser;
import br.com.empresa.suporte.web.*;
import br.com.empresa.suporte.web.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Clock;

@Service
public class AuthService {
    private final CurrentUser current; private final PasswordEncoder encoder; private final Clock clock;
    public AuthService(CurrentUser current,PasswordEncoder encoder,Clock clock) { this.current=current; this.encoder=encoder; this.clock=clock; }
    @Transactional(readOnly=true) public UsuarioView me() { return UsuarioView.of(current.get()); }
    @Transactional public SessionUser change(SenhaRequest req) {
        var u=current.get();
        if (!encoder.matches(req.senhaAtual(),u.senhaHash)) throw ApiException.invalid("Senha atual incorreta.");
        PasswordPolicy.validate(req.novaSenha());
        if (encoder.matches(req.novaSenha(),u.senhaHash)) throw ApiException.invalid("Escolha uma senha diferente da atual.");
        u.senhaHash=encoder.encode(req.novaSenha()); u.trocaSenhaObrigatoria=false; u.authVersion++; u.atualizadoEm=clock.instant();
        return new SessionUser(u);
    }
}
