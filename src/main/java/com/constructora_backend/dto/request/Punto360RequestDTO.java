package com.constructora_backend.dto.request;

import com.constructora_backend.enums.EscenaPunto360;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Datos de un punto (botón) sobre una imagen 360° o el plano de urbanismo")
public class Punto360RequestDTO {

    @NotNull(message = "El ID del proyecto es obligatorio")
    private Long proyectoId;

    @NotNull(message = "La escena es obligatoria")
    @Schema(description = "ENTORNO, AEREA, URBANISMO o ZONAS", example = "ENTORNO")
    private EscenaPunto360 escena;

    @Schema(description = "Lote al que apunta el botón (ENTORNO y AEREA)")
    private Long loteId;

    @Schema(description = "Etapa a la que apunta el botón (URBANISMO)")
    private Long etapaId;

    @Schema(description = "Zona común a la que apunta el botón (ZONAS)")
    private Long zonaComunId;

    @NotBlank(message = "La etiqueta es obligatoria")
    @Size(max = 120, message = "La etiqueta no puede superar los 120 caracteres")
    private String etiqueta;

    @Schema(description = "Ángulo horizontal en radianes (ENTORNO y AEREA)")
    private BigDecimal yaw;

    @Schema(description = "Ángulo vertical en radianes (ENTORNO y AEREA)")
    private BigDecimal pitch;

    @DecimalMin(value = "0", message = "posX debe estar entre 0 y 100")
    @DecimalMax(value = "100", message = "posX debe estar entre 0 y 100")
    @Schema(description = "Posición horizontal en % sobre el plano (URBANISMO)")
    private BigDecimal posX;

    @DecimalMin(value = "0", message = "posY debe estar entre 0 y 100")
    @DecimalMax(value = "100", message = "posY debe estar entre 0 y 100")
    @Schema(description = "Posición vertical en % sobre el plano (URBANISMO)")
    private BigDecimal posY;
}
