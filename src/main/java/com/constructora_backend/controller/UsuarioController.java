package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.request.UsuarioRequestDTO;
import com.constructora_backend.dto.request.UsuarioUpdateDTO;
import com.constructora_backend.dto.response.UsuarioResponseDTO;
import com.constructora_backend.service.UsuarioService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Endpoints para la gestión e interacción con el módulo de Usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar todos los usuarios", description = "Retorna una lista completa de los usuarios registrados en el sistema.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista recuperada exitosamente",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioResponseDTO.class)))),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol de Administrador")
    })
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @userSecurity.isOwnerOrAdmin(#id)")
    @Operation(summary = "Obtener un usuario por ID", description = "Retorna los detalles de un usuario específico según su ID. Protegido por Row Level Security (RLS).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario encontrado",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo el propio usuario o un Administrador pueden consultar este perfil")
    })
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(
            @Parameter(description = "ID del usuario a buscar", example = "1") @PathVariable Long id) {
        return usuarioService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "CREAR_USUARIO", entidad = "USUARIOS", descripcion = "Creación de nuevo usuario en el sistema")
    @Operation(summary = "Crear un nuevo usuario", description = "Registra un usuario nuevo en el sistema y encripta su contraseña.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida / Datos requeridos faltantes"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol de Administrador")
    })
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioRequestDTO usuarioDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.guardar(usuarioDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @userSecurity.isOwnerOrAdmin(#id)")
    @Auditable(accion = "ACTUALIZAR_USUARIO", entidad = "USUARIOS", descripcion = "Actualización de información de usuario")
    @Operation(summary = "Actualizar usuario existente", description = "Actualiza los datos de un usuario existente según su ID. Protegido por Row Level Security (RLS).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo el propio usuario o un Administrador pueden modificar este perfil")
    })
    public ResponseEntity<UsuarioResponseDTO> actualizar(
            @Parameter(description = "ID del usuario a actualizar", example = "1") @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateDTO usuarioDTO) {
        return ResponseEntity.ok(usuarioService.actualizar(id, usuarioDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "ELIMINAR_USUARIO", entidad = "USUARIOS", descripcion = "Eliminación permanente de un usuario")
    @Operation(summary = "Eliminar un usuario", description = "Elimina de forma permanente un usuario del sistema según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol de Administrador")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del usuario a eliminar", example = "1") @PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
