package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.ContenidoInstitucionalRequestDTO;
import com.constructora_backend.dto.response.ContenidoInstitucionalResponseDTO;
import com.constructora_backend.service.ContenidoInstitucionalService;
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
@RequestMapping("/api/contenidos-institucionales")
@Tag(name = "Contenidos Institucionales", description = "Endpoints para la gestión de contenidos institucionales de las empresas (Misión, Visión, Valores, etc.)")
@SecurityRequirement(name = "bearerAuth")
public class ContenidoInstitucionalController {

    private final ContenidoInstitucionalService contenidoService;

    @Autowired
    public ContenidoInstitucionalController(ContenidoInstitucionalService contenidoService) {
        this.contenidoService = contenidoService;
    }

    // ───── LISTAR TODOS ─────

    @GetMapping
    @Operation(summary = "Listar todos los contenidos institucionales",
               description = "Retorna todos los contenidos institucionales registrados en el sistema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista recuperada exitosamente",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContenidoInstitucionalResponseDTO.class))))
    })
    public ResponseEntity<List<ContenidoInstitucionalResponseDTO>> listarTodos() {
        return ResponseEntity.ok(contenidoService.listarTodos());
    }

    // ───── LISTAR POR EMPRESA ─────

    @GetMapping("/empresa/{empresaId}")
    @Operation(summary = "Listar contenidos por empresa",
               description = "Retorna todos los contenidos institucionales de una empresa específica. " +
                             "Con el parámetro soloPublicados se pueden filtrar los publicados para la vista pública.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista recuperada exitosamente",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContenidoInstitucionalResponseDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    public ResponseEntity<List<ContenidoInstitucionalResponseDTO>> listarPorEmpresa(
            @Parameter(description = "ID de la empresa", example = "1") @PathVariable Long empresaId,
            @Parameter(description = "Si es true, sólo retorna contenidos publicados", example = "false")
            @RequestParam(defaultValue = "false") boolean soloPublicados) {
        List<ContenidoInstitucionalResponseDTO> lista = soloPublicados
                ? contenidoService.listarPublicadosPorEmpresa(empresaId)
                : contenidoService.listarPorEmpresa(empresaId);
        return ResponseEntity.ok(lista);
    }

    // ───── OBTENER POR ID ─────

    @GetMapping("/{id}")
    @Operation(summary = "Obtener contenido institucional por ID",
               description = "Retorna un contenido institucional específico según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contenido encontrado",
            content = @Content(schema = @Schema(implementation = ContenidoInstitucionalResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Contenido no encontrado")
    })
    public ResponseEntity<ContenidoInstitucionalResponseDTO> obtenerPorId(
            @Parameter(description = "ID del contenido institucional", example = "1") @PathVariable Long id) {
        return contenidoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ───── OBTENER POR EMPRESA Y SECCIÓN ─────

    @GetMapping("/empresa/{empresaId}/seccion/{seccion}")
    @Operation(summary = "Obtener contenido por empresa y sección",
               description = "Busca el contenido institucional de una empresa para una sección específica (ej: MISION, VISION, VALORES).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contenido encontrado",
            content = @Content(schema = @Schema(implementation = ContenidoInstitucionalResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Contenido o empresa no encontrado")
    })
    public ResponseEntity<ContenidoInstitucionalResponseDTO> obtenerPorEmpresaYSeccion(
            @Parameter(description = "ID de la empresa", example = "1") @PathVariable Long empresaId,
            @Parameter(description = "Nombre de la sección institucional", example = "MISION") @PathVariable String seccion) {
        return contenidoService.obtenerPorEmpresaYSeccion(empresaId, seccion)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ───── CREAR ─────

    @PostMapping
    @Auditable(accion = "CREAR_CONTENIDO_INSTITUCIONAL", entidad = "CONTENIDOS_INSTITUCIONALES",
               descripcion = "Registro de nuevo contenido institucional de empresa")
    @Operation(summary = "Crear contenido institucional",
               description = "Registra un nuevo contenido institucional para una empresa. La combinación empresa + sección debe ser única.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Contenido registrado exitosamente",
            content = @Content(schema = @Schema(implementation = ContenidoInstitucionalResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Empresa o usuario no encontrado"),
        @ApiResponse(responseCode = "409", description = "Conflicto - Ya existe contenido para esa empresa y sección"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<ContenidoInstitucionalResponseDTO> crear(
            @Valid @RequestBody ContenidoInstitucionalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contenidoService.guardar(dto));
    }

    // ───── ACTUALIZAR ─────

    @PutMapping("/{id}")
    @Auditable(accion = "ACTUALIZAR_CONTENIDO_INSTITUCIONAL", entidad = "CONTENIDOS_INSTITUCIONALES",
               descripcion = "Actualización de contenido institucional de empresa")
    @Operation(summary = "Actualizar contenido institucional",
               description = "Modifica un contenido institucional existente según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contenido actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = ContenidoInstitucionalResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Contenido, empresa o usuario no encontrado"),
        @ApiResponse(responseCode = "409", description = "Conflicto - Ya existe contenido para esa empresa y sección"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<ContenidoInstitucionalResponseDTO> actualizar(
            @Parameter(description = "ID del contenido a actualizar", example = "1") @PathVariable Long id,
            @Valid @RequestBody ContenidoInstitucionalRequestDTO dto) {
        return ResponseEntity.ok(contenidoService.actualizar(id, dto));
    }

    // ───── ELIMINAR ─────

    @DeleteMapping("/{id}")
    @Auditable(accion = "ELIMINAR_CONTENIDO_INSTITUCIONAL", entidad = "CONTENIDOS_INSTITUCIONALES",
               descripcion = "Eliminación de contenido institucional de empresa")
    @Operation(summary = "Eliminar contenido institucional",
               description = "Elimina permanentemente un contenido institucional según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Contenido eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Contenido no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del contenido a eliminar", example = "1") @PathVariable Long id) {
        contenidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
