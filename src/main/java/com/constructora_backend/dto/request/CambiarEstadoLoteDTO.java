package com.constructora_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para solicitar el cambio de estado de un lote")
public class CambiarEstadoLoteDTO {

    @NotNull(message = "El ID del nuevo estado es obligatorio")
    @Schema(description = "ID del nuevo estado a asignar al lote", example = "2")
    private Short nuevoEstadoId;

    @Size(max = 500, message = "La observación no debe exceder los 500 caracteres")
    @JsonAlias({"observacio", "observacion", "observaciones"})
    @Schema(description = "Observaciones o justificación del cambio de estado", example = "Lote reservado por cliente tras pago de seña")
    private String observaciones;

    public String getObservacio() {
        return this.observaciones;
    }

    public void setObservacio(String observacio) {
        this.observaciones = observacio;
    }

    public String getObservacion() {
        return this.observaciones;
    }

    public void setObservacion(String observacion) {
        this.observaciones = observacion;
    }
}
