package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Respuesta con información detallada de un contenido institucional")
public class ContenidoInstitucionalResponseDTO {

    @Schema(description = "Identificador único del contenido institucional", example = "1")
    private Long id;

    @Schema(description = "ID de la empresa propietaria", example = "1")
    private Long empresaId;

    @Schema(description = "Nombre de la empresa propietaria", example = "Constructora Conclave")
    private String empresaNombre;

    @Schema(description = "Sección institucional", example = "MISION")
    private String seccion;

    @Schema(description = "Título descriptivo del contenido", example = "Nuestra Misión")
    private String titulo;

    @Schema(description = "Contenido completo de la sección")
    private String contenido;

    @Schema(description = "Estado de publicación", example = "true")
    private Boolean publicado;

    @Schema(description = "ID del último usuario que actualizó el contenido", example = "1")
    private Long actualizadoPorId;

    @Schema(description = "Nombre del último usuario que actualizó el contenido", example = "Admin Arquinova")
    private String actualizadoPorNombre;

    @Schema(description = "Fecha y hora de creación")
    private LocalDateTime creadoEn;

    @Schema(description = "Fecha y hora de última actualización")
    private LocalDateTime actualizadoEn;
}
