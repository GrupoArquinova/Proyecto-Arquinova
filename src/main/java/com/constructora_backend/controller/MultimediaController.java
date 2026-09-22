package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.MultimediaRequestDTO;
import com.constructora_backend.dto.response.MultimediaResponseDTO;
import com.constructora_backend.service.MultimediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/multimedia")
@RequiredArgsConstructor
@Tag(name = "Multimedia", description = "Endpoints para la gestión y consulta de recursos multimedia (imágenes, videos, planos, etc.)")
public class MultimediaController {

    private final MultimediaService multimediaService;

    @PostMapping
    @Operation(summary = "Crear recurso multimedia", description = "Registra un nuevo archivo multimedia asociado a un proyecto, lote, zona común o casa modelo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_MULTIMEDIA", entidad = "MULTIMEDIA", descripcion = "Registro de nuevo recurso multimedia")
    public ResponseEntity<MultimediaResponseDTO> crear(
            @Valid @RequestBody MultimediaRequestDTO dto,
            @RequestAttribute(name = "usuarioId", required = false) Long usuarioId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(multimediaService.guardar(dto, usuarioId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener multimedia por ID", description = "Retorna los detalles y metadatos de un recurso multimedia específico")
    public ResponseEntity<MultimediaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(multimediaService.obtenerPorId(id));
    }

    @GetMapping("/{tipoEntidad}/{entidadId}")
    @Operation(summary = "Listar multimedia por entidad", description = "Obtiene los recursos multimedia asociados a una entidad (proyecto, lote, zonacomun, casamodelo)")
    public ResponseEntity<List<MultimediaResponseDTO>> listarPorEntidad(
            @PathVariable String tipoEntidad,
            @PathVariable Long entidadId,
            @RequestParam(defaultValue = "false") boolean soloPublicados) {
        return ResponseEntity.ok(multimediaService.listarPorEntidad(tipoEntidad, entidadId, soloPublicados));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar multimedia", description = "Actualiza la información y metadatos de un recurso multimedia existente")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ACTUALIZAR_MULTIMEDIA", entidad = "MULTIMEDIA", descripcion = "Actualización de recurso multimedia")
    public ResponseEntity<MultimediaResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody MultimediaRequestDTO dto) {
        return ResponseEntity.ok(multimediaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar multimedia", description = "Elimina un recurso multimedia del sistema")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_MULTIMEDIA", entidad = "MULTIMEDIA", descripcion = "Eliminación de recurso multimedia")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        multimediaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
