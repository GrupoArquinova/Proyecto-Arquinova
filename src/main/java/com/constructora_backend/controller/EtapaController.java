package com.constructora_backend.controller;

import com.constructora_backend.dto.request.EtapaRequestDTO;
import com.constructora_backend.dto.response.EtapaResponseDTO;
import com.constructora_backend.service.EtapaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/etapas")
public class EtapaController {

    @Autowired
    private EtapaService etapaService;

    @GetMapping
    public ResponseEntity<List<EtapaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(etapaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EtapaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return etapaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/proyecto/{proyectoId}")
    public ResponseEntity<List<EtapaResponseDTO>> listarPorProyecto(
            @PathVariable Long proyectoId,
            @RequestParam(defaultValue = "false") boolean soloActivas) {
        List<EtapaResponseDTO> lista = soloActivas
                ? etapaService.listarActivasPorProyecto(proyectoId)
                : etapaService.listarPorProyecto(proyectoId);
                return ResponseEntity.ok(lista);
    }

    @PostMapping
    public ResponseEntity<EtapaResponseDTO> crear(@Valid @RequestBody EtapaRequestDTO dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(etapaService.guardar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EtapaResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody EtapaRequestDTO dto) {
        return ResponseEntity.ok(etapaService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        etapaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        etapaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
