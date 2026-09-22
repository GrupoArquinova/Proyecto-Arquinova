package com.constructora_backend.controller;

import com.constructora_backend.dto.request.CasaModeloRequestDTO;
import com.constructora_backend.dto.response.CasaModeloResponseDTO;
import com.constructora_backend.service.CasaModeloService;
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
@RequestMapping("/api/casas-modelo")
@Tag(name = "Casas Modelo", description = "Endpoints para la gestión de casas modelo en proyectos")
public class CasaModeloController {

    @Autowired
    private CasaModeloService casaModeloService;

    @GetMapping("/proyecto/{proyectoId}")
    @Operation(summary = "Listar por proyecto", description = "Obtiene todas las casas modelo de un proyecto")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<CasaModeloResponseDTO>> listarPorProyecto(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(casaModeloService.listarPorProyectos(proyectoId));
    }

    @GetMapping("/proyecto/{proyectoId}/publicas")
    @Operation(summary = "Listar públicas por proyecto", description = "Obtiene solo las casas modelo publicadas y activas de un proyecto")
    public ResponseEntity<List<CasaModeloResponseDTO>> listarPublicasPorProyecto(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(casaModeloService.listarPublicadasYActivasPorProyecto(proyectoId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener casa modelo por ID", description = "Retorna los detalles de una casa modelo")
    public ResponseEntity<CasaModeloResponseDTO> obtenerPorId(@PathVariable Long id) {
        return casaModeloService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear casa modelo", description = "Crea una nueva casa modelo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_CASA_MODELO", entidad = "CASAS_MODELO", descripcion = "Creación de casa modelo")
    public ResponseEntity<CasaModeloResponseDTO> crear(@Valid @RequestBody CasaModeloRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(casaModeloService.guardar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar casa modelo", description = "Actualiza los datos de una casa modelo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ACTUALIZAR_CASA_MODELO", entidad = "CASAS_MODELO", descripcion = "Actualización de casa modelo")
    public ResponseEntity<CasaModeloResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody CasaModeloRequestDTO dto) {
        return ResponseEntity.ok(casaModeloService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar casa modelo", description = "Elimina permanentemente una casa modelo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_CASA_MODELO", entidad = "CASAS_MODELO", descripcion = "Eliminación de casa modelo")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        casaModeloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
