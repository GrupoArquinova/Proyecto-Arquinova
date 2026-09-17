package com.constructora_backend.controller;

import com.constructora_backend.dto.request.CambiarEstadoLoteDTO;
import com.constructora_backend.dto.response.HistorialEstadoLoteDTO;
import com.constructora_backend.entity.HistorialEstadoLote;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.service.impl.LoteServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lotes")
@RequiredArgsConstructor
public class HistorialEstadoLoteController {

    private final LoteServiceImpl loteService;

    @PutMapping("/{loteId}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Long loteId,
            @Valid @RequestBody CambiarEstadoLoteDTO dto,
            @RequestAttribute(name = "usuarioId", required = false) Long usuarioId){

        loteService.cambiarEstadoLote(loteId, dto, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{loteId}/historial")
    public ResponseEntity<List<HistorialEstadoLoteDTO>> obtenerHistorial(@PathVariable Long loteId) {
        return ResponseEntity.ok(loteService.consultarHistorial(loteId));
    }
}
