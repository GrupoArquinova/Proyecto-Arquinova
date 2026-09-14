package com.constructora_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ZonaComunImagenRequestDTO {

    @NotNull(message = "El ID de la zona comun es obligatorio")
    private Long zonaComunId;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    @Size(max = 1000, message = "La URL de la imagen no puede superar los 1000 caracteres")
    private String imagenUrl;

    @Size(max = 150, message = "El titulo no puede superar los 150 caracteres")
    private String titulo;

    @NotNull(message = "El orden es obligatorio")
    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    private Short orden = 1;

    private Boolean esPrincipal = false;
}
