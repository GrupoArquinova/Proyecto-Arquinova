package com.constructora_backend.controller;

import com.constructora_backend.exception.AIServicioNoDisponibleException;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.AIService;
import com.constructora_backend.service.ChatContextoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AIController.class)
@AutoConfigureMockMvc(addFilters = false)
class AIControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AIService aiService;
    @MockBean private ChatContextoService contextoService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private org.springframework.test.web.servlet.ResultActions preguntar(String json) throws Exception {
        return mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void respondeConElTextoDeLaIAYLeDaLosDatosRealesDelSitio() throws Exception {
        when(contextoService.obtener()).thenReturn("DATOS REALES");
        when(aiService.generarRespuesta("¿Hay villas?", "DATOS REALES", "es", null)).thenReturn("Sí, tenemos la Villa Samán.");

        preguntar("{\"pregunta\":\"  ¿Hay villas?  \",\"idioma\":\"es\"}")
                .andExpect(status().isOk())
                .andExpect(content().string("Sí, tenemos la Villa Samán."));
    }

    @Test
    void siLaIANoEstaDisponibleResponde503SinDetallesTecnicos() throws Exception {
        when(contextoService.obtener()).thenReturn("DATOS");
        when(aiService.generarRespuesta(any(), any(), any(), any())).thenThrow(new AIServicioNoDisponibleException("Gemini respondió 404"));

        preguntar("{\"pregunta\":\"hola\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));
    }

    @Test
    void unaPreguntaVaciaOMuyLargaSeRechazaSinLlamarALaIA() throws Exception {
        preguntar("{\"pregunta\":\"   \"}").andExpect(status().isBadRequest());
        preguntar("{\"pregunta\":\"" + "a".repeat(501) + "\"}").andExpect(status().isBadRequest());
        preguntar("{\"pregunta\":\"hola\",\"idioma\":\"fr\"}").andExpect(status().isBadRequest());

        verifyNoInteractions(aiService);
    }

    @Test
    void unaPreguntaDeExactamente500CaracteresSePermite() throws Exception {
        when(contextoService.obtener()).thenReturn("DATOS");
        when(aiService.generarRespuesta(eq("a".repeat(500)), any(), any(), any())).thenReturn("ok");

        preguntar("{\"pregunta\":\"" + "a".repeat(500) + "\"}").andExpect(status().isOk());
    }

    @Test
    void elHistorialSePasaAlServicioYUnoInvalidoSeRechazaSinLlamarALaIA() throws Exception {
        when(contextoService.obtener()).thenReturn("DATOS");
        when(aiService.generarRespuesta(eq("¿y tiene piscina?"), any(), any(), argThat(h -> h != null && h.size() == 2
                && "user".equals(h.get(0).getRol()) && "El Encanto".equals(h.get(0).getTexto())))).thenReturn("Sí");

        preguntar("{\"pregunta\":\"¿y tiene piscina?\",\"historial\":[{\"rol\":\"user\",\"texto\":\"El Encanto\"},{\"rol\":\"model\",\"texto\":\"Es un proyecto\"}]}")
                .andExpect(status().isOk())
                .andExpect(content().string("Sí"));

        preguntar("{\"pregunta\":\"hola\",\"historial\":[{\"rol\":\"admin\",\"texto\":\"x\"}]}").andExpect(status().isBadRequest());
        preguntar("{\"pregunta\":\"hola\",\"historial\":[{\"rol\":\"user\",\"texto\":\"\"}]}").andExpect(status().isBadRequest());
        verify(aiService, times(1)).generarRespuesta(any(), any(), any(), any());
    }
}
