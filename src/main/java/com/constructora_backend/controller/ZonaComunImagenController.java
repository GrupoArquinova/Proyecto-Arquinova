package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ZonaComunImagenRequestDTO;
import com.constructora_backend.dto.response.ZonaComunImagenResponseDTO;
import com.constructora_backend.service.ZonaComunImagenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zonas-comunes-imagenes")
public class ZonaComunImagenController {

    @Autowired
    private ZonaComunImagenService imagenService;

    @GetMapping("/zona-comun/{zonaComunId}")
    public ResponseEntity<List<ZonaComunImagenResponseDTO>> listarPorZonaComun(@PathVariable Long zonaComunId) {
        return ResponseEntity.ok(imagenService.listarPorZonaComun(zonaComunId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZonaComunImagenResponseDTO> obtenerPorId(@PathVariable Long id) {
        return imagenService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ZonaComunImagenResponseDTO> crear(@Valid @RequestBody ZonaComunImagenRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imagenService.guardar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZonaComunImagenResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ZonaComunImagenRequestDTO dto) {
        return ResponseEntity.ok(imagenService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/principal")
    public ResponseEntity<Void> marcarComoPrincipal(@PathVariable Long id) {
        imagenService.marcarComoPrincipal(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        imagenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
