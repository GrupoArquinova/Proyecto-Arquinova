package com.constructora_backend.controller;

import com.constructora_backend.dto.request.CasaModeloRequestDTO;
import com.constructora_backend.dto.response.CasaModeloResponseDTO;
import com.constructora_backend.service.CasaModeloService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/casas-modelo")
public class CasaModeloController {

    @Autowired
    private CasaModeloService casaModeloService;

    @GetMapping("/proyecto/{proyectoId}")
    public ResponseEntity<List<CasaModeloResponseDTO>> listarPorProyecto(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(casaModeloService.listarPorProyectos(proyectoId));
    }

    @GetMapping("/proyecto/{proyectoId}/publicas")
    public ResponseEntity<List<CasaModeloResponseDTO>> listarPublicasPorProyecto(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(casaModeloService.listarPublicadasYActivasPorProyecto(proyectoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CasaModeloResponseDTO> obtenerPorId(@PathVariable Long id) {
        return casaModeloService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CasaModeloResponseDTO> crear(@Valid @RequestBody CasaModeloRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(casaModeloService.guardar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CasaModeloResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody CasaModeloRequestDTO dto) {
        return ResponseEntity.ok(casaModeloService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        casaModeloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
