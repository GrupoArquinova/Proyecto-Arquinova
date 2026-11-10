package com.constructora_backend.controller;

import com.constructora_backend.config.RateLimitConfig;
import com.constructora_backend.config.SecurityConfig;
import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.security.JwtFilter;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.AnaliticaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/analitica/evento: público, sin captcha, 204, validación del tipo, límite de tamaño y cupo propio. Con la seguridad real. */
@WebMvcTest(AnaliticaController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class AnaliticaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AnaliticaService analiticaService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private org.springframework.test.web.servlet.ResultActions enviar(String json) throws Exception {
        return mockMvc.perform(post("/api/analitica/evento").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("Es público: sin sesión ni captcha responde 204 y entrega el evento al servicio")
    void publicoResponde204() throws Exception {
        enviar("{\"tipo\":\"VISTA_PROYECTO\",\"proyectoId\":6,\"ruta\":\"/proyectos/6\"}").andExpect(status().isNoContent());

        verify(analiticaService).registrarDelNavegador(TipoEventoAnalitica.VISTA_PROYECTO, 6L, "/proyectos/6");
    }

    @Test
    @DisplayName("Responde 204 aunque el servicio ignore el evento (proyecto inexistente o tipo del servidor): no revela nada")
    void siempre204() throws Exception {
        when(analiticaService.registrarDelNavegador(any(), any(), any())).thenReturn(false);
        enviar("{\"tipo\":\"FORMULARIO_ENVIADO\"}").andExpect(status().isNoContent());
        enviar("{\"tipo\":\"VISTA_PROYECTO\",\"proyectoId\":99999}").andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Un fallo interno del servicio no se ve en el sitio: sigue siendo 204")
    void fallosInternosNoSeVen() throws Exception {
        when(analiticaService.registrarDelNavegador(any(), any(), any())).thenThrow(new RuntimeException("BD caída"));
        enviar("{\"tipo\":\"VISTA_PAGINA\",\"ruta\":\"/\"}").andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Tipo desconocido, ausente o cuerpo ilegible: 400 y no se llama al servicio")
    void validaElTipo() throws Exception {
        enviar("{\"tipo\":\"INVENTADO\"}").andExpect(status().isBadRequest());
        enviar("{\"proyectoId\":1}").andExpect(status().isBadRequest());
        enviar("no es json").andExpect(status().isBadRequest());
        verify(analiticaService, never()).registrarDelNavegador(any(), any(), any());
    }

    @Test
    @DisplayName("Respeta el límite de tamaño del cuerpo: más de 64 KB responde 413")
    void respetaElLimiteDeTamano() throws Exception {
        String enorme = "{\"tipo\":\"VISTA_PAGINA\",\"ruta\":\"/" + "a".repeat(70_000) + "\"}";
        enviar(enorme).andExpect(status().isPayloadTooLarge());
        verify(analiticaService, never()).registrarDelNavegador(any(), any(), any());
    }

    @Test
    @DisplayName("Tiene su propio cupo por IP (categoría ANALITICA), más generoso que el de contacto y acotado")
    void cupoPropio() {
        RateLimitConfig config = new RateLimitConfig();
        assertEquals(RateLimitConfig.RateCategory.ANALITICA, config.categorize("/api/analitica/evento"));
        assertEquals(RateLimitConfig.ANALITICA_LIMIT, config.resolveBucket("1.2.3.4", RateLimitConfig.RateCategory.ANALITICA).getAvailableTokens());
        org.junit.jupiter.api.Assertions.assertTrue(RateLimitConfig.ANALITICA_LIMIT > RateLimitConfig.PUBLIC_LIMIT);
        org.junit.jupiter.api.Assertions.assertTrue(RateLimitConfig.ANALITICA_LIMIT < RateLimitConfig.GENERAL_LIMIT);

        // Pasado el cupo, el bucket rechaza
        var bucket = config.resolveBucket("5.6.7.8", RateLimitConfig.RateCategory.ANALITICA);
        for (int i = 0; i < RateLimitConfig.ANALITICA_LIMIT; i++) {
            org.junit.jupiter.api.Assertions.assertTrue(bucket.tryConsume(1));
        }
        org.junit.jupiter.api.Assertions.assertFalse(bucket.tryConsume(1));
    }

    @Test
    @DisplayName("El brochure también tiene su cupo propio (la generación del PDF es costosa)")
    void cupoDelBrochure() {
        RateLimitConfig config = new RateLimitConfig();
        assertEquals(RateLimitConfig.RateCategory.BROCHURE, config.categorize("/api/proyectos/6/brochure.pdf"));
        assertEquals(RateLimitConfig.RateCategory.GENERAL, config.categorize("/api/proyectos/6"));
        org.junit.jupiter.api.Assertions.assertTrue(RateLimitConfig.BROCHURE_LIMIT <= 20);
    }
}
