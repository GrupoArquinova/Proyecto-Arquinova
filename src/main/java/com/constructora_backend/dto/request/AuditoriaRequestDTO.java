package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para el registro manual de eventos de auditoría")
public class AuditoriaRequestDTO {

    @Schema(description = "ID del usuario que realizó la acción (opcional si es sistema)", example = "1")
    private Long usuarioId;

    @NotBlank(message = "La accion es obligatoria")
    @Size(max = 50, message = "La accion no puede superar los 50 caracteres")
    @Schema(description = "Acción realizada en el sistema", example = "ACTUALIZAR_USUARIO")
    private String accion;

    @NotBlank(message = "La entidad es obligatoria")
    @Size(max = 80, message = "La entidad no puede superar los 80 caracteres")
    @Schema(description = "Nombre de la entidad modificada", example = "USUARIOS")
    private String entidad;

    @Schema(description = "ID del registro afectado en la entidad", example = "10")
    private Long entidadId;

    @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
    @Schema(description = "Descripción detallada del evento", example = "Se actualizó el correo del usuario Juan Pérez")
    private String descripcion;

    @Size(max = 45, message = "La IP no puede superar los 45 caracteres")
    @Schema(description = "Dirección IP origen del cliente", example = "192.168.1.50")
    private String ip;

    @Size(max = 500, message = "El User-Agent no puede superar los 500 caracteres")
    @Schema(description = "Navegador o cliente desde donde se realizó la petición", example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    private String userAgent;
}
