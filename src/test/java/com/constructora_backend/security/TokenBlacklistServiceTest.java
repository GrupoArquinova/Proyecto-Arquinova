package com.constructora_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBlacklistServiceTest {

    private TokenBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new TokenBlacklistService();
    }

    @Test
    @DisplayName("Debe reportar falso para un token que nunca fue revocado")
    void tokenNoRevocadoRetornaFalso() {
        assertFalse(blacklistService.estaRevocado("token_valido_xyz"));
    }

    @Test
    @DisplayName("Debe reportar verdadero para un token revocado dentro de su ventana de validez")
    void tokenRevocadoRetornaVerdadero() {
        String token = "jwt_token_logout_123";
        long expiraEnUnaHora = System.currentTimeMillis() + 3600000L;

        blacklistService.revocarToken(token, expiraEnUnaHora);

        assertTrue(blacklistService.estaRevocado(token));
    }

    @Test
    @DisplayName("Debe reportar falso si el token revocado ya expiró naturalmente")
    void tokenRevocadoYaExpiradoRetornaFalso() {
        String token = "jwt_token_antiguo";
        long expiroHaceDiezSegundos = System.currentTimeMillis() - 10000L;

        blacklistService.revocarToken(token, expiroHaceDiezSegundos);

        assertFalse(blacklistService.estaRevocado(token));
    }
}
