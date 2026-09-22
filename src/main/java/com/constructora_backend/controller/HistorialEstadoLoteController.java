package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.CambiarEstadoLoteDTO;
import com.constructora_backend.dto.response.HistorialEstadoLoteDTO;
import com.constructora_backend.service.impl.LoteServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lotes")
@RequiredArgsConstructor
@Tag(name = "Lotes - Historial de Estados", description = "Endpoints para la gestión del cambio de estado y consulta de la trazabilidad histórica de lotes")
public class HistorialEstadoLoteController {

    private final LoteServiceImpl loteService;

    @PutMapping("/{loteId}/estado")
    @Operation(summary = "Cambiar estado de un lote", description = "Actualiza el estado actual del lote y registra una entrada en el historial de cambios")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CAMBIAR_ESTADO_LOTE", entidad = "LOTES", descripcion = "Cambio de estado de lote e inserción en historial")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Long loteId,
            @Valid @RequestBody CambiarEstadoLoteDTO dto,
            @RequestAttribute(name = "usuarioId", required = false) Long usuarioId){

        loteService.cambiarEstadoLote(loteId, dto, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{loteId}/historial")
    @Operation(summary = "Consultar historial de estados", description = "Obtiene la lista cronológica de todos los cambios de estado sufridos por un lote")
    public ResponseEntity<List<HistorialEstadoLoteDTO>> obtenerHistorial(@PathVariable Long loteId) {
        return ResponseEntity.ok(loteService.consultarHistorial(loteId));
    }
}
