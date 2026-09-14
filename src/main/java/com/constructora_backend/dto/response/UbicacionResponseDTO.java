package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Datos de respuesta con los detalles de ubicación y recursos multimedia de un proyecto")
public class UbicacionResponseDTO {

    @Schema(description = "Identificador único del registro de ubicación", example = "1")
    private Long id;

    @Schema(description = "ID del proyecto asociado", example = "1")
    private Long proyectoId;

    @Schema(description = "Nombre comercial del proyecto asociado", example = "Condominio Campestre Los Álamos")
    private String proyectoNombre;

    @Schema(description = "Dirección física o referencia vial", example = "Km 5 Vía Armenia - La Tebaida")
    private String direccion;

    @Schema(description = "Ciudad o municipio", example = "Armenia")
    private String ciudad;

    @Schema(description = "Departamento o estado", example = "Quindío")
    private String departamento;

    @Schema(description = "Puntos de referencia de llegada", example = "A 500 metros después del Club Campestre")
    private String referencias;

    @Schema(description = "Latitud geográfica", example = "4.5388890")
    private BigDecimal latitud;

    @Schema(description = "Longitud geográfica", example = "-75.6727780")
    private BigDecimal longitud;

    @Schema(description = "URL o iframe de Google Maps", example = "https://maps.google.com/?q=4.5388890,-75.6727780")
    private String googleMapsUrl;

    @Schema(description = "URL del plano de urbanismo", example = "https://example.com/urbanismo/plano-general.jpg")
    private String urbanismoUrl;

    @Schema(description = "URL de vista aérea", example = "https://example.com/videos/vista-aerea.mp4")
    private String vistaAereaUrl;

    @Schema(description = "URL del recorrido 360", example = "https://example.com/tours/recorrido360")
    private String recorrido360Url;

    @Schema(description = "URL del video explicativo 'Cómo llegar'", example = "https://youtube.com/watch?v=example")
    private String videoComoLlegarUrl;

    @Schema(description = "Fecha y hora de creación del registro", example = "2026-09-11T16:00:00")
    private LocalDateTime creadoEn;

    @Schema(description = "Fecha y hora de última actualización del registro", example = "2026-09-11T16:00:00")
    private LocalDateTime actualizadoEn;
}
