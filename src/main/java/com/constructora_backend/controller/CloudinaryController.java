package com.constructora_backend.controller;

import com.constructora_backend.dto.response.CloudinaryFirmaResponseDTO;
import com.constructora_backend.service.CloudinaryFirmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cloudinary")
@Tag(name = "Cloudinary", description = "Firma de subidas directas de archivos a Cloudinary")
@SecurityRequirement(name = "bearerAuth")
public class CloudinaryController {

    private final CloudinaryFirmaService cloudinaryFirmaService;

    public CloudinaryController(CloudinaryFirmaService cloudinaryFirmaService) {
        this.cloudinaryFirmaService = cloudinaryFirmaService;
    }

    @PostMapping("/firma")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Obtener firma de subida",
            description = "Genera una firma de corta duración para que el panel de administración suba un archivo directamente a Cloudinary. El API secret nunca se expone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Firma generada",
                    content = @Content(schema = @Schema(implementation = CloudinaryFirmaResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Prohibido - Requiere rol ADMINISTRADOR"),
            @ApiResponse(responseCode = "503", description = "Cloudinary no está configurado en el servidor")
    })
    public ResponseEntity<CloudinaryFirmaResponseDTO> generarFirma() {
        if (!cloudinaryFirmaService.estaConfigurado()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(cloudinaryFirmaService.generarFirma());
    }
}
