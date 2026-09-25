package com.constructora_backend.dto.request;

import com.constructora_backend.enums.TipoMultimedia;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MultimediaRequestDTO {

    private Long proyectoId;
    private Long loteId;
    private Long zonaComunId;
    private Long casaModeloId;

    @NotNull(message = "El tipo de multimedia es obligatorio")
    private TipoMultimedia tipo;

    @Size(max = 200, message = "El titulo no debe superar los 200 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripcion no debe superar los 500 caracteres")
    private String descripcion;

    @NotBlank(message = "La URL del recurso es obligatorio")
    @Size(max = 150, message = "La URL no puede superar los 1500 caracteres")
    private String url;

    private String nombreArchivo;
    private String mimeType;

    @Min(value = 0, message = "El tamaño en bytes debe ser mayor o igual a 0")
    private Long tamanoBytes;

    @Min(value = 1, message = "El orden minimo deber ser 1")
    private Integer orden = 1;

    private Boolean portada = false;
    private Boolean publicado = true;
    private Boolean activo = true;
}
