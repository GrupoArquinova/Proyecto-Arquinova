package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Datos de respuesta con el detalle de una zona común")
public class ZonaComunResponseDTO {

    @Schema(description = "Identificador único de la zona común", example = "1")
    private Long id;
    
    @Schema(description = "ID del proyecto asociado", example = "1")
    private Long proyectoId;
    
    @Schema(description = "Nombre del proyecto asociado", example = "Condominio Campestre Los Álamos")
    private String proyectoNombre;
    
    @Schema(description = "Nombre de la zona común", example = "Piscina y zonas húmedas")
    private String nombre;
    
    @Schema(description = "Descripción detallada", example = "Amplia piscina para adultos y niños con calefacción.")
    private String descripcion;
    
    @Schema(description = "Estado de publicación", example = "true")
    private Boolean publicado;
    
    @Schema(description = "Estado de la zona común (activa/inactiva)", example = "true")
    private Boolean activo;
    
    @Schema(description = "Fecha de creación", example = "2026-09-11T10:30:00")
    private LocalDateTime creadoEn;
    
    @Schema(description = "Fecha de última actualización", example = "2026-09-11T10:30:00")
    private LocalDateTime actualizadoEn;

    @Schema(description = "Lista de imágenes asociadas a la zona común")
    private List<ZonaComunImagenResponseDTO> imagenes;

    @Schema(description = "URL de la imagen principal de la zona común", example = "https://example.com/imagen.jpg")
    private String imagenPrincipalUrl;
}
