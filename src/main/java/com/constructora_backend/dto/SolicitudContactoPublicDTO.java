package com.constructora_backend.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Datos que envía una persona desde el formulario público. La IP y el user-agent NO vienen aquí: los fija el servidor
 * a partir de la petición (si un cliente los manda, se ignoran).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolicitudContactoPublicDTO {

    /** Ningún dato de una sola línea (nombre, teléfono, servicio) admite saltos de línea ni caracteres de control. */
    static final String SIN_CONTROL = "^[^\\p{Cntrl}]*$";
    static final String SIN_CONTROL_MENSAJE = "No puede contener saltos de línea ni caracteres de control";

    private Long proyectoId;

    private Long loteId;

    /** Tipología (villa) por la que se consulta; opcional. */
    private Long casaModeloId;

    @NotBlank(message = "El nombre ess obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Pattern(regexp = SIN_CONTROL, message = SIN_CONTROL_MENSAJE)
    private String nombre;

    @Email(message = "Debe proporcionar un correo eletronico valido")
    @Size(max = 254, message = "El correo no puede superar los 254 caracteres")
    private String correo;

    @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
    @Pattern(regexp = SIN_CONTROL, message = SIN_CONTROL_MENSAJE)
    private String telefono;

    @Size(max = 120, message = "El servicio de interes no puede superar los 120 caracteres")
    @Pattern(regexp = SIN_CONTROL, message = SIN_CONTROL_MENSAJE)
    private String servicioInteres;

    /** Idioma del sitio cuando la persona escribió ("es" o "en"); sin dato se toma "es". */
    @Pattern(regexp = "^(es|en)$", message = "El idioma debe ser es o en")
    private String idioma;

    @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
    private String mensaje;

    @NotNull(message = "El consentimiento de tratamiento de datos es obligatorio")
    @AssertTrue(message = "Debe aceptar el consentimiento de tratamiento de datos")
    private Boolean consentimientoDatos;

    /**
     * Campo trampa (honeypot): oculto en el formulario, una persona nunca lo llena. Si llega con texto es un robot: se
     * responde como si todo hubiera salido bien, pero no se guarda nada ni se envía ningún correo.
     */
    private String sitioWeb;

    /** Basta con un medio de contacto: teléfono o correo. */
    @JsonIgnore
    @AssertTrue(message = "Debe indicar al menos un telefono o un correo electronico")
    public boolean isMedioDeContactoValido() {
        return (correo != null && !correo.isBlank()) || (telefono != null && !telefono.isBlank());
    }

    /** true si el campo trampa vino con texto. */
    @JsonIgnore
    public boolean esRobot() {
        return sitioWeb != null && !sitioWeb.isBlank();
    }
}
