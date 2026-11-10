package com.constructora_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteSolicitudItemDTO {

    private Long id;
    private String proyectoNombre;
    private String loteCodigo;
    private String clienteNombre;
    private String clienteCorreo;
    private String clienteTelefono;
    private String estadoNombre;
    private String atendidaPorNombre;
    private LocalDateTime creadoEn;
    /** Tipología (villa) por la que se consultó; vacío si no eligió una. */
    private String tipologiaNombre;
    private String mensaje;
    private LocalDateTime atendidaEn;
}