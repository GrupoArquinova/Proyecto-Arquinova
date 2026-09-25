package com.constructora_backend.dto.request;

import com.constructora_backend.enums.EstadoProyecto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Datos para la creación o actualización de un proyecto inmobiliario")
public class ProyectoRequestDTO {

    @NotNull(message = "La empresa es obligatoria")
    @Schema(description = "ID de la empresa a la que pertenece el proyecto", example = "1")
    private Long empresaId;

    @NotBlank(message = "El nombre del proyecto es obligatorio")
    @Size(max = 180, message = "El nombre no puede superar los 180 caracteres")
    @Schema(description = "Nombre comercial del proyecto", example = "Condominio Campestre Los Álamos")
    private String nombre;

    @NotBlank(message = "El slug es obligatorio")
    @Size(max = 200, message = "El slug no puede superar los 200 caracteres")
    @Schema(description = "Slug amigable para URL (único por empresa)", example = "condominio-campestre-los-alamos")
    private String slug;

    @Schema(description = "Descripción detallada del proyecto", example = "Exclusivo proyecto de lotes y casas campestres con vista a la cordillera.")
    private String descripcion;

    @Schema(description = "Estado actual del proyecto", example = "PLANIFICACION", allowableValues = {"PLANIFICACION", "EN_CONSTRUCCION", "ENTREGADO", "FINALIZADO"})
    private EstadoProyecto estadoProyecto = EstadoProyecto.PLANIFICACION;

    @Schema(description = "Indica si el proyecto es visible al público", example = "true")
    private Boolean publicado = false;

    @Schema(description = "Estado activo/inactivo del proyecto", example = "true")
    private Boolean activo = true;

    @Schema(description = "URL de la imagen principal del proyecto (Cloudinary)", example = "https://res.cloudinary.com/...")
    private String imagenUrl;

    @Schema(description = "Fecha estimada o real de lanzamiento (YYYY-MM-DD)", example = "2026-11-01")
    private LocalDate fechaLanzamiento;

    @Schema(description = "ID del usuario creador (opcional)", example = "1")
    private Long creadoPorId;

    @Schema(description = "ID del usuario que realiza la modificación (opcional)", example = "1")
    private Long actualizadoPorId;
}
