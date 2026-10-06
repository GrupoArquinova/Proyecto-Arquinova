package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.Punto360RequestDTO;
import com.constructora_backend.dto.response.Punto360ResponseDTO;
import com.constructora_backend.enums.EscenaPunto360;
import com.constructora_backend.service.Punto360Service;
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
@RequestMapping("/api/puntos-360")
@Tag(name = "Puntos 360", description = "Botones sobre la imagen 360° del entorno, la vista aérea y el plano de urbanismo")
public class Punto360Controller {

    @Autowired
    private Punto360Service punto360Service;

    @GetMapping("/proyecto/{proyectoId}")
    @Operation(summary = "Listar puntos de un proyecto",
            description = "Público. Opcionalmente filtra por escena (ENTORNO, AEREA o URBANISMO)")
    public ResponseEntity<List<Punto360ResponseDTO>> listarPorProyecto(
            @PathVariable Long proyectoId,
            @RequestParam(required = false) EscenaPunto360 escena) {
        return ResponseEntity.ok(punto360Service.listarPorProyecto(proyectoId, escena));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear punto")
    @Auditable(accion = "CREAR_PUNTO_360", entidad = "PUNTOS_360", descripcion = "Creación de punto sobre imagen 360°")
    public ResponseEntity<Punto360ResponseDTO> crear(@Valid @RequestBody Punto360RequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(punto360Service.guardar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar punto")
    @Auditable(accion = "ACTUALIZAR_PUNTO_360", entidad = "PUNTOS_360", descripcion = "Actualización de punto sobre imagen 360°")
    public ResponseEntity<Punto360ResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody Punto360RequestDTO dto) {
        return ResponseEntity.ok(punto360Service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Eliminar punto")
    @Auditable(accion = "ELIMINAR_PUNTO_360", entidad = "PUNTOS_360", descripcion = "Eliminación de punto sobre imagen 360°")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        punto360Service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
