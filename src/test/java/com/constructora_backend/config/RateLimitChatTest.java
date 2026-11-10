package com.constructora_backend.config;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El asistente de IA gasta cuota de pago por uso: tiene su propio límite, más estricto que el del resto de la API. */
class RateLimitChatTest {

    private final RateLimitConfig config = new RateLimitConfig();

    @Test
    void laRutaDelChatTieneSuPropiaCategoria() {
        assertEquals(RateLimitConfig.RateCategory.CHAT, config.categorize("/api/chat"));
        assertEquals(RateLimitConfig.RateCategory.GENERAL, config.categorize("/api/proyectos"));
    }

    @Test
    void soloDejaDiezPreguntasPorMinutoYPorIp() {
        Bucket bucket = config.resolveBucket("203.0.113.7", RateLimitConfig.RateCategory.CHAT);

        for (int i = 0; i < RateLimitConfig.CHAT_LIMIT; i++) {
            assertTrue(bucket.tryConsume(1), "la pregunta " + (i + 1) + " debe pasar");
        }
        assertFalse(bucket.tryConsume(1), "la pregunta 11 en el mismo minuto se rechaza");
    }

    @Test
    void cadaIpTieneSuPropioCupo() {
        Bucket a = config.resolveBucket("203.0.113.7", RateLimitConfig.RateCategory.CHAT);
        Bucket b = config.resolveBucket("203.0.113.8", RateLimitConfig.RateCategory.CHAT);
        for (int i = 0; i < RateLimitConfig.CHAT_LIMIT; i++) a.tryConsume(1);

        assertFalse(a.tryConsume(1));
        assertTrue(b.tryConsume(1));
    }
}
