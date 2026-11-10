package com.constructora_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Una fila del reporte de etapas: lotes de la etapa por estado comercial. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteEtapaItemDTO {

    private Long proyectoId;
    private String proyectoNombre;
    private Long etapaId;
    private String etapaNombre;
    private Long totalLotes;
    private Long disponibles;
    private Long reservados;
    private Long vendidos;
    private BigDecimal areaTotalM2;
}
