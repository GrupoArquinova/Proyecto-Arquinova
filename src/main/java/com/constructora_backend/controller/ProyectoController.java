package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.ProyectoRequestDTO;
import com.constructora_backend.dto.response.ProyectoResponseDTO;
import com.constructora_backend.service.ProyectoService;
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
@RequestMapping("/api/proyectos")
@Tag(name = "Proyectos", description = "Endpoints para la gestión y consulta de proyectos inmobiliarios")
@SecurityRequirement(name = "bearerAuth")
public class ProyectoController {

    private final ProyectoService proyectoService;

    @Autowired
    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    // ───── LISTAR TODOS / PUBLICADOS ─────

    @GetMapping
    @Operation(summary = "Listar proyectos",
               description = "Retorna el listado de proyectos registrados. Permite filtrar solo los publicados y activos mediante el parámetro 'soloPublicados'.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de proyectos recuperada exitosamente",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProyectoResponseDTO.class))))
    })
    public ResponseEntity<List<ProyectoResponseDTO>> listar(
            @Parameter(description = "Si es true, sólo retorna proyectos publicados y activos", example = "false")
            @RequestParam(defaultValue = "false") boolean soloPublicados) {
        List<ProyectoResponseDTO> lista = soloPublicados ? proyectoService.listarPublicados() : proyectoService.listarTodos();
        return ResponseEntity.ok(lista);
    }

    // ───── OBTENER POR ID ─────

    @GetMapping("/{id}")
    @Operation(summary = "Obtener proyecto por ID",
               description = "Retorna la información detallada de un proyecto específico según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Proyecto encontrado",
            content = @Content(schema = @Schema(implementation = ProyectoResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<ProyectoResponseDTO> obtenerPorId(
            @Parameter(description = "ID del proyecto", example = "1") @PathVariable Long id) {
        return proyectoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ───── LISTAR POR EMPRESA ─────

    @GetMapping("/empresa/{empresaId}")
    @Operation(summary = "Listar proyectos por empresa",
               description = "Retorna todos los proyectos pertenecientes a una empresa constructora específica.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de proyectos recuperada exitosamente",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProyectoResponseDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    public ResponseEntity<List<ProyectoResponseDTO>> listarPorEmpresa(
            @Parameter(description = "ID de la empresa", example = "1") @PathVariable Long empresaId) {
        return ResponseEntity.ok(proyectoService.listarPorEmpresa(empresaId));
    }

    // ───── OBTENER POR EMPRESA Y SLUG ─────

    @GetMapping("/empresa/{empresaId}/slug/{slug}")
    @Operation(summary = "Obtener proyecto por empresa y slug",
               description = "Permite consultar un proyecto específico mediante el ID de la empresa y el slug amigable de URL.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Proyecto encontrado",
            content = @Content(schema = @Schema(implementation = ProyectoResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Empresa o proyecto no encontrado")
    })
    public ResponseEntity<ProyectoResponseDTO> obtenerPorEmpresaYSlug(
            @Parameter(description = "ID de la empresa", example = "1") @PathVariable Long empresaId,
            @Parameter(description = "Slug único del proyecto", example = "condominio-campestre-los-alamos") @PathVariable String slug) {
        return proyectoService.obtenerPorEmpresaYSlug(empresaId, slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ───── CREAR ─────

    @PostMapping
    @Auditable(accion = "CREAR_PROYECTO", entidad = "PROYECTOS", descripcion = "Registro de nuevo proyecto inmobiliario")
    @Operation(summary = "Crear un nuevo proyecto",
               description = "Registra un nuevo proyecto inmobiliario en el sistema. El slug debe ser único por empresa.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Proyecto registrado exitosamente",
            content = @Content(schema = @Schema(implementation = ProyectoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Empresa o usuario no encontrado"),
        @ApiResponse(responseCode = "409", description = "Conflicto - Ya existe un proyecto con ese slug para la empresa"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<ProyectoResponseDTO> crear(@Valid @RequestBody ProyectoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proyectoService.guardar(dto));
    }

    // ───── ACTUALIZAR ─────

    @PutMapping("/{id}")
    @Auditable(accion = "ACTUALIZAR_PROYECTO", entidad = "PROYECTOS", descripcion = "Actualización de proyecto inmobiliario")
    @Operation(summary = "Actualizar proyecto existente",
               description = "Modifica los datos de un proyecto existente según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Proyecto actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = ProyectoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Proyecto, empresa o usuario no encontrado"),
        @ApiResponse(responseCode = "409", description = "Conflicto - El slug ya pertenece a otro proyecto de la empresa"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<ProyectoResponseDTO> actualizar(
            @Parameter(description = "ID del proyecto a actualizar", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProyectoRequestDTO dto) {
        return ResponseEntity.ok(proyectoService.actualizar(id, dto));
    }

    // ───── DESACTIVAR ─────

    @DeleteMapping("/{id}")
    @Auditable(accion = "DESACTIVAR_PROYECTO", entidad = "PROYECTOS", descripcion = "Desactivación lógica de proyecto")
    @Operation(summary = "Desactivar un proyecto",
               description = "Realiza la desactivación lógica (baja) de un proyecto inmobiliario sin eliminar su historial.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Proyecto desactivado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Proyecto no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "ID del proyecto a desactivar", example = "1") @PathVariable Long id) {
        proyectoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
