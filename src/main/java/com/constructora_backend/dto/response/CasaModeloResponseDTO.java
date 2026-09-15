package com.constructora_backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CasaModeloResponseDTO {

    private Long id;
    private Long proyectoId;
    private String proyectoNombre;
    private String nombre;
    private String descripcion;
    private BigDecimal areaConstruidaM2;
    private Byte numeroHabitaciones;
    private Byte numeroBanos;
    private String tourVirtualUrl;
    private String planoUrl;
    private Boolean publicado;
    private Boolean activo;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
