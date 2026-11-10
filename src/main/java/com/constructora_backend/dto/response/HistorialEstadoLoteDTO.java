package com.constructora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistorialEstadoLoteDTO {

    private Long id;
    private Long loteId;
    private String estadoAnterior;
    private String estadoNuevo;
    private String usuarioNombre;
    private String observaciones;
    private LocalDateTime cambiadoEn;
}
