package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Métricas del dashboard del panel (GET /api/admin/metricas). Todas las listas vienen siempre (vacías si no hay datos) y las
 * series de tiempo (semanas y días) vienen sin huecos: los periodos sin datos aparecen con total 0.
 */
@Schema(description = "Métricas del dashboard: solicitudes, lotes, analítica sin cookies y preguntas del asistente")
public record MetricasDTO(Periodo periodo, Solicitudes solicitudes, Lotes lotes, Analitica analitica, Chat chat) {

    /** Rango consultado; ambos días incluidos. */
    public record Periodo(LocalDate desde, LocalDate hasta) {
    }

    // ---- solicitudes de contacto (dentro del periodo)

    public record Solicitudes(long total, List<PorSemana> porSemana, List<PorProyecto> porProyecto, List<PorEstado> porEstado) {
    }

    /** Semana ISO (lunes a domingo): "2026-W44" con el lunes en que empieza. */
    public record PorSemana(String semana, LocalDate inicio, long total) {
    }

    /** proyectoId es null (y proyecto "Sin proyecto") para las solicitudes que no apuntan a ninguno. */
    public record PorProyecto(Long proyectoId, String proyecto, long total) {
    }

    public record PorEstado(String estado, long total) {
    }

    // ---- lotes (estado actual, no depende del periodo)

    public record Lotes(List<PorEstado> porEstado, List<LotesDeProyecto> porProyecto) {
    }

    public record LotesDeProyecto(Long proyectoId, String proyecto, long disponibles, long reservados, long vendidos, long total) {
    }

    // ---- analítica propia sin cookies (dentro del periodo)

    public record Analitica(List<VistasDeProyecto> visitasPorProyecto, List<PorTipo> eventosPorTipo,
                            List<PorDia> visitasPorDia, Embudo embudo) {
    }

    public record VistasDeProyecto(Long proyectoId, String proyecto, long vistas) {
    }

    public record PorTipo(String tipo, long total) {
    }

    /** Vistas de página de ese día (VISTA_PAGINA). */
    public record PorDia(LocalDate fecha, long total) {
    }

    public record Embudo(long vistasProyecto, long clicksWhatsapp, long formulariosEnviados, long descargasBrochure) {
    }

    // ---- preguntas del asistente (dentro del periodo)

    /** recientes (las últimas 30) solo la ve un ADMINISTRADOR; para el COMERCIAL viene vacía. */
    public record Chat(long totalPreguntas, List<PorTema> temas, List<PreguntaReciente> recientes) {
    }

    public record PorTema(String tema, long total) {
    }

    public record PreguntaReciente(LocalDateTime fecha, String pregunta, String tema) {
    }
}
