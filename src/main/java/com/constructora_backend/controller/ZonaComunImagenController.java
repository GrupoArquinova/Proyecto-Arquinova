package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.ZonaComunImagenRequestDTO;
import com.constructora_backend.dto.response.ZonaComunImagenResponseDTO;
import com.constructora_backend.service.ZonaComunImagenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zonas-comunes-imagenes")
@Tag(name = "Zonas Comunes - Imágenes", description = "Endpoints para la gestión de imágenes asociadas a zonas comunes")
public class ZonaComunImagenController {

    @Autowired
    private ZonaComunImagenService imagenService;

    @Operation(summary = "Listar imágenes por zona común", description = "Obtiene todas las imágenes asociadas a una zona común ordenadas por orden")
    @GetMapping("/zona-comun/{zonaComunId}")
    public ResponseEntity<List<ZonaComunImagenResponseDTO>> listarPorZonaComun(@PathVariable Long zonaComunId) {
        return ResponseEntity.ok(imagenService.listarPorZonaComun(zonaComunId));
    }

    @Operation(summary = "Obtener imagen por ID", description = "Obtiene los detalles de una imagen por su ID único")
    @GetMapping("/{id}")
    public ResponseEntity<ZonaComunImagenResponseDTO> obtenerPorId(@PathVariable Long id) {
        return imagenService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear nueva imagen para zona común", description = "Registra una nueva imagen asociada a una zona común")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_ZONA_COMUN_IMAGEN", entidad = "ZonaComunImagen", descripcion = "Creación de imagen para zona común")
    public ResponseEntity<ZonaComunImagenResponseDTO> crear(@Valid @RequestBody ZonaComunImagenRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imagenService.guardar(dto));
    }

    @Operation(summary = "Actualizar imagen de zona común", description = "Actualiza los datos de una imagen existente")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ACTUALIZAR_ZONA_COMUN_IMAGEN", entidad = "ZonaComunImagen", descripcion = "Actualización de imagen de zona común")
    public ResponseEntity<ZonaComunImagenResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ZonaComunImagenRequestDTO dto) {
        return ResponseEntity.ok(imagenService.actualizar(id, dto));
    }

    @Operation(summary = "Marcar imagen como principal", description = "Establece una imagen como la principal de la zona común")
    @PatchMapping("/{id}/principal")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "MARCAR_PRINCIPAL_ZONA_COMUN_IMAGEN", entidad = "ZonaComunImagen", descripcion = "Marcado de imagen como principal de zona común")
    public ResponseEntity<Void> marcarComoPrincipal(@PathVariable Long id) {
        imagenService.marcarComoPrincipal(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar imagen de zona común", description = "Elimina permanentemente una imagen asociada a una zona común")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_ZONA_COMUN_IMAGEN", entidad = "ZonaComunImagen", descripcion = "Eliminación de imagen de zona común")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        imagenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
