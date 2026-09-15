package com.constructora_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ZonaComunRequestDTO {

    @NotNull(message = "El ID del proyecto es obligatorio")
    private Long proyectoId;

    @NotBlank(message = "El nombre de la zona comun es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    private String descripcion;

    private Boolean publicado = true;

    private Boolean activo = true;
}
