package com.constructora_backend.security;

import com.constructora_backend.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/** Con APP_DOCS_PUBLICAS=true (desarrollo) Swagger se abre y su CSP deja cargar la interfaz. */
@WebMvcTest(controllers = SwaggerAbiertoEnDesarrolloTest.SinRutas.class)
@Import({SecurityConfig.class, JwtFilter.class, MetricasTokenFilter.class, SwaggerAbiertoEnDesarrolloTest.SinRutas.class})
@TestPropertySource(properties = "app.seguridad.docs-publicas=true")
class SwaggerAbiertoEnDesarrolloTest {

    /** Controlador vacío: evita que la prueba cargue todos los controladores de la aplicación. */
    @org.springframework.web.bind.annotation.RestController
    static class SinRutas {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Swagger abierto: no pide sesión y la CSP permite los scripts de su interfaz")
    void swaggerAbierto() throws Exception {
        int estado = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus();
        // Sin el controlador de springdoc en esta prueba da 404, pero nunca 401/403: la seguridad lo deja pasar
        assertNotEquals(401, estado);
        assertNotEquals(403, estado);
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'"));
    }
}
