package com.constructora_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Retención: analítica 24 meses, preguntas del chat 12 meses e historial de cambios 24 meses (cada plazo configurable; 0 = no borrar). */
class RetencionDatosAmpliadaTest {

    private final SolicitudContactoService solicitudes = mock(SolicitudContactoService.class);
    private final AnaliticaService analitica = mock(AnaliticaService.class);
    private final ChatPreguntaService chat = mock(ChatPreguntaService.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final RetencionDatosJob job = new RetencionDatosJob(solicitudes, analitica, chat, auditoria);
    private final LocalDateTime ahora = LocalDateTime.of(2026, 11, 3, 3, 0);

    @Test
    @DisplayName("Plazos por defecto: eventos de analítica 24 meses, preguntas del chat 12 meses, historial 24 meses")
    void plazosPorDefecto() {
        when(analitica.eliminarAnterioresA(LocalDateTime.of(2024, 11, 3, 3, 0))).thenReturn(10);
        when(chat.eliminarAnterioresA(LocalDateTime.of(2025, 11, 3, 3, 0))).thenReturn(4);
        when(auditoria.eliminarAnterioresA(LocalDateTime.of(2024, 11, 3, 3, 0))).thenReturn(2);

        assertEquals(10, job.depurarAnalitica(ahora));
        assertEquals(4, job.depurarPreguntasChat(ahora));
        assertEquals(2, job.depurarAuditoria(ahora));
    }

    @Test
    @DisplayName("La tarea diaria ejecuta las cuatro depuraciones")
    void laTareaDiariaLasEjecutaTodas() {
        job.depurarSolicitudes();

        verify(solicitudes).eliminarAnterioresA(any());
        verify(analitica).eliminarAnterioresA(any());
        verify(chat).eliminarAnterioresA(any());
        verify(auditoria).eliminarAnterioresA(any());
    }

    @Test
    @DisplayName("Con 0 meses ese dato no se borra nunca")
    void ceroDesactiva() {
        ReflectionTestUtils.setField(job, "mesesAnalitica", 0);
        ReflectionTestUtils.setField(job, "mesesChat", 0);
        ReflectionTestUtils.setField(job, "mesesAuditoria", 0);

        assertEquals(0, job.depurarAnalitica(ahora));
        assertEquals(0, job.depurarPreguntasChat(ahora));
        assertEquals(0, job.depurarAuditoria(ahora));
        verify(analitica, never()).eliminarAnterioresA(any());
        verify(chat, never()).eliminarAnterioresA(any());
        verify(auditoria, never()).eliminarAnterioresA(any());
    }

    @Test
    @DisplayName("Si una depuración falla, las demás se ejecutan igual y nada se propaga")
    void unaFallaNoDetieneLasDemas() {
        when(solicitudes.eliminarAnterioresA(any())).thenThrow(new RuntimeException("BD caída"));
        when(analitica.eliminarAnterioresA(any())).thenThrow(new RuntimeException("BD caída"));

        job.depurarSolicitudes();

        verify(chat).eliminarAnterioresA(any());
        verify(auditoria).eliminarAnterioresA(any());
    }

    @Test
    @DisplayName("Sin los servicios opcionales (construcción mínima) solo depura solicitudes")
    void construccionMinima() {
        RetencionDatosJob minimo = new RetencionDatosJob(solicitudes);

        assertEquals(0, minimo.depurarAnalitica(ahora));
        assertEquals(0, minimo.depurarPreguntasChat(ahora));
        assertEquals(0, minimo.depurarAuditoria(ahora));
    }
}
