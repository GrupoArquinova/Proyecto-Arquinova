package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Respuesta detallada de un registro de auditoría")
public class AuditoriaResponseDTO {

    @Schema(description = "Identificador único de la auditoría", example = "1")
    private Long id;

    @Schema(description = "ID del usuario que realizó la acción", example = "1")
    private Long usuarioId;

    @Schema(description = "Nombre completo del usuario", example = "Administrador Arquinova")
    private String usuarioNombre;

    @Schema(description = "Acción ejecutada", example = "CREAR_USUARIO")
    private String accion;

    @Schema(description = "Entidad involucrada", example = "USUARIOS")
    private String entidad;

    @Schema(description = "ID de la entidad afectada", example = "10")
    private Long entidadId;

    @Schema(description = "Descripción del cambio o evento", example = "Creación de nuevo usuario registrado en el sistema")
    private String descripcion;

    @Schema(description = "Dirección IP origen", example = "127.0.0.1")
    private String ip;

    @Schema(description = "User Agent del cliente", example = "Mozilla/5.0...")
    private String userAgent;

    @Schema(description = "Fecha y hora del registro de auditoría")
    private LocalDateTime creadoEn;
}
