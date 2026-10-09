package com.constructora_backend.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.boot.archive.scan.internal.ScanResultImpl;

@Data
public class SolicitudContactoPublicDTO {

    private Long proyectoId;

    private Long loteId;

    @NotBlank(message = "El nombre ess obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @Email(message = "Debe proporcionar un correo eletronico valido")
    @Size(max = 254, message = "El correo no puede superar los 254 caracteres")
    private String correo;

    @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
    private String telefono;

    @Size(max = 120, message = "El servicio de interes no puede superar los 120 caracteres")
    private String servicioInteres;

    /** Idioma del sitio cuando la persona escribió ("es" o "en"); sin dato se toma "es". */
    @Pattern(regexp = "^(es|en)$", message = "El idioma debe ser es o en")
    private String idioma;

    private String mensaje;

    @NotNull(message = "El consentimiento de tratamiento de datos es obligatorio")
    @AssertTrue(message = "Debe aceptar el consentimiento de tratamiento de datos")
    private Boolean consentimientoDatos;

    private String ip;

    private String userAgent;

    /** Basta con un medio de contacto: teléfono o correo. */
    @JsonIgnore
    @AssertTrue(message = "Debe indicar al menos un telefono o un correo electronico")
    public boolean isMedioDeContactoValido() {
        return (correo != null && !correo.isBlank()) || (telefono != null && !telefono.isBlank());
    }
}
