package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.EstadoSolicitudRequestDTO;
import com.constructora_backend.dto.response.EstadoSolicitudResponseDTO;
import com.constructora_backend.service.EstadoSolicitudService;
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
@RequestMapping("/api/estados-solicitud")
@Tag(name = "Estados de Solicitud", description = "Endpoints para la gestión de los estados de solicitudes de contacto")
public class EstadoSolicitudController {

    @Autowired
    private EstadoSolicitudService estadoSolicitudService;

    @GetMapping
    @Operation(summary = "Listar todos los estados", description = "Obtiene todos los estados de solicitud (incluyendo inactivos)")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<EstadoSolicitudResponseDTO>> listarTodos(){
        return ResponseEntity.ok(estadoSolicitudService.listarTodos());
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar estados activos", description = "Obtiene solo los estados de solicitud que están activos")
    public ResponseEntity<List<EstadoSolicitudResponseDTO>> listarActivos(){
        return ResponseEntity.ok(estadoSolicitudService.listarActivos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener estado por ID", description = "Retorna los detalles de un estado de solicitud específico")
    public ResponseEntity<EstadoSolicitudResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return estadoSolicitudService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear estado de solicitud", description = "Crea un nuevo estado para las solicitudes de contacto")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_ESTADO_SOLICITUD", entidad = "ESTADOS_SOLICITUD", descripcion = "Creación de estado de solicitud")
    public ResponseEntity<EstadoSolicitudResponseDTO> crear(@Valid @RequestBody EstadoSolicitudRequestDTO dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(estadoSolicitudService.guardar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar estado de solicitud", description = "Actualiza los datos de un estado de solicitud existente")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ACTUALIZAR_ESTADO_SOLICITUD", entidad = "ESTADOS_SOLICITUD", descripcion = "Actualización de estado de solicitud")
    public ResponseEntity<EstadoSolicitudResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody EstadoSolicitudRequestDTO dto){
        return ResponseEntity.ok(estadoSolicitudService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar estado de solicitud", description = "Elimina de forma permanente un estado de solicitud")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_ESTADO_SOLICITUD", entidad = "ESTADOS_SOLICITUD", descripcion = "Eliminación de estado de solicitud")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        estadoSolicitudService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
