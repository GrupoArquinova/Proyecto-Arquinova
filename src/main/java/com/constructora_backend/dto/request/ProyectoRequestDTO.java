package com.constructora_backend.dto.request;

import com.constructora_backend.enums.EstadoProyecto;
import com.constructora_backend.enums.TipoProyecto;
import com.constructora_backend.enums.TipoRegistroProyecto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Datos para la creación o actualización de un proyecto inmobiliario")
public class ProyectoRequestDTO {

    @NotNull(message = "La empresa es obligatoria")
    @Schema(description = "ID de la empresa a la que pertenece el proyecto", example = "1")
    private Long empresaId;

    @NotBlank(message = "El nombre del proyecto es obligatorio")
    @Size(max = 180, message = "El nombre no puede superar los 180 caracteres")
    @Schema(description = "Nombre comercial del proyecto", example = "Condominio Campestre Los Álamos")
    private String nombre;

    @NotBlank(message = "El slug es obligatorio")
    @Size(max = 200, message = "El slug no puede superar los 200 caracteres")
    @Schema(description = "Slug amigable para URL (único por empresa)", example = "condominio-campestre-los-alamos")
    private String slug;

    @Schema(description = "Descripción detallada del proyecto", example = "Exclusivo proyecto de lotes y casas campestres con vista a la cordillera.")
    private String descripcion;

    /** Texto en inglés para el sitio público (opcional). */
    private String descripcionEn;

    @Schema(description = "Etapa del proyecto", example = "EN_DISENO", allowableValues = {"EN_DISENO", "EN_TRAMITE", "EN_CONSTRUCCION", "FINALIZADO"})
    private EstadoProyecto estadoProyecto = EstadoProyecto.EN_DISENO;

    @Schema(description = "Caso de portafolio u oferta comercial", example = "OFERTA_COMERCIAL", allowableValues = {"PORTAFOLIO", "OFERTA_COMERCIAL"})
    private TipoRegistroProyecto tipoRegistro;

    @Schema(description = "Tipo de proyecto", example = "RURAL", allowableValues = {"RESIDENCIAL", "RURAL", "TURISTICO", "OTRO"})
    private TipoProyecto tipoProyecto;

    @Size(max = 255, message = "La participación no puede superar los 255 caracteres")
    @Schema(description = "Papel de Arquinova en el proyecto", example = "Diseño arquitectónico y gestión de licencias")
    private String participacion;

    /** Texto en inglés para el sitio público (opcional). */
    private String participacionEn;

    @Schema(description = "Si es el proyecto destacado del inicio (solo uno por empresa)", example = "false")
    private Boolean destacado;

    @Schema(description = "Indica si el proyecto es visible al público", example = "true")
    private Boolean publicado = false;

    @Schema(description = "Estado activo/inactivo del proyecto", example = "true")
    private Boolean activo = true;

    @Schema(description = "URL de la imagen principal del proyecto (Cloudinary)", example = "https://res.cloudinary.com/...")
    @Size(max = 1000, message = "La URL de la imagen no puede superar los 1000 caracteres")
    @Pattern(regexp = "^$|^https?://\\S+$",
             message = "La imagen debe ser una URL http(s) (por ejemplo de Cloudinary), no un archivo en Base64")
    private String imagenUrl;

    @Schema(description = "Fecha estimada o real de lanzamiento (YYYY-MM-DD)", example = "2026-11-01")
    private LocalDate fechaLanzamiento;

    @Schema(description = "ID del usuario creador (opcional)", example = "1")
    private Long creadoPorId;

    @Schema(description = "ID del usuario que realiza la modificación (opcional)", example = "1")
    private Long actualizadoPorId;
}
