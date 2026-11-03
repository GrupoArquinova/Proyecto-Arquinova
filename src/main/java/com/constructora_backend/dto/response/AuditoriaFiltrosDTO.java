package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Valores que existen en el historial, para llenar los filtros del panel")
public class AuditoriaFiltrosDTO {

    @Schema(description = "Entidades con cambios registrados", example = "[\"LOTES\", \"PROYECTOS\"]")
    private List<String> entidades;

    @Schema(description = "Acciones registradas", example = "[\"ACTUALIZAR_PROYECTO\", \"CREAR_LOTE\"]")
    private List<String> acciones;
}
