package com.constructora_backend.config;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rate limiting: IP real (no el encabezado que mande el cliente), rutas de autenticación, tope de memoria y cupo global. */
class RateLimitSeguridadTest {

    private final RateLimitConfig config = new RateLimitConfig();

    private MockHttpServletResponse peticion(RateLimitingFilter filtro, String metodoUri, String ip, String xForwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", metodoUri);
        request.setRemoteAddr(ip);
        if (xForwardedFor != null) {
            request.addHeader("X-Forwarded-For", xForwardedFor);
            request.addHeader("X-Real-IP", xForwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    @DisplayName("Las rutas de login, recuperación, restablecimiento y renovación tienen límite de autenticación (10 por minuto)")
    void rutasDeAutenticacionTienenLimite() {
        assertEquals(RateLimitConfig.RateCategory.AUTH, config.categorize("/api/auth/login"));
        assertEquals(RateLimitConfig.RateCategory.AUTH, config.categorize("/api/auth/forgot-password"));
        assertEquals(RateLimitConfig.RateCategory.AUTH, config.categorize("/api/auth/reset-password"));
        assertEquals(RateLimitConfig.RateCategory.AUTH, config.categorize("/api/auth/renovar"));
        assertEquals(10, RateLimitConfig.AUTH_LIMIT);
    }

    @Test
    @DisplayName("Login: la 11.ª petición del minuto desde la misma IP recibe 429")
    void loginLimitadoPorIp() throws Exception {
        RateLimitingFilter filtro = new RateLimitingFilter(config);

        for (int i = 0; i < RateLimitConfig.AUTH_LIMIT; i++) {
            assertEquals(200, peticion(filtro, "/api/auth/login", "203.0.113.7", null).getStatus());
        }
        MockHttpServletResponse rechazada = peticion(filtro, "/api/auth/login", "203.0.113.7", null);

        assertEquals(429, rechazada.getStatus());
        assertNotNull(rechazada.getHeader("Retry-After"));
    }

    @Test
    @DisplayName("Cambiar el encabezado X-Forwarded-For en cada petición NO sirve para saltarse el límite")
    void xForwardedForFalsoNoEvadeElLimite() throws Exception {
        RateLimitingFilter filtro = new RateLimitingFilter(config);

        for (int i = 0; i < RateLimitConfig.AUTH_LIMIT; i++) {
            peticion(filtro, "/api/auth/login", "203.0.113.7", "198.51.100." + i);
        }

        assertEquals(429, peticion(filtro, "/api/auth/login", "203.0.113.7", "198.51.100.200").getStatus());
    }

    @Test
    @DisplayName("Otra IP real tiene su propio cupo")
    void cadaIpRealTieneSuCupo() throws Exception {
        RateLimitingFilter filtro = new RateLimitingFilter(config);
        for (int i = 0; i < RateLimitConfig.AUTH_LIMIT + 3; i++) {
            peticion(filtro, "/api/auth/forgot-password", "203.0.113.7", null);
        }

        assertEquals(200, peticion(filtro, "/api/auth/forgot-password", "203.0.113.8", null).getStatus());
    }

    @Test
    @DisplayName("El mapa de cupos tiene un tope de entradas: una avalancha de IP distintas no lo hace crecer sin fin")
    void elMapaDeCuposTieneTope() {
        RateLimitConfig pequena = new RateLimitConfig(100);

        for (int i = 0; i < 5000; i++) {
            pequena.resolveBucket("10.0." + (i / 250) + "." + (i % 250), RateLimitConfig.RateCategory.GENERAL);
        }

        assertTrue(pequena.tamanoBuckets() <= 100, "tamaño: " + pequena.tamanoBuckets());
    }

    @Test
    @DisplayName("Los cupos que no se han usado se purgan")
    void losCuposInactivosSePurgan() {
        config.resolveBucket("203.0.113.1", RateLimitConfig.RateCategory.CHAT); // nunca consumido: capacidad completa
        Bucket gastado = config.resolveBucket("203.0.113.2", RateLimitConfig.RateCategory.CHAT);
        gastado.tryConsume(5);
        assertEquals(2, config.tamanoBuckets());

        config.purgeInactiveBuckets();

        assertEquals(1, config.tamanoBuckets(), "queda el que tiene consumo reciente");
    }

    @Test
    @DisplayName("El chat y el formulario de contacto tienen además un cupo global (todas las IP juntas); el resto de rutas, no")
    void cupoGlobalSoloParaChatYContacto() {
        assertNotNull(config.resolveGlobalBucket(RateLimitConfig.RateCategory.CHAT));
        assertNotNull(config.resolveGlobalBucket(RateLimitConfig.RateCategory.PUBLIC));
        assertNull(config.resolveGlobalBucket(RateLimitConfig.RateCategory.GENERAL));
        assertNull(config.resolveGlobalBucket(RateLimitConfig.RateCategory.AUTH));
    }

    @Test
    @DisplayName("Chat: aunque cada IP respete su límite, el total del minuto tiene tope (respaldo global)")
    void chatTieneTopeGlobal() throws Exception {
        RateLimitingFilter filtro = new RateLimitingFilter(config);

        int aceptadas = 0;
        for (int i = 0; i < RateLimitConfig.CHAT_GLOBAL_LIMIT + 30; i++) {
            // Cada petición viene de una IP distinta: ninguna alcanza su límite individual
            MockHttpServletResponse r = peticion(filtro, "/api/chat", "198.51.100." + (i % 250), null);
            if (r.getStatus() == 200) aceptadas++;
        }

        assertEquals(RateLimitConfig.CHAT_GLOBAL_LIMIT, aceptadas);
    }

    @Test
    @DisplayName("Contacto público: tope global del minuto")
    void contactoTieneTopeGlobal() throws Exception {
        RateLimitingFilter filtro = new RateLimitingFilter(config);

        int aceptadas = 0;
        for (int i = 0; i < RateLimitConfig.PUBLIC_GLOBAL_LIMIT + 20; i++) {
            MockHttpServletResponse r = peticion(filtro, "/api/solicitudes-contacto/publico", "192.0.2." + (i % 250), null);
            if (r.getStatus() == 200) aceptadas++;
        }

        assertEquals(RateLimitConfig.PUBLIC_GLOBAL_LIMIT, aceptadas);
        assertFalse(aceptadas > RateLimitConfig.PUBLIC_GLOBAL_LIMIT);
    }
}
