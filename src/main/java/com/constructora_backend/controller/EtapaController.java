package com.constructora_backend.controller;

import com.constructora_backend.dto.request.EtapaRequestDTO;
import com.constructora_backend.dto.response.EtapaResponseDTO;
import com.constructora_backend.service.EtapaService;
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
@RequestMapping("/api/etapas")
@Tag(name = "Etapas", description = "Endpoints para la gestión de etapas de proyectos")
public class EtapaController {

    @Autowired
    private EtapaService etapaService;

    @GetMapping
    @Operation(summary = "Listar todas las etapas", description = "Obtiene una lista de todas las etapas")
    public ResponseEntity<List<EtapaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(etapaService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener etapa por ID", description = "Obtiene los detalles de una etapa específica")
    public ResponseEntity<EtapaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return etapaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/proyecto/{proyectoId}")
    @Operation(summary = "Listar etapas por proyecto", description = "Obtiene las etapas asociadas a un proyecto")
    public ResponseEntity<List<EtapaResponseDTO>> listarPorProyecto(
            @PathVariable Long proyectoId,
            @RequestParam(defaultValue = "false") boolean soloActivas) {
        List<EtapaResponseDTO> lista = soloActivas
                ? etapaService.listarActivasPorProyecto(proyectoId)
                : etapaService.listarPorProyecto(proyectoId);
                return ResponseEntity.ok(lista);
    }

    @PostMapping
    @Operation(summary = "Crear etapa", description = "Crea una nueva etapa en un proyecto")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_ETAPA", entidad = "ETAPAS", descripcion = "Creación de nueva etapa")
    public ResponseEntity<EtapaResponseDTO> crear(@Valid @RequestBody EtapaRequestDTO dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(etapaService.guardar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar etapa", description = "Actualiza los datos de una etapa existente")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ACTUALIZAR_ETAPA", entidad = "ETAPAS", descripcion = "Actualización de etapa")
    public ResponseEntity<EtapaResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody EtapaRequestDTO dto) {
        return ResponseEntity.ok(etapaService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar etapa", description = "Realiza un borrado lógico de una etapa")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "DESACTIVAR_ETAPA", entidad = "ETAPAS", descripcion = "Desactivación de etapa")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        etapaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar etapa", description = "Elimina físicamente una etapa del sistema")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_ETAPA", entidad = "ETAPAS", descripcion = "Eliminación permanente de etapa")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        etapaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
