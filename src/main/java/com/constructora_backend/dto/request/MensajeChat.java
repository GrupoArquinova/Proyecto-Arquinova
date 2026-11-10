package com.constructora_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Un turno previo de la conversación con el asistente. Viene del navegador, así que se trata como texto no confiable. */
public class MensajeChat {

    /** "user" (la persona) o "model" (el asistente). */
    @NotBlank(message = "El rol del mensaje del historial es obligatorio")
    @Pattern(regexp = "^(user|model)$", message = "El rol del historial debe ser user o model")
    private String rol;

    @NotBlank(message = "Los mensajes del historial no pueden estar vacíos")
    @Size(max = 600, message = "Cada mensaje del historial no puede superar los 600 caracteres")
    private String texto;

    public MensajeChat() {
    }

    public MensajeChat(String rol, String texto) {
        this.rol = rol;
        this.texto = texto;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
