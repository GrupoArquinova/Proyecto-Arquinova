package com.constructora_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Datos para la generación de minuta contractual en formato Word (.docx)")
public class GenerarMinutaDTO {

    @Schema(description = "ID del lote objeto del contrato", example = "5")
    @NotNull(message = "El ID del lote es obligatorio")
    private Long loteId;

    @Schema(description = "Nombre completo del comprador o promitente comprador", example = "Juan Pérez González")
    @NotBlank(message = "El nombre completo del comprador es obligatorio")
    private String compradorNombre;

    @Schema(description = "Número de cédula o documento de identidad del comprador (solo dígitos)", example = "1098765432")
    @NotBlank(message = "El documento de identidad del comprador es obligatorio")
    @Pattern(regexp = "\\d{5,15}", message = "La cédula debe contener entre 5 y 15 dígitos numéricos")
    private String compradorCedula;
}