package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Solicitud pública de la tarjeta digital de Grupo Arquinova para Google Wallet")
public class PaseWalletRequestDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "El correo no es válido")
    @Size(max = 254, message = "El correo es demasiado largo")
    @Schema(description = "Correo al que se envía el enlace para guardar la tarjeta", example = "cliente@correo.com")
    private String email;
}
