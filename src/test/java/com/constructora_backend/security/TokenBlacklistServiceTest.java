package com.constructora_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    @DisplayName("Con Redis guarda la huella SHA-256 del token, nunca el token completo")
    @SuppressWarnings("unchecked")
    void redisGuardaSoloLaHuella() {
        org.springframework.data.redis.core.StringRedisTemplate redis = org.mockito.Mockito.mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, String> ops = org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        org.mockito.Mockito.when(redis.opsForValue()).thenReturn(ops);
        TokenBlacklistService servicio = new TokenBlacklistService(redis);
        String token = "eyJhbGciOiJIUzI1NiJ9.contenido-del-token.firma-del-token";

        servicio.revocarToken(token, System.currentTimeMillis() + 60_000);

        org.mockito.ArgumentCaptor<String> clave = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(ops).set(clave.capture(), org.mockito.ArgumentMatchers.eq("revoked"),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq(java.util.concurrent.TimeUnit.MILLISECONDS));
        assertEquals("jwt:blacklist:" + TokenBlacklistService.huella(token), clave.getValue());
        assertFalse(clave.getValue().contains(token), "el token completo no se guarda");
        assertFalse(clave.getValue().contains("contenido-del-token"));
        assertEquals(64, TokenBlacklistService.huella(token).length());

        org.mockito.Mockito.when(redis.hasKey("jwt:blacklist:" + TokenBlacklistService.huella(token))).thenReturn(true);
        assertTrue(servicio.estaRevocado(token));
    }

    @Test
    @DisplayName("La huella es determinista y distinta para tokens distintos")
    void huellaDeterminista() {
        assertEquals(TokenBlacklistService.huella("a"), TokenBlacklistService.huella("a"));
        assertNotEquals(TokenBlacklistService.huella("a"), TokenBlacklistService.huella("b"));
        assertNotEquals("a", TokenBlacklistService.huella("a"));
    }

    @Test
    @DisplayName("La purga periódica quita de memoria las revocaciones que ya vencieron")
    void purgaPeriodica() throws Exception {
        blacklistService.revocarToken("vence-pronto", System.currentTimeMillis() + 50);
        blacklistService.revocarToken("vence-tarde", System.currentTimeMillis() + 3_600_000);
        assertEquals(2, blacklistService.tamanoLocal());

        Thread.sleep(120);
        blacklistService.purgarVencidos();

        assertEquals(1, blacklistService.tamanoLocal());
        assertTrue(blacklistService.estaRevocado("vence-tarde"));
    }
}
