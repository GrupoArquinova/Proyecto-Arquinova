package com.constructora_backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SolicitudContactoResponseDTO {

    private Long Id;
    private Integer estadoId;
    private String estadoNombre;
    private Long proyectoId;
    private String proyectoNombre;
    private Long loteId;
    private String loteCodigo;
    private String nombre;
    private String correo;
    private String telefono;
    private String mensaje;
    private Boolean consentimientoDatos;
    private String ip;
    private String userAgent;
    private Long atendidaPorId;
    private String atentidaPorNombre;
    private LocalDateTime atentidaEn;
    private String observacionesInternas;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
