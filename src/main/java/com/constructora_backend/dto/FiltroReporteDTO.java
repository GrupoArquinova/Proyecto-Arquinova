package com.constructora_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "Criterios de filtrado para consultas y exportaciones de reportes")
public class FiltroReporteDTO {

    @Schema(description = "ID del proyecto para acotar el reporte", example = "1")
    private Long proyectoId;

    @Schema(description = "ID del estado de lote (1=Disponible, 2=Reservado, 3=Vendido)", example = "1")
    private Integer estadoLoteId;

    @Schema(description = "ID del estado de solicitud de contacto", example = "1")
    private Integer estadoSolicitudId;

    @Schema(description = "Nombre del estado del proyecto", example = "LANZAMIENTO")
    private String estadoProyecto;

    @Schema(description = "Fecha inicial del rango de consulta (YYYY-MM-DD)", example = "2026-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaInicio;

    @Schema(description = "Fecha final del rango de consulta (YYYY-MM-DD)", example = "2026-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFin;

    @AssertTrue(message = "La fecha inicial no puede ser posterior a la fecha final")
    public boolean isRangoFechasValido() {
        if (fechaInicio == null || fechaFin == null) {
            return true;
        }
        return !fechaInicio.isAfter(fechaFin);
    }
}