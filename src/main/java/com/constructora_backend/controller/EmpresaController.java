package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.EmpresaRequestDTO;
import com.constructora_backend.dto.response.EmpresaResponseDTO;
import com.constructora_backend.service.EmpresaService;
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
@RequestMapping("/api/empresas")
@Tag(name = "Empresas", description = "Endpoints para la gestión e información pública/privada de empresas constructoras")
@SecurityRequirement(name = "bearerAuth")
public class EmpresaController {

    private final EmpresaService empresaService;

    @Autowired
    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    @Operation(summary = "Listar empresas", description = "Retorna el catálogo de empresas registradas. Permite filtrar por solo activas mediante query parameter.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista recuperada exitosamente",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EmpresaResponseDTO.class))))
    })
    public ResponseEntity<List<EmpresaResponseDTO>> listar(
            @Parameter(description = "Si es true, sólo retorna empresas activas", example = "false")
            @RequestParam(defaultValue = "false") boolean soloActivas) {
        List<EmpresaResponseDTO> lista = soloActivas ? empresaService.listarActivas() : empresaService.listarTodas();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener empresa por ID", description = "Retorna la información detallada de una empresa específica según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Empresa encontrada",
            content = @Content(schema = @Schema(implementation = EmpresaResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    public ResponseEntity<EmpresaResponseDTO> obtenerPorId(
            @Parameter(description = "ID de la empresa a consultar", example = "1") @PathVariable Long id) {
        return empresaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Auditable(accion = "CREAR_EMPRESA", entidad = "EMPRESAS", descripcion = "Registro de nueva empresa constructora")
    @Operation(summary = "Crear una nueva empresa", description = "Registra una nueva empresa constructora en el sistema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Empresa registrada exitosamente",
            content = @Content(schema = @Schema(implementation = EmpresaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Conflicto - NIT ya registrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<EmpresaResponseDTO> crear(@Valid @RequestBody EmpresaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.guardar(dto));
    }

    @PutMapping("/{id}")
    @Auditable(accion = "ACTUALIZAR_EMPRESA", entidad = "EMPRESAS", descripcion = "Actualización de datos institucionales de la empresa")
    @Operation(summary = "Actualizar empresa existente", description = "Modifica los datos institucionales o comerciales de una empresa según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Empresa actualizada exitosamente",
            content = @Content(schema = @Schema(implementation = EmpresaResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada"),
        @ApiResponse(responseCode = "409", description = "Conflicto - NIT pertenece a otra empresa"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<EmpresaResponseDTO> actualizar(
            @Parameter(description = "ID de la empresa a actualizar", example = "1") @PathVariable Long id,
            @Valid @RequestBody EmpresaRequestDTO dto) {
        return ResponseEntity.ok(empresaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Auditable(accion = "DESACTIVAR_EMPRESA", entidad = "EMPRESAS", descripcion = "Desactivación lógica de empresa")
    @Operation(summary = "Desactivar una empresa", description = "Cambia el estado de la empresa a inactivo sin eliminar los registros históricos.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Empresa desactivada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido")
    })
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "ID de la empresa a desactivar", example = "1") @PathVariable Long id) {
        empresaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
