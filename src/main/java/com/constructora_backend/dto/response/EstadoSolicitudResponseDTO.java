package com.constructora_backend.dto.response;

import lombok.Data;

@Data
public class EstadoSolicitudResponseDTO {

    private Integer id;
    private String nombre;
    private String descripcion;
    private Integer orden;
    private Boolean activo;
}
