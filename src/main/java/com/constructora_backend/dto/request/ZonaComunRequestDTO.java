package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para la creación o actualización de una zona común")
public class ZonaComunRequestDTO {

    @NotNull(message = "El ID del proyecto es obligatorio")
    @Schema(description = "ID del proyecto al que pertenece la zona común", example = "1")
    private Long proyectoId;

    @NotBlank(message = "El nombre de la zona comun es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Schema(description = "Nombre de la zona común", example = "Piscina y zonas húmedas")
    private String nombre;

    @Schema(description = "Descripción detallada de la zona común", example = "Amplia piscina para adultos y niños con calefacción.")
    private String descripcion;

    @Schema(description = "Indica si la zona común está publicada", example = "true")
    private Boolean publicado = true;

    @Schema(description = "Indica si la zona común está activa", example = "true")
    private Boolean activo = true;
}
