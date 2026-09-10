package com.constructora_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Schema(description = "Objeto de respuesta tras autenticación exitosa")
public class AuthResponseDTO {

    @Schema(description = "Token JWT generado para autenticar peticiones subsecuentes", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Tipo de esquema de autorización", example = "Bearer")
    private String tipo;

    public AuthResponseDTO(String token) {
        this.token = token;
        this.tipo = "Bearer";
    }
}
