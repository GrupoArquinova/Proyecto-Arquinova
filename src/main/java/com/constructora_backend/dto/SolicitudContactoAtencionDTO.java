package com.constructora_backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SolicitudContactoAtencionDTO {

    @NotNull(message = "El ID del estado es obligatorio")
    private Integer estadoId;

    @JsonAlias("atentidaPorId")
    private Long atendidaPorId;

    private String observacionesInternas;

    public Long getAtentidaPorId() {
        return this.atendidaPorId;
    }

    public void setAtentidaPorId(Long atentidaPorId) {
        this.atendidaPorId = atentidaPorId;
    }
}
