package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para la creación de un nuevo usuario en el sistema.
 * Aplica política de contraseña estricta (OWASP Authentication Cheat Sheet).
 */
@Data
@Schema(description = "Datos para la creación de un nuevo usuario")
public class UsuarioRequestDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    private String nombreCompleto;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe proporcionar un correo válido")
    @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
    @Schema(description = "Correo electrónico del usuario", example = "juan.perez@arquinova.com")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "La contraseña debe contener al menos: una mayúscula, una minúscula, un número y un carácter especial (!@#$%^&*...)"
    )
    @Schema(
            description = "Contraseña (mínimo 8 caracteres, debe incluir mayúscula, minúscula, número y carácter especial)",
            example = "Password123!"
    )
    private String password;

    @NotNull(message = "El ID del rol es obligatorio")
    @Schema(description = "ID del rol asignado al usuario", example = "1")
    private Long rolId;

    @Schema(description = "Estado inicial de la cuenta (activo/inactivo)", example = "true")
    private Boolean activo = true;
}

