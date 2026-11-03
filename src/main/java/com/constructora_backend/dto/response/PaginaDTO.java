package com.constructora_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** Una página de resultados con lo necesario para pintar la paginación. */
@Data
@Schema(description = "Página de resultados")
public class PaginaDTO<T> {

    @Schema(description = "Elementos de esta página")
    private List<T> contenido;

    @Schema(description = "Número de página actual (empieza en 0)", example = "0")
    private int pagina;

    @Schema(description = "Elementos por página", example = "25")
    private int tamano;

    @Schema(description = "Total de elementos que cumplen el filtro", example = "134")
    private long totalElementos;

    @Schema(description = "Total de páginas", example = "6")
    private int totalPaginas;

    public static <T> PaginaDTO<T> de(org.springframework.data.domain.Page<T> pagina) {
        PaginaDTO<T> dto = new PaginaDTO<>();
        dto.setContenido(pagina.getContent());
        dto.setPagina(pagina.getNumber());
        dto.setTamano(pagina.getSize());
        dto.setTotalElementos(pagina.getTotalElements());
        dto.setTotalPaginas(pagina.getTotalPages());
        return dto;
    }
}
