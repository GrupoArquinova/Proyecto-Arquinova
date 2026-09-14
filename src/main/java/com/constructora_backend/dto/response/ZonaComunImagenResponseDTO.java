package com.constructora_backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZonaComunImagenResponseDTO {

    private Long id;
    private Long zonaComunId;
    private String zonaComunNombre;
    private String imagenUrl;
    private String titulo;
    private Short orden;
    private Boolean esPrincipal;
    private LocalDateTime creadoEn;
}
