package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para la confirmación de restablecimiento de contraseña.
 * Aplica política de seguridad estricta compatible con OWASP Authentication Guidelines.
 */
@Data
@Schema(description = "Datos para restablecer la contraseña con un token de recuperación válido")
public class ResetPasswordRequest {

    @NotBlank(message = "El token de recuperación es obligatorio")
    @Schema(description = "Token de recuperación recibido por correo electrónico", example = "abc123xyz...")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "La contraseña debe contener al menos: una mayúscula, una minúscula, un número y un carácter especial (!@#$%^&*...)"
    )
    @Schema(
            description = "Nueva contraseña (mínimo 8 caracteres, debe incluir mayúscula, minúscula, número y carácter especial)",
            example = "NuevaPass123!"
    )
    private String nuevaPassword;
}
