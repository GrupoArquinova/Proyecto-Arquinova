package com.constructora_backend.dto.response;

import com.constructora_backend.enums.EscenaPunto360;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class Punto360ResponseDTO {

    private Long id;
    private Long proyectoId;
    private EscenaPunto360 escena;
    private String etiqueta;

    private Long loteId;
    private String loteCodigo;
    private BigDecimal loteAreaM2;
    private String loteEstado;

    private Long etapaId;
    private String etapaNombre;

    private Long zonaComunId;
    private String zonaComunNombre;

    private BigDecimal yaw;
    private BigDecimal pitch;
    private BigDecimal posX;
    private BigDecimal posY;
}
