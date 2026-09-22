package com.constructora_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticaEstadoComercialDTO {

    private Long proyectoId;
    private String proyectoNombre;
    private String estadoNombre;
    private Long cantidadLotes;
    private BigDecimal porcentaje;
}