package com.constructora_backend.dto.response;

import com.constructora_backend.enums.EstadoProyecto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "Datos de respuesta con el detalle de un proyecto inmobiliario")
public class ProyectoResponseDTO {

    @Schema(description = "Identificador único del proyecto", example = "1")
    private Long id;

    @Schema(description = "ID de la empresa constructora", example = "1")
    private Long empresaId;

    @Schema(description = "Nombre de la empresa constructora", example = "Constructora Conclave")
    private String empresaNombre;

    @Schema(description = "Nombre comercial del proyecto", example = "Condominio Campestre Los Álamos")
    private String nombre;

    @Schema(description = "Slug amigable para URL", example = "condominio-campestre-los-alamos")
    private String slug;

    @Schema(description = "Descripción detallada del proyecto", example = "Exclusivo proyecto de lotes y casas campestres.")
    private String descripcion;

    @Schema(description = "Estado actual del proyecto", example = "PLANIFICACION")
    private EstadoProyecto estadoProyecto;

    @Schema(description = "Indica si el proyecto está publicado", example = "true")
    private Boolean publicado;

    @Schema(description = "Indica si el proyecto está activo", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha de lanzamiento del proyecto", example = "2026-11-01")
    private LocalDate fechaLanzamiento;

    @Schema(description = "ID del usuario que creó el proyecto", example = "1")
    private Long creadoPorId;

    @Schema(description = "Nombre completo del usuario creador", example = "Juan David")
    private String creadoPorNombre;

    @Schema(description = "ID del usuario que realizó la última actualización", example = "1")
    private Long actualizadoPorId;

    @Schema(description = "Nombre completo del usuario que actualizó", example = "Juan David")
    private String actualizadoPorNombre;

    @Schema(description = "Fecha y hora de creación del registro", example = "2026-09-11T10:30:00")
    private LocalDateTime creadoEn;

    @Schema(description = "Fecha y hora de última actualización", example = "2026-09-11T10:30:00")
    private LocalDateTime actualizadoEn;
}
