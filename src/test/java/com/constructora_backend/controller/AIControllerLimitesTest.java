package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ChatRequest;
import com.constructora_backend.exception.AIServicioNoDisponibleException;
import com.constructora_backend.service.AIService;
import com.constructora_backend.service.ChatContextoService;
import com.constructora_backend.service.LimitadorVentana;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El chat gasta cuota de pago: tope de consultas simultáneas (sin hacer esperar) y cuota diaria global. */
class AIControllerLimitesTest {

    private AIService aiService;
    private ChatContextoService contexto;

    @BeforeEach
    void preparar() {
        aiService = mock(AIService.class);
        contexto = mock(ChatContextoService.class);
        when(contexto.obtener()).thenReturn("DATOS");
    }

    private ChatRequest pregunta(String texto) {
        ChatRequest r = new ChatRequest();
        r.setPregunta(texto);
        return r;
    }

    @Test
    @DisplayName("Con todos los cupos de concurrencia ocupados, la siguiente consulta recibe 429 de inmediato, sin esperar")
    void sinCupoDeConcurrenciaResponde429Rapido() throws Exception {
        AIController controlador = new AIController(aiService, contexto, new LimitadorVentana(), 1, 0);
        CountDownLatch dentro = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        when(aiService.generarRespuesta(any(), any(), any())).thenAnswer(i -> {
            dentro.countDown();
            liberar.await(10, TimeUnit.SECONDS);
            return "respuesta";
        });

        CompletableFuture<ResponseEntity<String>> primera = CompletableFuture.supplyAsync(() -> controlador.chatearConIA(pregunta("uno")));
        assertTrue(dentro.await(5, TimeUnit.SECONDS), "la primera consulta ocupa el único cupo");

        long inicio = System.nanoTime();
        ResponseEntity<String> segunda = controlador.chatearConIA(pregunta("dos"));
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        assertEquals(429, segunda.getStatusCode().value());
        assertEquals("10", segunda.getHeaders().getFirst("Retry-After"));
        assertTrue(milisegundos < 1000, "no hace esperar: respondió en " + milisegundos + " ms");

        liberar.countDown();
        assertEquals(200, primera.get(5, TimeUnit.SECONDS).getStatusCode().value());

        // Con el cupo liberado, vuelve a atender
        assertEquals(200, controlador.chatearConIA(pregunta("tres")).getStatusCode().value());
    }

    @Test
    @DisplayName("El cupo de concurrencia se libera aunque la IA falle")
    void elCupoSeLiberaSiLaIAFalla() {
        AIController controlador = new AIController(aiService, contexto, new LimitadorVentana(), 1, 0);
        when(aiService.generarRespuesta(any(), any(), any())).thenThrow(new AIServicioNoDisponibleException("falla"));

        for (int i = 0; i < 3; i++) {
            assertEquals(503, controlador.chatearConIA(pregunta("hola")).getStatusCode().value(),
                    "cada intento llega a la IA (503), ninguno queda bloqueado por un cupo perdido");
        }
    }

    @Test
    @DisplayName("Cuota diaria global: al agotarse responde 503 sin llamar a la IA")
    void cuotaDiariaAgotada() {
        AIController controlador = new AIController(aiService, contexto, new LimitadorVentana(), 5, 2);
        when(aiService.generarRespuesta(any(), any(), any())).thenReturn("ok");

        assertEquals(200, controlador.chatearConIA(pregunta("1")).getStatusCode().value());
        assertEquals(200, controlador.chatearConIA(pregunta("2")).getStatusCode().value());
        assertEquals(503, controlador.chatearConIA(pregunta("3")).getStatusCode().value());

        verify(aiService, times(2)).generarRespuesta(any(), any(), any());
    }

    @Test
    @DisplayName("Una consulta que falló sin dar respuesta no gasta cuota del día")
    void laConsultaFallidaDevuelveLaCuota() {
        AIController controlador = new AIController(aiService, contexto, new LimitadorVentana(), 5, 1);
        when(aiService.generarRespuesta(any(), any(), any()))
                .thenThrow(new AIServicioNoDisponibleException("falla"))
                .thenReturn("ok");

        assertEquals(503, controlador.chatearConIA(pregunta("1")).getStatusCode().value());
        assertEquals(200, controlador.chatearConIA(pregunta("2")).getStatusCode().value(), "la cuota de 1 seguía disponible");
    }

    @Test
    @DisplayName("Cuota 0 = sin tope diario")
    void cuotaCeroSinTope() {
        AIController controlador = new AIController(aiService, contexto, new LimitadorVentana(), 5, 0);
        when(aiService.generarRespuesta(any(), any(), any())).thenReturn("ok");

        for (int i = 0; i < 50; i++) {
            assertEquals(200, controlador.chatearConIA(pregunta("x")).getStatusCode().value());
        }
    }
}
