package com.constructora_backend.dto.response;

import com.constructora_backend.enums.TipoMultimedia;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MultimediaResponseDTO {
    private Long id;
    private Long proyectoId;
    private Long loteId;
    private Long zonaComunId;
    private Long casaModeloId;
    private TipoMultimedia tipo;
    private String titulo;
    private String descripcion;
    private String url;
    private String nombreArchivo;
    private String mimeType;
    private Long tamanoBytes;
    private Integer orden;
    private Boolean portada;
    private Boolean publicado;
    private Boolean activo;
    private String creadoPorNombre;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}