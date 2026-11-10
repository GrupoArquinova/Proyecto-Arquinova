package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Datos de respuesta de una imagen de zona común")
public class ZonaComunImagenResponseDTO {

    @Schema(description = "Identificador único de la imagen", example = "1")
    private Long id;

    @Schema(description = "ID de la zona común asociada", example = "10")
    private Long zonaComunId;

    @Schema(description = "Nombre de la zona común asociada", example = "Piscina")
    private String zonaComunNombre;

    @Schema(description = "URL de la imagen", example = "https://misitio.com/imagenes/piscina.jpg")
    private String imagenUrl;

    @Schema(description = "Título o descripción corta de la imagen", example = "Piscina Principal")
    private String titulo;

    @Schema(description = "Orden de visualización", example = "1")
    private Short orden;

    @Schema(description = "Indica si es la imagen principal", example = "true")
    private Boolean esPrincipal;

    @Schema(description = "Fecha y hora de creación", example = "2026-09-16T10:00:00")
    private LocalDateTime creadoEn;
}
