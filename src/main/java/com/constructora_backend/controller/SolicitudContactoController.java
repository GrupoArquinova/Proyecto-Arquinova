package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.SolicitudContactoAtencionDTO;
import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.service.SolicitudContactoService;
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
@RequestMapping("/api/solicitudes-contacto")
@Tag(name = "Solicitudes de Contacto", description = "Endpoints para la recepción, atención y administración de solicitudes de contacto")
public class SolicitudContactoController {

    @Autowired
    private SolicitudContactoService solicitudService;

    @GetMapping
    @Operation(summary = "Listar todas las solicitudes", description = "Obtiene la lista completa de solicitudes de contacto registradas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<SolicitudContactoResponseDTO>> listarTodas(){
        return ResponseEntity.ok(solicitudService.listarTodas());
    }

    @GetMapping("/estado/{estadoId}")
    @Operation(summary = "Listar solicitudes por estado", description = "Filtra las solicitudes de contacto según su estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<SolicitudContactoResponseDTO>> listarPorEstado(@PathVariable Integer estadoId){
        return ResponseEntity.ok(solicitudService.listarPorEstado(estadoId));
    }

    @GetMapping("/proyecto/{proyectoId}")
    @Operation(summary = "Listar solicitudes por proyecto", description = "Filtra las solicitudes de contacto asociadas a un proyecto específico")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<SolicitudContactoResponseDTO>> listarPorProyecto(@PathVariable Long proyectoId){
        return ResponseEntity.ok(solicitudService.listarPorProyecto(proyectoId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener solicitud por ID", description = "Retorna la información detallada de una solicitud de contacto")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SolicitudContactoResponseDTO> obtenerPorId(@PathVariable Long id){
        return solicitudService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/publico")
    @Operation(summary = "Crear solicitud pública", description = "Endpoint público para que visitantes envíen una solicitud de información")
    public ResponseEntity<SolicitudContactoResponseDTO> crearPublica(@Valid @RequestBody SolicitudContactoPublicDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crearPublica(dto));
    }

    @PatchMapping("/{id}/atender")
    @Operation(summary = "Atender solicitud", description = "Actualiza el estado y observaciones internas tras contactar al cliente")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ATENDER_SOLICITUD_CONTACTO", entidad = "SOLICITUDES_CONTACTO", descripcion = "Atención y actualización de estado de solicitud de contacto")
    public ResponseEntity<SolicitudContactoResponseDTO> atenderSolicitud(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudContactoAtencionDTO dto){
        return ResponseEntity.ok(solicitudService.atenderSolicitud(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar solicitud", description = "Elimina de forma permanente una solicitud de contacto")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_SOLICITUD_CONTACTO", entidad = "SOLICITUDES_CONTACTO", descripcion = "Eliminación de solicitud de contacto")
    public ResponseEntity<Void> eliminar(@PathVariable Long id){
        solicitudService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
