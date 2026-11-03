package com.constructora_backend.controller;

import com.constructora_backend.dto.response.RolDTO;
import com.constructora_backend.repository.RolRepository;
import com.constructora_backend.security.Permisos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Roles que el panel de usuarios ofrece al crear o editar una cuenta (ADMINISTRADOR, COMERCIAL, EDITOR). Así el panel no
 * depende de los números de rol: usa los que devuelve este listado.
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Roles asignables a los usuarios del panel")
@SecurityRequirement(name = "bearerAuth")
public class RolController {

    private final RolRepository rolRepository;

    @Autowired
    public RolController(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @GetMapping
    @PreAuthorize(Permisos.SOLO_ADMIN)
    @Operation(summary = "Listar roles asignables", description = "Roles activos, ordenados por ID. Solo administradores.")
    public ResponseEntity<List<RolDTO>> listar() {
        List<RolDTO> roles = rolRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()))
                .sorted(java.util.Comparator.comparing(com.constructora_backend.entity.Rol::getId))
                .map(r -> {
                    RolDTO dto = new RolDTO();
                    dto.setId(r.getId());
                    dto.setNombre(RolDTO.nombreSinPrefijo(r.getNombre()));
                    dto.setDescripcion(r.getDescripcion());
                    dto.setActivo(r.getActivo());
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(roles);
    }
}
