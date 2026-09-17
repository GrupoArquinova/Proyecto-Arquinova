package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Datos de respuesta con el detalle de una etapa")
public class EtapaResponseDTO {

    @Schema(description = "Identificador único de la etapa", example = "1")
    private Long id;
    
    @Schema(description = "ID del proyecto asociado", example = "1")
    private Long proyectoId;
    
    @Schema(description = "Nombre del proyecto asociado", example = "Residencial Bosques del Café")
    private String proyectoNombre;
    
    @Schema(description = "Nombre de la etapa", example = "Etapa 1 - El Roble")
    private String nombre;
    
    @Schema(description = "Descripción de la etapa", example = "Primera fase de entrega con lotes totalmente urbanizados.")
    private String descripcion;
    
    @Schema(description = "Orden de visualización", example = "1")
    private Short orden;
    
    @Schema(description = "Estado de la etapa", example = "true")
    private Boolean activo;
    
    @Schema(description = "Fecha de creación", example = "2026-09-11T10:30:00")
    private LocalDateTime creadoEn;
}
