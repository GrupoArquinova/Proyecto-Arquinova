package com.constructora_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GenerarMinutaDTO {

    @NotNull(message = "El ID del lote es obligatorio")
    private Long loteId;

    @NotBlank(message = "El nombre completo del comprador es obligatorio")
    private String compradorNombre;

    @NotBlank(message = "El documento de identidad del comprador es obligatorio")
    @Pattern(regexp = "\\d{5,15}", message = "La cédula debe contener solo dígitos")
    private String compradorCedula;
}