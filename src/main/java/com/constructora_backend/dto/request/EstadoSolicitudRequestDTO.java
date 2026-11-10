package com.constructora_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EstadoSolicitudRequestDTO {

    @NotBlank(message = "El nombre del estado de solicitud es obligatorio")
    @Size(max = 40, message = "El nombre no puede superar los 40 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres")
    private String descripcion;

    @NotNull(message = "El orden es obligatorio")
    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    private Integer orden = 1;

    private Boolean activo = true;
}
