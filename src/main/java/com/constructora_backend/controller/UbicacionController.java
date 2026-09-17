package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.UbicacionRequestDTO;
import com.constructora_backend.dto.response.UbicacionResponseDTO;
import com.constructora_backend.service.UbicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/ubicaciones")
@Tag(name = "Ubicaciones", description = "Operaciones relacionadas con las ubicaciones de los proyectos")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    @GetMapping
    @Operation(summary = "Listar todas las ubicaciones", description = "Obtiene una lista de todas las ubicaciones registradas")
    public ResponseEntity<List<UbicacionResponseDTO>> listarTodas() {
        return ResponseEntity.ok(ubicacionService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ubicación por ID", description = "Obtiene los detalles de una ubicación específica")
    public ResponseEntity<UbicacionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ubicacionService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/proyecto/{proyectoId}")
    @Operation(summary = "Obtener ubicación por ID de proyecto", description = "Obtiene la ubicación asignada a un proyecto específico")
    public ResponseEntity<UbicacionResponseDTO> obtenerPorProyectoId(@PathVariable Long proyectoId) {
        return ubicacionService.obtenerPorProyectoId(proyectoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ciudad/{ciudad}")
    @Operation(summary = "Listar ubicaciones por ciudad", description = "Busca y retorna todas las ubicaciones en una ciudad dada")
    public ResponseEntity<List<UbicacionResponseDTO>> listarPorCiudad(@PathVariable String ciudad) {
        return ResponseEntity.ok(ubicacionService.listarPorCiudad(ciudad));
    }

    @PostMapping
    @Operation(summary = "Crear ubicación", description = "Registra una nueva ubicación para un proyecto")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_UBICACION", entidad = "UBICACIONES", descripcion = "Creación de ubicación")
    public ResponseEntity<UbicacionResponseDTO> crear(@Valid @RequestBody UbicacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ubicacionService.guardar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar ubicación", description = "Modifica los datos de una ubicación existente")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Auditable(accion = "ACTUALIZAR_UBICACION", entidad = "UBICACIONES", descripcion = "Actualización de ubicación")
    public ResponseEntity<UbicacionResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody UbicacionRequestDTO dto) {
        return ResponseEntity.ok(ubicacionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar ubicación", description = "Elimina una ubicación del sistema")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_UBICACION", entidad = "UBICACIONES", descripcion = "Eliminación de ubicación")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ubicacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
