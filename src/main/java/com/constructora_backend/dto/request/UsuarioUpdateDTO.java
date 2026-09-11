package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Objeto para actualización de datos de usuario")
public class UsuarioUpdateDTO {

    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez Actualizado")
    private String nombreCompleto;

    @Email(message = "Debe proporcionar un correo valido")
    @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
    @Schema(description = "Correo electrónico del usuario", example = "juan.actualizado@arquinova.com")
    private String correo;

    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Schema(description = "Nueva contraseña (opcional)", example = "Nuevapassword123")
    private String password;

    @Schema(description = "ID del nuevo rol a asignar (opcional)", example = "2")
    private Long rolId;

    @Schema(description = "Nuevo estado de la cuenta (opcional)", example = "true")
    private Boolean activo;
}
