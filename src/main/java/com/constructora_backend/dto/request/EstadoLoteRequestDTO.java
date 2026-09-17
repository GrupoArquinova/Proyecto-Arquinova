package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para la creación o actualización de un estado de lote")
public class EstadoLoteRequestDTO {

    @NotBlank(message = "El nombre del estado es obligatorio")
    @Size(max = 30, message = "El nombre no puede superar los 30 caracteres")
    @Schema(description = "Nombre del estado", example = "DISPONIBLE")
    private String nombre;

    @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres")
    @Schema(description = "Descripción del estado", example = "Lote disponible para venta")
    private String descripcion;

    @NotNull(message = "El orden es obligatorio")
    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    @Schema(description = "Orden de visualización", example = "1")
    private Integer orden = 1;

    @Schema(description = "Indica si el estado está activo", example = "true")
    private Boolean activo = true;
}
