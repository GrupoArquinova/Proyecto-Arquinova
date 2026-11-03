package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ChatRequest;
import com.constructora_backend.dto.request.MensajeChat;
import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.service.AIService;
import com.constructora_backend.service.AnaliticaService;
import com.constructora_backend.service.ChatContextoService;
import com.constructora_backend.service.ChatPreguntaService;
import com.constructora_backend.service.LimitadorVentana;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El chat guarda SOLO la pregunta y cuenta INICIO_CHAT, sin que un fallo de eso afecte la respuesta. */
class AIControllerPreguntasTest {

    private AIService ia;
    private ChatContextoService contexto;
    private ChatPreguntaService preguntas;
    private AnaliticaService analitica;
    private AIController controlador;

    @BeforeEach
    void preparar() {
        ia = mock(AIService.class);
        contexto = mock(ChatContextoService.class);
        preguntas = mock(ChatPreguntaService.class);
        analitica = mock(AnaliticaService.class);
        when(contexto.obtener()).thenReturn("DATOS");
        when(ia.generarRespuesta(any(), any(), any(), any())).thenReturn("respuesta");
        controlador = new AIController(ia, contexto, new LimitadorVentana(), 5, 0, preguntas, analitica);
    }

    private static ChatRequest pregunta(String texto, List<MensajeChat> historial) {
        ChatRequest r = new ChatRequest();
        r.setPregunta(texto);
        r.setIdioma("es");
        r.setHistorial(historial);
        return r;
    }

    @Test
    @DisplayName("Guarda la pregunta con su idioma (el servicio la depura); nunca la respuesta ni el historial")
    void guardaSoloLaPregunta() {
        var respuesta = controlador.chatearConIA(pregunta("  ¿Dónde queda?  ", null));

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals("respuesta", respuesta.getBody());
        verify(preguntas).guardar("  ¿Dónde queda?  ", "es");
        // El servicio recibe solo (pregunta, idioma): ni la respuesta de la IA ni el historial pasan por ahí
        verify(preguntas, org.mockito.Mockito.times(1)).guardar(any(), any());
    }

    @Test
    @DisplayName("INICIO_CHAT solo con la primera pregunta de la conversación (sin historial)")
    void inicioDeChatSoloSinHistorial() {
        controlador.chatearConIA(pregunta("hola", null));
        controlador.chatearConIA(pregunta("hola", List.of()));
        verify(analitica, org.mockito.Mockito.times(2)).registrarDelServidor(TipoEventoAnalitica.INICIO_CHAT, null);

        controlador.chatearConIA(pregunta("y la piscina?", List.of(new MensajeChat("user", "hola"), new MensajeChat("model", "hola"))));
        verify(analitica, org.mockito.Mockito.times(2)).registrarDelServidor(any(), any());
    }

    @Test
    @DisplayName("Si guardar o la analítica fallan, el chat responde igual")
    void unFalloNoRompeElChat() {
        org.mockito.Mockito.doThrow(new RuntimeException("BD caída")).when(preguntas).guardar(any(), any());
        org.mockito.Mockito.doThrow(new RuntimeException("BD caída")).when(analitica).registrarDelServidor(any(), any());

        var respuesta = controlador.chatearConIA(pregunta("hola", null));

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals("respuesta", respuesta.getBody());
    }

    @Test
    @DisplayName("El controlador sin servicios opcionales (guardado y analítica) también funciona y no guarda nada")
    void sinServiciosOpcionales() {
        AIController minimo = new AIController(ia, contexto, new LimitadorVentana(), 5, 0);

        assertEquals(200, minimo.chatearConIA(pregunta("hola", null)).getStatusCode().value());
        verify(preguntas, never()).guardar(any(), any());
    }
}
