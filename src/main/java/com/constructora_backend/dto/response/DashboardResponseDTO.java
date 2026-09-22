package com.constructora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DashboardResponseDTO {

    private long totalProyectos;
    private long proyectosPublicados;
    private long totalLotes;
    private long lotesDisponibles;
    private long lotesReservados;
    private long lotesVendidos;
    private long totalSolicitudes;
    private long solicitudesNuevas;
    private Map<String, Long> distribucionLotesPorEstado;
    private Map<String, Long> distribucionSolicitudesPorEstado;
}