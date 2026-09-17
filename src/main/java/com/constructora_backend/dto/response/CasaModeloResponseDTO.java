package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Datos de respuesta de una casa modelo")
public class CasaModeloResponseDTO {

    @Schema(description = "Identificador único", example = "1")
    private Long id;
    
    @Schema(description = "ID del proyecto asociado", example = "1")
    private Long proyectoId;
    
    @Schema(description = "Nombre del proyecto", example = "Residencial Bosques del Café")
    private String proyectoNombre;
    
    @Schema(description = "Nombre de la casa modelo", example = "Casa Tipo A")
    private String nombre;
    
    @Schema(description = "Descripción", example = "Casa de 2 niveles")
    private String descripcion;
    
    @Schema(description = "Área construida en m2", example = "120.5")
    private BigDecimal areaConstruidaM2;
    
    @Schema(description = "Número de habitaciones", example = "3")
    private Byte numeroHabitaciones;
    
    @Schema(description = "Número de baños", example = "2")
    private Byte numeroBanos;
    
    @Schema(description = "URL del tour virtual", example = "https://tourvirtual.com/casaA")
    private String tourVirtualUrl;
    
    @Schema(description = "URL del plano", example = "https://planos.com/casaA.pdf")
    private String planoUrl;
    
    @Schema(description = "Indica si está publicada", example = "true")
    private Boolean publicado;
    
    @Schema(description = "Indica si está activa", example = "true")
    private Boolean activo;
    
    @Schema(description = "Fecha de creación", example = "2026-09-11T10:30:00")
    private LocalDateTime creadoEn;
    
    @Schema(description = "Fecha de actualización", example = "2026-09-11T10:30:00")
    private LocalDateTime actualizadoEn;
}
