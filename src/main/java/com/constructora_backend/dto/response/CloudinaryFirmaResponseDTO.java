package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datos que el frontend necesita para subir un archivo directamente a Cloudinary
 * con una firma generada por el backend. El API secret nunca sale del servidor.
 */
@Schema(description = "Parámetros firmados para una subida directa a Cloudinary")
public record CloudinaryFirmaResponseDTO(
        @Schema(description = "Nombre de la nube de Cloudinary", example = "dn3s7utod")
        String cloudName,
        @Schema(description = "API key pública de Cloudinary")
        String apiKey,
        @Schema(description = "Marca de tiempo (segundos UNIX) usada en la firma", example = "1791208744")
        long timestamp,
        @Schema(description = "Carpeta de destino en Cloudinary (incluida en la firma)", example = "arquinova")
        String folder,
        @Schema(description = "Firma SHA-1 en hexadecimal")
        String signature
) {
}
