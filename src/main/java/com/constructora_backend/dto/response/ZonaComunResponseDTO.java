package com.constructora_backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ZonaComunResponseDTO {

    private Long id;
    private Long proyectoId;
    private String proyectoNombre;
    private String nombre;
    private String descripcion;
    private Boolean publicado;
    private Boolean activo;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    // Galeria completa de imagenes asociados
    private List<ZonaComunImagenResponseDTO> imagenes;

    private String imagenPrincipalUrl;
}
