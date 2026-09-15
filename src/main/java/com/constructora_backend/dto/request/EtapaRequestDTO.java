package com.constructora_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EtapaRequestDTO {

    @NotNull(message = "El ID del proyecto es obligatorio")
    private Long proyectoId;

    @NotBlank(message = "El nombre de la etapa es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El orden es obligatorio")
    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    private Short orden = 1;

    private Boolean activo = true;
}
