package com.constructora_backend.controller;

import com.constructora_backend.dto.response.AuditoriaFiltrosDTO;
import com.constructora_backend.dto.response.AuditoriaResponseDTO;
import com.constructora_backend.dto.response.PaginaDTO;
import com.constructora_backend.security.Permisos;
import com.constructora_backend.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Historial de cambios del panel: quién creó, editó o eliminó qué y cuándo. Lectura paginada y filtrable, solo para
 * administradores (la ruta /api/admin/** ya lo exige y cada método lo repite con @PreAuthorize).
 */
@RestController
@RequestMapping("/api/admin/auditoria")
@Tag(name = "Historial de cambios", description = "Consulta paginada del historial de cambios (solo administradores)")
@SecurityRequirement(name = "bearerAuth")
public class HistorialCambiosController {

    private final AuditoriaService auditoriaService;

    @Autowired
    public HistorialCambiosController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize(Permisos.SOLO_ADMIN)
    @Operation(summary = "Historial de cambios paginado",
            description = "Lo más reciente primero. Filtros opcionales: usuarioId, entidad (p. ej. PROYECTOS), accion (p. ej. ACTUALIZAR_PROYECTO), "
                    + "texto (en la descripción), desde y hasta (YYYY-MM-DD, ambos días incluidos). size máximo 100.")
    public ResponseEntity<PaginaDTO<AuditoriaResponseDTO>> listar(
            @Parameter(description = "ID de quien hizo el cambio") @RequestParam(required = false) Long usuarioId,
            @Parameter(description = "Entidad afectada", example = "PROYECTOS") @RequestParam(required = false) String entidad,
            @Parameter(description = "Acción", example = "ACTUALIZAR_PROYECTO") @RequestParam(required = false) String accion,
            @Parameter(description = "Texto dentro de la descripción") @RequestParam(required = false) String texto,
            @Parameter(description = "Primer día (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Último día (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(auditoriaService.buscar(usuarioId, entidad, accion, texto, desde, hasta, page, size));
    }

    @GetMapping("/filtros")
    @PreAuthorize(Permisos.SOLO_ADMIN)
    @Operation(summary = "Valores para los filtros", description = "Entidades y acciones que existen en el historial.")
    public ResponseEntity<AuditoriaFiltrosDTO> filtros() {
        return ResponseEntity.ok(auditoriaService.filtrosDisponibles());
    }
}
