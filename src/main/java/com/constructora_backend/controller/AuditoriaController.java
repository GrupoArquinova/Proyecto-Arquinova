package com.constructora_backend.controller;

import com.constructora_backend.dto.request.AuditoriaRequestDTO;
import com.constructora_backend.dto.response.AuditoriaResponseDTO;
import com.constructora_backend.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@Tag(name = "Auditoría", description = "Endpoints para la consulta y registro de eventos de auditoría del sistema")
@SecurityRequirement(name = "bearerAuth")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @Autowired
    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @Operation(summary = "Listar todas las auditorías", description = "Retorna el historial completo de eventos de auditoría registrados en el sistema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Historial de auditorías obtenido exitosamente",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuditoriaResponseDTO.class)))),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol de Administrador")
    })
    public ResponseEntity<List<AuditoriaResponseDTO>> listar() {
        return ResponseEntity.ok(auditoriaService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener auditoría por ID", description = "Retorna los detalles de un registro de auditoría específico.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Auditoría encontrada",
            content = @Content(schema = @Schema(implementation = AuditoriaResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Registro de auditoría no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<AuditoriaResponseDTO> obtenerPorId(
            @Parameter(description = "ID del registro de auditoría", example = "1") @PathVariable Long id) {
        return auditoriaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/entidad/{entidad}/{entidadId}")
    @Operation(summary = "Filtrar auditorías por entidad y ID", description = "Retorna los eventos de auditoría asociados a una entidad y registro específico.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Eventos de auditoría encontrados",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuditoriaResponseDTO.class)))),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<List<AuditoriaResponseDTO>> obtenerPorEntidad(
            @Parameter(description = "Nombre de la entidad (ej. USUARIOS, PROYECTOS)", example = "USUARIOS") @PathVariable String entidad,
            @Parameter(description = "ID de la entidad afectada", example = "10") @PathVariable Long entidadId) {
        return ResponseEntity.ok(auditoriaService.obtenerPorEntidad(entidad, entidadId));
    }

    @PostMapping
    @Operation(summary = "Registrar un evento de auditoría manual", description = "Registra un evento de auditoría personalizado en el sistema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Evento de auditoría registrado exitosamente",
            content = @Content(schema = @Schema(implementation = AuditoriaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<AuditoriaResponseDTO> registrar(@Valid @RequestBody AuditoriaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditoriaService.registrar(dto));
    }
}
