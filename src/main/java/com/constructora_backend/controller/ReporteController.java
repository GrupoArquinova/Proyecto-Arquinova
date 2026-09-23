package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.*;
import com.constructora_backend.dto.response.DashboardResponseDTO;
import com.constructora_backend.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Endpoints para generación de dashboard gerencial y reportes analíticos del sistema")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

    private final ReporteService reporteService;

    @Autowired
    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // RF22 - Dashboard de indicadores
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CONSULTAR_DASHBOARD", entidad = "REPORTES", descripcion = "Consulta de estadísticas del dashboard gerencial")
    @Operation(summary = "Consultar dashboard de indicadores", description = "Retorna el resumen de métricas clave: total proyectos, lotes por estado y solicitudes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = DashboardResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<DashboardResponseDTO> obtenerDashboard() {
        return ResponseEntity.ok(reporteService.obtenerDashboard());
    }

    // RF23 - Reporte de proyectos
    @GetMapping("/proyectos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CONSULTAR_REPORTE_PROYECTOS", entidad = "REPORTES", descripcion = "Consulta de reporte consolidado de proyectos")
    @Operation(summary = "Reporte de proyectos", description = "Lista proyectos con estadísticas de lotes asociados, filtrado por fechas o estado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte de proyectos obtenido",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReporteProyectoItemDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<List<ReporteProyectoItemDTO>> consultarReporteProyectos(
            @Parameter(description = "Filtros opcionales para la consulta") @ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarReporteProyectos(filtro));
    }

    // RF24 - Reporte de lotes
    @GetMapping("/lotes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CONSULTAR_REPORTE_LOTES", entidad = "REPORTES", descripcion = "Consulta de reporte de inventario de lotes")
    @Operation(summary = "Reporte de lotes", description = "Obtiene el inventario detallado de lotes con filtros por proyecto, etapa, estado y fechas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte de lotes obtenido",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReporteLoteItemDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<List<ReporteLoteItemDTO>> consultarReporteLotes(
            @Parameter(description = "Filtros opcionales de proyecto, estado comercial y rango de fechas") @ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarReporteLotes(filtro));
    }

    // RF25 - Reporte de estados comerciales
    @GetMapping("/estados-comerciales")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CONSULTAR_REPORTE_ESTADOS", entidad = "REPORTES", descripcion = "Consulta de distribución y porcentajes de estados comerciales")
    @Operation(summary = "Reporte de estados comerciales de lotes", description = "Calcula distribución y porcentaje de lotes disponibles, reservados y vendidos por proyecto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas calculadas exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EstadisticaEstadoComercialDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<List<EstadisticaEstadoComercialDTO>> consultarEstadisticasEstadosComerciales(
            @Parameter(description = "Filtro opcional por proyecto") @ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarEstadisticasEstadosComerciales(filtro));
    }

    // RF26 - Reporte de solicitudes de contacto
    @GetMapping("/solicitudes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CONSULTAR_REPORTE_SOLICITUDES", entidad = "REPORTES", descripcion = "Consulta de reporte de solicitudes de contacto recibidas")
    @Operation(summary = "Reporte de solicitudes de contacto", description = "Lista solicitudes de prospectos recibidas a través de la web con estado de atención.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte de solicitudes obtenido",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReporteSolicitudItemDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<List<ReporteSolicitudItemDTO>> consultarReporteSolicitudes(
            @Parameter(description = "Filtro por proyecto, estado de solicitud y fechas") @ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarReporteSolicitudes(filtro));
    }

    // RF29 - Reporte de actividad administrativa
    @GetMapping("/actividad")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CONSULTAR_REPORTE_ACTIVIDAD", entidad = "REPORTES", descripcion = "Consulta de historial de auditoría y actividad administrativa")
    @Operation(summary = "Reporte de actividad administrativa", description = "Historial auditable de eventos administrativos registrados por usuario y fecha.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte de actividad obtenido",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReporteActividadItemDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<List<ReporteActividadItemDTO>> consultarReporteActividad(
            @Parameter(description = "Filtro por rango de fechas") @ModelAttribute FiltroReporteDTO filtro) {
        return ResponseEntity.ok(reporteService.consultarReporteActividad(filtro));
    }
}