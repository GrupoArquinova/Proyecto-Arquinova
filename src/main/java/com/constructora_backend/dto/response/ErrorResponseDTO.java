package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estructura estándar de respuesta para errores de la API")
public class ErrorResponseDTO {

    @Schema(description = "Fecha y hora en que ocurrió el error", example = "2026-09-10T11:45:00")
    private LocalDateTime timestamp;

    @Schema(description = "Código de estado HTTP", example = "400")
    private int status;

    @Schema(description = "Nombre descriptivo del estado HTTP", example = "Bad Request")
    private String error;

    @Schema(description = "Mensaje explicativo del error", example = "Error de validación en los datos enviados")
    private String mensaje;

    @Schema(description = "Ruta o URI de la petición que generó el error", example = "/api/usuarios")
    private String path;

    @Schema(description = "Detalles adicionales o mapa de errores por campo (si aplica)")
    private Map<String, String> detalles;

    @Schema(description = "Identificador único de trazabilidad para rastreo en logs (Correlation ID)", example = "a5f8b3c9-1234-5678-9abc-def012345678")
    private String correlationId;
}
