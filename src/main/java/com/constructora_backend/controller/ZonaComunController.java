package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ZonaComunRequestDTO;
import com.constructora_backend.dto.response.ZonaComunResponseDTO;
import com.constructora_backend.service.ZonaComunService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zonas-comunes")
public class ZonaComunController {

    @Autowired
    private ZonaComunService zonaComunService;

    @GetMapping("/proyecto/{proyectoId}")
    public ResponseEntity<List<ZonaComunResponseDTO>> listarPorProyectos(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(zonaComunService.listarPorProyecto(proyectoId));
    }

    @GetMapping("/proyecto/{proyectoId}/publicas")
    public ResponseEntity<List<ZonaComunResponseDTO>> listarPublicasPorProyecto(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(zonaComunService.listarPublicadasYActivasPorProyectos(proyectoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZonaComunResponseDTO> obtenerPorId(@PathVariable Long id) {
        return zonaComunService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ZonaComunResponseDTO> crear(@Valid @RequestBody ZonaComunRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zonaComunService.guardar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZonaComunResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ZonaComunRequestDTO dto) {
        return ResponseEntity.ok(zonaComunService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        zonaComunService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
