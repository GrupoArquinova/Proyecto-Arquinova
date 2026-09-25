package com.constructora_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteLoteItemDTO {

    private Long id;
    private String proyectoNombre;
    private String etapaNombre;
    private String codigo;
    private String loteNombre;
    private BigDecimal areaM2;
    private String estadoComercial;
    private Boolean publicado;
    private Boolean activo;
    private LocalDateTime creadoEn;
}