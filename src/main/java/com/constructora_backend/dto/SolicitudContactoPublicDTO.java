package com.constructora_backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.boot.archive.scan.internal.ScanResultImpl;

@Data
public class SolicitudContactoPublicDTO {

    private Long proyectoId;

    private Long loteId;

    @NotBlank(message = "El nombre ess obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "Debe proporcionar un correo eletronico valido")
    @Size(max = 254, message = "El correo no puede superar los 254 caracteres")
    private String correo;

    @NotBlank(message = "El telefono es obligatorio")
    @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
    private String telefono;

    private String mensaje;

    @NotNull(message = "El consentimiento de tratamiento de datos es obligatorio")
    @AssertTrue(message = "Debe aceptar el consentimiento de tratamiento de datos")
    private Boolean consentimientoDatos;

    private String ip;

    private String userAgent;
}
