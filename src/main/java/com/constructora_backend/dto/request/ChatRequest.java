package com.constructora_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChatRequest {

    @NotBlank(message = "La pregunta es obligatoria")
    @Size(max = 500, message = "La pregunta no puede superar los 500 caracteres")
    private String pregunta;

    /** Idioma del sitio cuando la persona escribe ("es" o "en"); sin dato se responde en el idioma de la pregunta. */
    @Pattern(regexp = "^(es|en)$", message = "El idioma debe ser es o en")
    private String idioma;

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }
}
