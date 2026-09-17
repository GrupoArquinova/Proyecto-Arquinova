package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para crear o actualizar una imagen de zona común")
public class ZonaComunImagenRequestDTO {

    @NotNull(message = "El ID de la zona comun es obligatorio")
    @Schema(description = "ID de la zona común asociada", example = "10")
    private Long zonaComunId;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    @Size(max = 1000, message = "La URL de la imagen no puede superar los 1000 caracteres")
    @Schema(description = "URL de la imagen", example = "https://misitio.com/imagenes/piscina.jpg")
    private String imagenUrl;

    @Size(max = 150, message = "El titulo no puede superar los 150 caracteres")
    @Schema(description = "Título o descripción corta de la imagen", example = "Piscina Principal")
    private String titulo;

    @NotNull(message = "El orden es obligatorio")
    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    @Schema(description = "Orden de visualización", example = "1")
    private Short orden = 1;

    @Schema(description = "Indica si es la imagen principal de la zona común", example = "true")
    private Boolean esPrincipal = false;
}
