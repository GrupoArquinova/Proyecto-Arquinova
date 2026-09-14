package com.constructora_backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EtapaResponseDTO {

    private Long id;
    private Long proyectoId;
    private String proyectoNombre;
    private String nombre;
    private String descripcion;
    private Short orden;
    private Boolean activo;
    private LocalDateTime creadoEn;
}
