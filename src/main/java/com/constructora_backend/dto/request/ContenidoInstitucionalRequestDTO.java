package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para la creación o actualización de un contenido institucional de una empresa")
public class ContenidoInstitucionalRequestDTO {

    @NotNull(message = "El ID de la empresa es obligatorio")
    @Schema(description = "ID de la empresa a la que pertenece este contenido", example = "1")
    private Long empresaId;

    @NotBlank(message = "La sección es obligatoria")
    @Size(max = 80, message = "La sección no puede superar los 80 caracteres")
    @Schema(description = "Nombre de la sección institucional (ej: MISION, VISION, QUIENES_SOMOS, VALORES, POLITICAS)",
            example = "MISION")
    private String seccion;

    @Size(max = 200, message = "El título no puede superar los 200 caracteres")
    @Schema(description = "Título descriptivo del contenido", example = "Nuestra Misión")
    private String titulo;

    @Schema(description = "Contenido completo en texto o HTML de la sección",
            example = "Somos una constructora comprometida con la calidad y la innovación arquitectónica.")
    private String contenido;

    @Schema(description = "Indica si el contenido es visible públicamente", example = "true")
    private Boolean publicado = true;

    @Schema(description = "ID del usuario que realiza la actualización (se puede obtener del JWT)", example = "1")
    private Long actualizadoPorId;
}
