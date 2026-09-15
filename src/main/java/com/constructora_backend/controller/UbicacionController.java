package com.constructora_backend.controller;

import com.constructora_backend.dto.request.UbicacionRequestDTO;
import com.constructora_backend.dto.response.UbicacionResponseDTO;
import com.constructora_backend.service.UbicacionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

    @Autowired
    private UbicacionService ubicacionService;

    @GetMapping
    public ResponseEntity<List<UbicacionResponseDTO>> listarTodas() {
        return ResponseEntity.ok(ubicacionService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UbicacionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ubicacionService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/proyecto/{proyectoId}")
    public ResponseEntity<UbicacionResponseDTO> obtenerPorProyectoId(@PathVariable Long proyectoId) {
        return ubicacionService.obtenerPorProyectoId(proyectoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ciudad/{ciudad}")
    public ResponseEntity<List<UbicacionResponseDTO>> listarPorCiudad(@PathVariable String ciudad) {
        return ResponseEntity.ok(ubicacionService.listarPorCiudad(ciudad));
    }

    @PostMapping
    public ResponseEntity<UbicacionResponseDTO> crear(@Valid @RequestBody UbicacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ubicacionService.guardar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UbicacionResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody UbicacionRequestDTO dto) {
        return ResponseEntity.ok(ubicacionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ubicacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
