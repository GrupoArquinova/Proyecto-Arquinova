package com.constructora_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteProyectoItemDTO {

    private Long id;
    private String empresaNombre;
    private String nombre;
    private String estadoProyecto;
    private Boolean publicado;
    private Boolean activo;
    private LocalDate fechaLanzamiento;
    private Long totalLotes;
    private LocalDateTime creadoEn;
}