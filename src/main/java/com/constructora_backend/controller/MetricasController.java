package com.constructora_backend.controller;

import com.constructora_backend.dto.response.MetricasDTO;
import com.constructora_backend.security.Permisos;
import com.constructora_backend.service.MetricasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Métricas del dashboard: solicitudes, lotes, analítica sin cookies y preguntas del asistente. Solo lectura. La ven
 * administradores y comerciales; las últimas preguntas del asistente (texto) solo las ve el administrador.
 */
@RestController
@RequestMapping("/api/admin/metricas")
@Tag(name = "Métricas", description = "Dashboard de métricas del panel (administrador y comercial, solo lectura)")
@SecurityRequirement(name = "bearerAuth")
public class MetricasController {

    private final MetricasService metricasService;

    @Autowired
    public MetricasController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping
    @PreAuthorize(Permisos.ADMIN_O_COMERCIAL)
    @Operation(summary = "Métricas del dashboard",
            description = "Periodo por defecto: últimos 90 días. Las semanas y los días sin datos vienen con 0. "
                    + "chat.recientes solo se llena para el ADMINISTRADOR (para el COMERCIAL va vacía). Máximo 731 días.")
    public ResponseEntity<MetricasDTO> obtener(
            @Parameter(description = "Primer día incluido (YYYY-MM-DD)", example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Último día incluido (YYYY-MM-DD)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {
        boolean esAdmin = Permisos.tieneAlgunRol(authentication, Permisos.ADMINISTRADOR);
        return ResponseEntity.ok(metricasService.obtener(desde, hasta, esAdmin));
    }
}
