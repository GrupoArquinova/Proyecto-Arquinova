package com.constructora_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CasaModeloRequestDTO {

    @NotNull(message = "El ID del proyecto es obligatorio")
    private Long proyectoId;

    @NotBlank(message = "El nombre de la casa modelo es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    private String descripcion;

    @DecimalMin(value = "0.01", message = "El area construida debe ser mayor a 0")
    private BigDecimal areaContruidaM2;

    @Min(value = 1, message = "El numero de habitaciones debe ser al menos 1")
    private Byte numeroHabitaciones;

    @Min(value = 1, message = "El numero de baños debe ser al menos 1")
    private Byte numeroBanos;

    @Size(max = 1000, message = "La URL del tour virtual no puede superar los 1000 caracteres")
    private String tourVirtualUrl;

    @Size(max = 1000, message = "La URL del plano no puede superar los 1000 caracteres")
    private String planoUrl;

    private Boolean publicado = true;

    private Boolean activo = true;
}
