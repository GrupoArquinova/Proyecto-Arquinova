package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ZonaComunRequestDTO;
import com.constructora_backend.dto.response.ZonaComunResponseDTO;
import com.constructora_backend.service.ZonaComunService;
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
@RequestMapping("/api/zonas-comunes")
@Tag(name = "Zonas Comunes", description = "Endpoints para la gestión de las zonas comunes de proyectos")
public class ZonaComunController {

    @Autowired
    private ZonaComunService zonaComunService;

    @GetMapping("/proyecto/{proyectoId}")
    @Operation(summary = "Listar por proyecto", description = "Obtiene todas las zonas comunes de un proyecto (incluyendo no publicadas/inactivas)")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ZonaComunResponseDTO>> listarPorProyectos(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(zonaComunService.listarPorProyecto(proyectoId));
    }

    @GetMapping("/proyecto/{proyectoId}/publicas")
    @Operation(summary = "Listar públicas por proyecto", description = "Obtiene solo las zonas comunes publicadas y activas de un proyecto")
    public ResponseEntity<List<ZonaComunResponseDTO>> listarPublicasPorProyecto(@PathVariable Long proyectoId) {
        return ResponseEntity.ok(zonaComunService.listarPublicadasYActivasPorProyectos(proyectoId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener zona común por ID", description = "Retorna los detalles de una zona común específica")
    public ResponseEntity<ZonaComunResponseDTO> obtenerPorId(@PathVariable Long id) {
        return zonaComunService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear zona común", description = "Crea una nueva zona común para un proyecto")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_ZONA_COMUN", entidad = "ZONAS_COMUNES", descripcion = "Creación de zona común")
    public ResponseEntity<ZonaComunResponseDTO> crear(@Valid @RequestBody ZonaComunRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zonaComunService.guardar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar zona común", description = "Actualiza los datos de una zona común")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ACTUALIZAR_ZONA_COMUN", entidad = "ZONAS_COMUNES", descripcion = "Actualización de zona común")
    public ResponseEntity<ZonaComunResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ZonaComunRequestDTO dto) {
        return ResponseEntity.ok(zonaComunService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar zona común", description = "Elimina de forma permanente una zona común")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_ZONA_COMUN", entidad = "ZONAS_COMUNES", descripcion = "Eliminación de zona común")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        zonaComunService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
