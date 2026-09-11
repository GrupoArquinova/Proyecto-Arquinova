package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Respuesta con información detallada de una empresa constructora")
public class EmpresaResponseDTO {

    @Schema(description = "Identificador único de la empresa", example = "1")
    private Long id;

    @Schema(description = "Nombre de la empresa", example = "Constructora Conclave")
    private String nombre;

    @Schema(description = "NIT de la empresa", example = "900123456-1")
    private String nit;

    @Schema(description = "Descripción general", example = "Especialistas en desarrollo urbano...")
    private String descripcion;

    @Schema(description = "Trayectoria e historia", example = "Más de 15 años liderando proyectos...")
    private String trayectoria;

    @Schema(description = "Servicios prestados", example = "Diseño, construcción, urbanización")
    private String servicios;

    @Schema(description = "Correo comercial", example = "contacto@conclave.com")
    private String correoComercial;

    @Schema(description = "Teléfono de contacto", example = "+57 606 7400000")
    private String telefono;

    @Schema(description = "WhatsApp comercial", example = "+57 310 1112233")
    private String whatsapp;

    @Schema(description = "URL del sitio web oficial", example = "https://www.constructoraconclave.com")
    private String sitioWeb;

    @Schema(description = "Dirección de la empresa", example = "Calle 19 Norte # 14-20, Armenia")
    private String direccion;

    @Schema(description = "URL del logotipo", example = "https://example.com/logos/conclave.png")
    private String logoUrl;

    @Schema(description = "Estado de la empresa", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha y hora de registro")
    private LocalDateTime creadoEn;

    @Schema(description = "Fecha y hora de última actualización")
    private LocalDateTime actualizadoEn;
}
