package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Respuesta detallada de la información de usuario")
public class UsuarioResponseDTO {

    @Schema(description = "Identificador único del usuario", example = "1")
    private Long id;

    @Schema(description = "Nombre completo", example = "Juan Pérez")
    private String nombreCompleto;

    @Schema(description = "Correo electrónico del usuario", example = "juan.perez@arquinova.com")
    private String correo;

    @Schema(description = "Identificador del rol asignado", example = "1")
    private Long rolId;

    @Schema(description = "Estado actual de la cuenta", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha y hora del último acceso exitoso")
    private LocalDateTime ultimoAccesoEn;

    @Schema(description = "Fecha y hora de creación de la cuenta")
    private LocalDateTime creadoEn;

    @Schema(description = "Fecha y hora de la última modificación de la cuenta")
    private LocalDateTime actualizadoEn;
}