package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para la creación o actualización de una etapa")
public class EtapaRequestDTO {

    @NotNull(message = "El ID del proyecto es obligatorio")
    @Schema(description = "ID del proyecto asociado", example = "1")
    private Long proyectoId;

    @NotBlank(message = "El nombre de la etapa es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Schema(description = "Nombre de la etapa", example = "Etapa 1 - El Roble")
    private String nombre;

    @Schema(description = "Descripción de la etapa", example = "Primera fase de entrega con lotes totalmente urbanizados.")
    private String descripcion;

    @NotNull(message = "El orden es obligatorio")
    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    @Schema(description = "Orden de visualización de la etapa", example = "1")
    private Short orden = 1;

    @Schema(description = "Estado de la etapa (activa/inactiva)", example = "true")
    private Boolean activo = true;
}
