package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de una eliminación en lote")
public record ResultadoEliminacionDTO(
        @Schema(description = "Cuántos registros se eliminaron", example = "42") long eliminados) {
}
