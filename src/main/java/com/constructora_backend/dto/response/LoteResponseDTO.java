package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Datos de respuesta del detalle de un lote")
public class LoteResponseDTO {

    @Schema(description = "Identificador único", example = "1")
    private Long id;
    
    @Schema(description = "ID de la etapa asociada", example = "1")
    private Long etapaId;
    
    @Schema(description = "Nombre de la etapa", example = "Etapa 1 - El Roble")
    private String etapaNombre;
    
    @Schema(description = "ID del estado del lote", example = "1")
    private Integer estadoId;
    
    @Schema(description = "Nombre del estado", example = "DISPONIBLE")
    private String estadoNombre;
    
    @Schema(description = "Código del lote", example = "LT-101")
    private String codigo;
    
    @Schema(description = "Nombre opcional del lote", example = "Lote Esquinero 101")
    private String nombre;
    
    @Schema(description = "Área en m2", example = "1200.00")
    private BigDecimal areaM2;
    
    @Schema(description = "Precio (si aplica)")
    private BigDecimal precio;
    
    @Schema(description = "Descripción del lote")
    private String descripcion;
    
    @Schema(description = "Características")
    private String caracteristicas;
    
    @Schema(description = "Posición X en el plano")
    private BigDecimal posicionX;
    
    @Schema(description = "Posición Y en el plano")
    private BigDecimal posicionY;
    
    @Schema(description = "Estado de publicación", example = "true")
    private Boolean publicado;
    
    @Schema(description = "Estado lógico", example = "true")
    private Boolean activo;
    
    @Schema(description = "ID del usuario creador")
    private Long creadoPorId;
    
    @Schema(description = "Nombre del usuario creador")
    private String creadoPorNombre;
    
    @Schema(description = "ID del usuario actualizador")
    private Long actualizadoPorId;
    
    @Schema(description = "Nombre del usuario actualizador")
    private String actualizadoPorNombre;
    
    @Schema(description = "Fecha de creación")
    private LocalDateTime creadoEn;
    
    @Schema(description = "Fecha de última actualización")
    private LocalDateTime actualizadoEn;
}
