package br.com.empresa.suporte.service;

import static org.junit.jupiter.api.Assertions.*;

import br.com.empresa.suporte.web.ApiException;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
    @Test
    void aceitaSenhaComDozeCaracteres() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("senha-segura-123"));
    }

    @Test
    void rejeitaSenhaCurtaVaziaOuAcimaDe72BytesUtf8() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("curta"));
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("            "));
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("á".repeat(37)));
        assertThrows(ApiException.class, () -> PasswordPolicy.validate(null));
    }
}
