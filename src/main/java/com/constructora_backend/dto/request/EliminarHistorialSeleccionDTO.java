package com.constructora_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Registros del historial de cambios que se van a eliminar, elegidos uno por uno")
public class EliminarHistorialSeleccionDTO {

    @NotEmpty(message = "Elige al menos un registro")
    @Size(max = 1000, message = "Se pueden eliminar hasta 1000 registros por vez")
    @Schema(description = "IDs de los registros", example = "[101, 102, 103]")
    private List<@NotNull(message = "Hay un registro sin identificador") Long> ids;
}
