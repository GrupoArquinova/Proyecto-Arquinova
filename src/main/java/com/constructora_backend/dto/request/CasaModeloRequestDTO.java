package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Datos para la creación o actualización de una casa modelo")
public class CasaModeloRequestDTO {

    @NotNull(message = "El ID del proyecto es obligatorio")
    @Schema(description = "ID del proyecto asociado", example = "1")
    private Long proyectoId;

    @NotBlank(message = "El nombre de la casa modelo es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Schema(description = "Nombre de la casa modelo", example = "Casa Tipo A")
    private String nombre;

    @Schema(description = "Descripción de la casa modelo", example = "Casa de 2 niveles con diseño moderno")
    private String descripcion;

    @DecimalMin(value = "0.01", message = "El area construida debe ser mayor a 0")
    @Schema(description = "Área construida en metros cuadrados", example = "120.5")
    private BigDecimal areaConstruidaM2;

    @Min(value = 1, message = "El numero de habitaciones debe ser al menos 1")
    @Schema(description = "Número de habitaciones", example = "3")
    private Byte numeroHabitaciones;

    @Min(value = 1, message = "El numero de baños debe ser al menos 1")
    @Schema(description = "Número de baños", example = "2")
    private Byte numeroBanos;

    @Size(max = 1000, message = "La URL del tour virtual no puede superar los 1000 caracteres")
    @Schema(description = "URL para acceder al tour virtual", example = "https://tourvirtual.com/casaA")
    private String tourVirtualUrl;

    @Size(max = 1000, message = "La URL del plano no puede superar los 1000 caracteres")
    @Schema(description = "URL del plano arquitectónico", example = "https://planos.com/casaA.pdf")
    private String planoUrl;

    @Schema(description = "Indica si está publicada", example = "true")
    private Boolean publicado = true;

    @Schema(description = "Indica si está activa", example = "true")
    private Boolean activo = true;
}
