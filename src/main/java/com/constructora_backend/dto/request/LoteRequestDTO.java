package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Datos para la creación o actualización de un lote")
public class LoteRequestDTO {

    @NotNull(message = "El ID de la etapa es obligatorio")
    @Schema(description = "ID de la etapa a la que pertenece el lote", example = "1")
    private Long etapaId;

    @NotNull(message = "El ID del estado del lote es obligatorio")
    @Schema(description = "ID del estado del lote", example = "1")
    private Integer estadoId;

    @NotBlank(message = "El codigo del lote es obligatorio")
    @Size(max = 50, message = "El codigo no puede superar los 50 caracteres")
    @Schema(description = "Código único o identificador del lote", example = "LT-101")
    private String codigo;

    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    @Schema(description = "Nombre opcional del lote", example = "Lote Esquinero 101")
    private String nombre;

    @NotNull(message = "El area en m2 es obligatoria")
    @DecimalMin(value = "0.01", message = "El area debe ser mayor a 0")
    @Schema(description = "Área total en metros cuadrados", example = "1200.00")
    private BigDecimal areaM2;

    @Schema(description = "Descripción general del lote", example = "Lote plano con excelente vista al atardecer.")
    private String descripcion;

    @Schema(description = "Características específicas o amenidades cercanas")
    private String caracteristicas;

    @DecimalMin(value = "0.00", message = "La posicion X no puede ser negativa")
    @Schema(description = "Coordenada X en el plano interactivo")
    private BigDecimal posicionX;

    @DecimalMin(value = "0.00", message = "La posicion Y no puede ser negativa")
    @Schema(description = "Coordenada Y en el plano interactivo")
    private BigDecimal posicionY;

    @Schema(description = "Indica si el lote está publicado", example = "true")
    private Boolean publicado = true;

    @Schema(description = "Indica si el lote está activo en el sistema", example = "true")
    private Boolean activo = true;

    @Schema(description = "ID del usuario que realiza la creación o modificación", example = "1")
    private Long usuarioId;
}
