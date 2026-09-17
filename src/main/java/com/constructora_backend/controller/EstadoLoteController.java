package com.constructora_backend.controller;

import com.constructora_backend.dto.request.EstadoLoteRequestDTO;
import com.constructora_backend.dto.response.EstadoLoteResponseDTO;
import com.constructora_backend.service.EstadoLoteService;
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
@RequestMapping("/api/estados-lote")
@Tag(name = "Estados Lote", description = "Endpoints para la gestión de los estados que puede tener un lote")
public class EstadoLoteController {

    @Autowired
    private EstadoLoteService estadoLoteService;

    @GetMapping
    @Operation(summary = "Listar todos los estados", description = "Obtiene todos los estados (incluidos inactivos)")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<List<EstadoLoteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estadoLoteService.listarTodos());
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar estados activos", description = "Obtiene los estados que están actualmente activos")
    public ResponseEntity<List<EstadoLoteResponseDTO>> listarActivos() {
        return ResponseEntity.ok(estadoLoteService.listarActivos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener estado por ID", description = "Retorna los detalles de un estado")
    public ResponseEntity<EstadoLoteResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return estadoLoteService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear estado", description = "Crea un nuevo estado de lote")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_ESTADO_LOTE", entidad = "ESTADOS_LOTE", descripcion = "Creación de estado de lote")
    public ResponseEntity<EstadoLoteResponseDTO> crear(@Valid @RequestBody EstadoLoteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estadoLoteService.guardar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar estado", description = "Actualiza los datos de un estado existente")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Auditable(accion = "ACTUALIZAR_ESTADO_LOTE", entidad = "ESTADOS_LOTE", descripcion = "Actualización de estado de lote")
    public ResponseEntity<EstadoLoteResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody EstadoLoteRequestDTO dto) {
        return ResponseEntity.ok(estadoLoteService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar estado", description = "Elimina permanentemente un estado de lote")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_ESTADO_LOTE", entidad = "ESTADOS_LOTE", descripcion = "Eliminación de estado de lote")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        estadoLoteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
