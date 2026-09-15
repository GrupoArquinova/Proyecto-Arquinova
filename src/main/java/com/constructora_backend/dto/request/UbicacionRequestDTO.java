package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Datos para la creación o actualización de la ubicación y recursos multimedia de un proyecto")
public class UbicacionRequestDTO {

    @NotNull(message = "El ID del proyecto es obligatorio")
    @Schema(description = "ID del proyecto al que se asocia la ubicación (relación 1 a 1)", example = "1")
    private Long proyectoId;

    @Size(max = 255, message = "La direccion no puede superar los 255 caracteres")
    @Schema(description = "Dirección física o referencia vial", example = "Km 5 Vía Armenia - La Tebaida")
    private String direccion;

    @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
    @Schema(description = "Ciudad o municipio", example = "Armenia")
    private String ciudad;

    @Size(max = 100, message = "El departamento no puede superar los 100 caracteres")
    @Schema(description = "Departamento o estado", example = "Quindío")
    private String departamento;

    @Schema(description = "Puntos de referencia o indicaciones adicionales de llegada", example = "A 500 metros después del Club Campestre, costado oriental.")
    private String referencia;

    @DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
    @DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
    @Schema(description = "Latitud geográfica en grados decimales (-90 a 90)", example = "4.5388890")
    private BigDecimal latitud;

    @DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180")
    @DecimalMax(value = "180.0", message = "La longitud debe ser menos o igual a 180")
    @Schema(description = "Longitud geográfica en grados decimales (-180 a 180)", example = "-75.6727780")
    private BigDecimal longitud;

    @Size(max = 1000, message = "La URL de Google Maps no puede superar los 1000 caracteres")
    @Schema(description = "URL o iframe para Google Maps", example = "https://maps.google.com/?q=4.5388890,-75.6727780")
    private String googleMapUrl;

    @Size(max = 1000, message = "La URL de Urbanismo no puede superar los 1000 caracteres")
    @Schema(description = "URL del plano de urbanismo o render general", example = "https://example.com/urbanismo/plano-general.jpg")
    private String urbanismoUrl;

    @Size(max = 1000, message = "La URL de vista aerea no puede superar los 1000 caracteres")
    @Schema(description = "URL del video o imagen aérea con dron", example = "https://example.com/videos/vista-aerea.mp4")
    private String vistaAereaUrl;

    @Size(max = 1000, message = "La URL de Recorrido 360 no puede superar los 1000 caracteres")
    @Schema(description = "URL del recorrido interactivo 360 virtual", example = "https://example.com/tours/recorrido360")
    private String recorrido360Url;

    @Size(max = 1000, message = "La Url de video como llegar no puede superar los 1000 caracteres")
    @Schema(description = "URL del video explicativo 'Cómo llegar'", example = "https://youtube.com/watch?v=example")
    private String videoComoLlegarUrl;
}
