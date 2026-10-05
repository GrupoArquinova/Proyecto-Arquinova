package com.constructora_backend.controller;

import com.constructora_backend.dto.request.LoteRequestDTO;
import com.constructora_backend.dto.request.CambiarEstadoLoteDTO;
import com.constructora_backend.dto.response.HistorialEstadoLoteDTO;
import com.constructora_backend.dto.response.LoteResponseDTO;
import com.constructora_backend.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.constructora_backend.aspect.Auditable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@Tag(name = "Lotes", description = "Endpoints para la gestión de lotes en etapas de proyectos")
public class LoteController {

    @Autowired
    private LoteService loteService;

    @GetMapping
    @Operation(summary = "Listar todos los lotes (admin)", description = "Obtiene todos los lotes registrados, incluidos inactivos y no publicados. Requiere rol ADMINISTRADOR.")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<LoteResponseDTO>> obtenerLotes() {
        return ResponseEntity.ok(loteService.listarTodos());
    }

    @GetMapping("/publicos")
    @Operation(summary = "Listar lotes públicos", description = "Obtiene solo los lotes publicados y activos, para el sitio público")
    public ResponseEntity<List<LoteResponseDTO>> obtenerLotesPublicos() {
        return ResponseEntity.ok(loteService.listarPublicadosYActivos());
    }

    @GetMapping("/etapa/{etapaId}")
    @Operation(summary = "Listar lotes por etapa", description = "Obtiene todos los lotes asociados a una etapa (incluidos inactivos y no publicados)")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<LoteResponseDTO>> listarPorEtapa(@PathVariable Long etapaId) {
        return ResponseEntity.ok(loteService.listarPorEtapa(etapaId));
    }

    @GetMapping("/etapa/{etapaId}/publicos")
    @Operation(summary = "Listar lotes públicos por etapa", description = "Obtiene solo los lotes publicados y activos de una etapa")
    public ResponseEntity<List<LoteResponseDTO>> listarPublicosPorEtapa(@PathVariable Long etapaId) {
        return ResponseEntity.ok(loteService.listarPublicadosYActivosPorEtapa(etapaId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener lote por ID", description = "Retorna los detalles de un lote. Un ADMINISTRADOR ve cualquier lote; el público solo ve lotes publicados y activos (los demás responden 404).")
    public ResponseEntity<LoteResponseDTO> obtenerPorId(@PathVariable Long id, Authentication authentication) {
        boolean esAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
        java.util.Optional<LoteResponseDTO> lote = esAdmin
                ? loteService.obtenerPorId(id)
                : loteService.obtenerPublicoPorId(id);
        return lote.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear lote", description = "Crea un nuevo lote asociado a una etapa")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_LOTE", entidad = "LOTES", descripcion = "Creación de un nuevo lote")
    public ResponseEntity<LoteResponseDTO> crear(@Valid @RequestBody LoteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteService.guardar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar lote", description = "Actualiza los datos de un lote existente")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ACTUALIZAR_LOTE", entidad = "LOTES", descripcion = "Actualización de datos de lote")
    public ResponseEntity<LoteResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody LoteRequestDTO dto) {
        return ResponseEntity.ok(loteService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar lote", description = "Elimina permanentemente un lote del sistema")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_LOTE", entidad = "LOTES", descripcion = "Eliminación permanente de lote")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        loteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de lote",
            description = "Cambia el estado comercial del lote y lo registra en el historial. Cuerpo: {\"estadoId\": 2, \"observaciones\": \"opcional\"} (también acepta \"nuevoEstadoId\").")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CAMBIAR_ESTADO_LOTE", entidad = "LOTES", descripcion = "Cambio de estado de lote e inserción en historial")
    public ResponseEntity<LoteResponseDTO> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoLoteDTO dto) {
        return ResponseEntity.ok(loteService.cambiarEstado(id, dto.getNuevoEstadoId().intValue(), dto.getObservaciones()));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Consultar historial de estados", description = "Lista cronológica (más reciente primero) de los cambios de estado de un lote")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<HistorialEstadoLoteDTO>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(loteService.consultarHistorial(id));
    }

    @PatchMapping("/{id}/activo")
    @Operation(summary = "Activar o desactivar lote")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> toggleActivo(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> body) {
        Boolean activo = body.get("activo");
        loteService.toggleActivo(id, activo);
        return ResponseEntity.noContent().build();
    }

}
