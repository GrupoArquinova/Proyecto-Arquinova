package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Elimina del historial todo lo que coincida con estos filtros (los mismos de la consulta). Sin ningún filtro se borra TODO el historial y hay que confirmarlo.")
public class EliminarHistorialFiltroDTO {

    @Schema(description = "Quien hizo el cambio", example = "3")
    private Long usuarioId;

    @Schema(description = "Entidad afectada", example = "REPORTES")
    private String entidad;

    @Schema(description = "Acción", example = "CONSULTAR_REPORTE_LOTES")
    private String accion;

    @Schema(description = "Texto dentro de la descripción")
    private String texto;

    @Schema(description = "Primer día incluido (YYYY-MM-DD)", example = "2026-10-01")
    private LocalDate desde;

    @Schema(description = "Último día incluido (YYYY-MM-DD)", example = "2026-10-31")
    private LocalDate hasta;

    @Schema(description = "Obligatorio en true cuando no hay ningún filtro (borrar todo el historial)")
    private boolean confirmarTodo;
}
