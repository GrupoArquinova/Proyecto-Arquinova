package com.constructora_backend.controller;

import com.constructora_backend.dto.*;
import com.constructora_backend.dto.response.DashboardResponseDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.ReporteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReporteController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteService reporteService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("GET /api/reportes/dashboard - Retorna métricas globales con HTTP 200")
    @WithMockUser(roles = "ADMINISTRADOR")
    void obtenerDashboard_Exitoso() throws Exception {
        DashboardResponseDTO dto = DashboardResponseDTO.builder()
                .totalProyectos(10L)
                .proyectosPublicados(8L)
                .totalLotes(50L)
                .lotesDisponibles(30L)
                .lotesReservados(10L)
                .lotesVendidos(10L)
                .totalSolicitudes(25L)
                .solicitudesNuevas(5L)
                .distribucionLotesPorEstado(Map.of("DISPONIBLE", 30L))
                .distribucionSolicitudesPorEstado(Map.of("NUEVA", 5L))
                .build();

        when(reporteService.obtenerDashboard()).thenReturn(dto);

        mockMvc.perform(get("/api/reportes/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProyectos").value(10))
                .andExpect(jsonPath("$.lotesDisponibles").value(30));
    }

    @Test
    @DisplayName("GET /api/reportes/proyectos - Retorna lista de proyectos con HTTP 200")
    @WithMockUser(roles = "ADMINISTRADOR")
    void consultarReporteProyectos_Exitoso() throws Exception {
        ReporteProyectoItemDTO item = new ReporteProyectoItemDTO(
                1L, "Constructora", "Altos del Sol", "LANZAMIENTO", true, true, LocalDate.now(), 20L, LocalDateTime.now()
        );
        when(reporteService.consultarReporteProyectos(any(FiltroReporteDTO.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/reportes/proyectos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Altos del Sol"))
                .andExpect(jsonPath("$[0].totalLotes").value(20));
    }

    @Test
    @DisplayName("GET /api/reportes/lotes - Retorna reporte de lotes con HTTP 200")
    @WithMockUser(roles = "ADMINISTRADOR")
    void consultarReporteLotes_Exitoso() throws Exception {
        ReporteLoteItemDTO lote = new ReporteLoteItemDTO(
                10L, "Altos del Sol", "Etapa 1", "L-01", "Lote 01", new BigDecimal("150.00"), "DISPONIBLE", true, true, LocalDateTime.now()
        );
        when(reporteService.consultarReporteLotes(any(FiltroReporteDTO.class)))
                .thenReturn(List.of(lote));

        mockMvc.perform(get("/api/reportes/lotes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("L-01"))
                .andExpect(jsonPath("$[0].estadoComercial").value("DISPONIBLE"));
    }

    @Test
    @DisplayName("GET /api/reportes/estados-comerciales - Retorna estadísticas comerciales con HTTP 200")
    @WithMockUser(roles = "ADMINISTRADOR")
    void consultarEstadisticasEstadosComerciales_Exitoso() throws Exception {
        EstadisticaEstadoComercialDTO stat = new EstadisticaEstadoComercialDTO(
                1L, "Altos del Sol", "DISPONIBLE", 15L, new BigDecimal("75.00")
        );
        when(reporteService.consultarEstadisticasEstadosComerciales(any(FiltroReporteDTO.class)))
                .thenReturn(List.of(stat));

        mockMvc.perform(get("/api/reportes/estados-comerciales")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].proyectoNombre").value("Altos del Sol"))
                .andExpect(jsonPath("$[0].porcentaje").value(75.00));
    }

    @Test
    @DisplayName("GET /api/reportes/solicitudes - Retorna lista de solicitudes con HTTP 200")
    @WithMockUser(roles = "ADMINISTRADOR")
    void consultarReporteSolicitudes_Exitoso() throws Exception {
        ReporteSolicitudItemDTO item = new ReporteSolicitudItemDTO(
                1L, "Altos del Sol", "L-01", "Carlos Pérez", "carlos@correo.com", "3001234567",
                "NUEVA", "Asesor Comercial", LocalDateTime.now()
        );
        when(reporteService.consultarReporteSolicitudes(any(FiltroReporteDTO.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/reportes/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clienteNombre").value("Carlos Pérez"))
                .andExpect(jsonPath("$[0].estadoNombre").value("NUEVA"));
    }

    @Test
    @DisplayName("GET /api/reportes/actividad - Retorna historial de auditoría con HTTP 200")
    @WithMockUser(roles = "ADMINISTRADOR")
    void consultarReporteActividad_Exitoso() throws Exception {
        ReporteActividadItemDTO item = new ReporteActividadItemDTO(
                5L, "Admin User", "admin@constructora.com", "CREAR_PROYECTO", "PROYECTOS", 1L,
                "Proyecto creado", "127.0.0.1", LocalDateTime.now()
        );
        when(reporteService.consultarReporteActividad(any(FiltroReporteDTO.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/reportes/actividad")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("CREAR_PROYECTO"))
                .andExpect(jsonPath("$[0].entidad").value("PROYECTOS"));
    }
}
