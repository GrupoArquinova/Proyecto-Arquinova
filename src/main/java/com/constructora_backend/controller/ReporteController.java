package com.constructora_backend.controller;

import com.constructora_backend.dto.*;
import com.constructora_backend.dto.response.DashboardResponseDTO;
import com.constructora_backend.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    // RF22 - Dashboard de indicadores
    @GetMapping("/dashboard")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<DashboardResponseDTO> obtenerDashboard() {
        return ResponseEntity.ok(reporteService.obtenerDashboard());
    }

    // RF23 - Reporte de proyectos
    @GetMapping("/proyectos")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ReporteProyectoItemDTO>> consultarReporteProyectos(@ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarReporteProyectos(filtro));
    }

    // RF24 - Reporte de lotes
    @GetMapping("/lotes")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ReporteLoteItemDTO>> consultarReporteLotes(@ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarReporteLotes(filtro));
    }

    // RF25 - Reporte de estados comerciales
    @GetMapping("/estados-comerciales")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<EstadisticaEstadoComercialDTO>> consultarEstadisticasEstadosComerciales(@ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarEstadisticasEstadosComerciales(filtro));
    }

    // RF26 - Reporte de solicitudes de contacto
    @GetMapping("/solicitudes")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ReporteSolicitudItemDTO>> consultarReporteSolicitudes(@ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarReporteSolicitudes(filtro));
    }

    // RF29 - Reporte de actividad administrativa
    @GetMapping("/actividad")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ReporteActividadItemDTO>> consultarReporteActividad(@ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarReporteActividad(filtro));
    }
}