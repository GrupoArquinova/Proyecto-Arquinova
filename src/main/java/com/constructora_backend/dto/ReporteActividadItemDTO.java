package com.constructora_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteActividadItemDTO {

    private Long id;
    private String usuarioNombre;
    private String usuarioCorreo;
    private String accion;
    private String entidad;
    private Long entidadId;
    private String descripcion;
    private String ip;
    private LocalDateTime creadoEn;
}