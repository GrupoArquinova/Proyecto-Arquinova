package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para la creación o actualización de una empresa constructora / socia")
public class EmpresaRequestDTO {

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Schema(description = "Nombre de la empresa", example = "Constructora Conclave")
    private String nombre;

    @Size(max = 30, message = "El NIT no puede superar los 30 caracteres")
    @Schema(description = "Número de Identificación Tributaria (NIT)", example = "900123456-1")
    private String nit;

    @Schema(description = "Descripción general de la empresa", example = "Especialistas en desarrollo urbano y proyectos residenciales.")
    private String descripcion;

    @Schema(description = "Trayectoria e historia de la constructora", example = "Más de 15 años liderando proyectos campestres en el Quindío.")
    private String trayectoria;

    @Schema(description = "Servicios ofrecidos", example = "Diseño, construcción, urbanización y consultoría arquitectónica.")
    private String servicios;

    @Email(message = "Debe ser una direccion de correo valida")
    @Size(max = 254, message = "El correo no puede superar los 254 caracteres")
    @Schema(description = "Correo electrónico comercial", example = "contacto@conclave.com")
    private String correoComercial;

    @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
    @Schema(description = "Teléfono fijo de contacto", example = "+57 606 7400000")
    private String telefono;

    @Size(max = 30, message = "El WhatsApp no puede superar los 30 caracteres")
    @Schema(description = "Número de WhatsApp para atención a clientes", example = "+57 310 1112233")
    private String whatsapp;

    @Size(max = 255, message = "El sitio web no puede superar los 255 caracteres")
    @Schema(description = "Sitio web oficial", example = "https://www.constructoraconclave.com")
    private String sitioWeb;

    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    @Schema(description = "Dirección física de la oficina principal", example = "Calle 19 Norte # 14-20, Armenia, Quindío")
    private String direccion;

    @Size(max = 1000, message = "La URL del logo no puede superar los 1000 caracteres")
    @Schema(description = "URL completa de la imagen del logotipo de la empresa", example = "https://example.com/logos/conclave.png")
    private String logoUrl;

    @Schema(description = "Estado activo/inactivo de la empresa en la plataforma", example = "true")
    private Boolean activo = true;
}
