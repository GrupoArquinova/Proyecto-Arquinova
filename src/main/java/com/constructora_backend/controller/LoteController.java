package com.constructora_backend.controller;

import com.constructora_backend.dto.request.LoteRequestDTO;
import com.constructora_backend.dto.response.LoteResponseDTO;
import com.constructora_backend.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(summary = "Obtener lote por ID", description = "Retorna los detalles de un lote específico")
    public ResponseEntity<LoteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return loteService.obtenerPorId(id)
                .map(ResponseEntity::ok)
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
}
