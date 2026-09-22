package com.constructora_backend.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class FiltroReporteDTO {

    private Long proyectoId;
    private Integer estadoLoteId;
    private Integer estadoSolicitudId;
    private String estadoProyecto;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFin;
}