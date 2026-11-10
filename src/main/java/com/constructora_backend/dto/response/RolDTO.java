package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Rol que se puede asignar a un usuario del panel")
public class RolDTO {

    @Schema(description = "Identificador del rol (el que se envía como rolId al crear o editar un usuario)", example = "2")
    private Long id;

    @Schema(description = "Nombre del rol, sin prefijo ROLE_", example = "COMERCIAL")
    private String nombre;

    @Schema(description = "Qué puede hacer el rol", example = "Atiende las solicitudes de contacto y consulta las métricas")
    private String descripcion;

    @Schema(description = "Si el rol se puede asignar", example = "true")
    private Boolean activo;

    /** El rol puede estar guardado con o sin el prefijo ROLE_; al exterior siempre se muestra sin él. */
    public static String nombreSinPrefijo(String nombre) {
        if (nombre == null) {
            return null;
        }
        return nombre.startsWith("ROLE_") ? nombre.substring(5) : nombre;
    }
}
