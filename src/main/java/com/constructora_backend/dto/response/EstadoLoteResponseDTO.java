package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(description = "Datos de respuesta de un estado de lote")
public class EstadoLoteResponseDTO {

    @Schema(description = "Identificador único", example = "1")
    private Integer id;
    
    @Schema(description = "Nombre del estado", example = "DISPONIBLE")
    private String nombre;
    
    @Schema(description = "Descripción", example = "Lote disponible para venta")
    private String descripcion;
    
    @Schema(description = "Orden de visualización", example = "1")
    private Integer orden;
    
    @Schema(description = "Estado de activación", example = "true")
    private Boolean activo;
}
