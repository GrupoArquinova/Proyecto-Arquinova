package com.constructora_backend.dto.request;

import com.constructora_backend.enums.TipoEventoAnalitica;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Evento de analítica enviado por el navegador (sin cookies ni identificadores)")
public class EventoAnaliticaRequestDTO {

    @NotNull(message = "El tipo de evento es obligatorio")
    @Schema(description = "Tipo de evento. FORMULARIO_ENVIADO, INICIO_CHAT y DESCARGA_BROCHURE los registra el servidor: si llegan aquí se ignoran.",
            example = "VISTA_PROYECTO")
    private TipoEventoAnalitica tipo;

    @Schema(description = "Proyecto relacionado (opcional). Si no existe o no está publicado, el evento se ignora.", example = "6")
    private Long proyectoId;

    @Schema(description = "Ruta del sitio (opcional); se guarda sin querystring ni fragmento y recortada a 200 caracteres.",
            example = "/proyectos/6/ubicacion")
    private String ruta;
}
