package com.constructora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarEstadoLoteDTO {

    @NotNull(message = "El ID del nuevo es obligatorio")
    private Short nuevoEstadoId;

    @Size(max = 500, message = "La observacion no debe exceder los 500 caracteres")
    private String observacio;
}
