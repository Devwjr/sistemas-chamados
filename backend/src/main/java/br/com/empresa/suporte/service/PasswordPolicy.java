package br.com.empresa.suporte.service;
import br.com.empresa.suporte.web.ApiException;
import java.nio.charset.StandardCharsets;
public final class PasswordPolicy {
    private PasswordPolicy() {}
    public static void validate(String password) {
        if (password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72 || password.isBlank())
            throw ApiException.invalid("A senha deve ter pelo menos 12 caracteres e no máximo 72 bytes UTF-8.");
    }
}
