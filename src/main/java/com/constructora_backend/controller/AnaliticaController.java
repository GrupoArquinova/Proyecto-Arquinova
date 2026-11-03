package com.constructora_backend.controller;

import com.constructora_backend.dto.request.EventoAnaliticaRequestDTO;
import com.constructora_backend.service.AnaliticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analítica propia SIN cookies: recibe los eventos del navegador. Público y sin captcha; el cupo por IP lo pone
 * RateLimitConfig (categoría ANALITICA, generosa pero acotada) y el tamaño del cuerpo LimiteTamanoFilter. La IP se usa
 * únicamente para ese cupo en memoria: no se guarda en ninguna parte.
 * <p>
 * Responde siempre 204, haya guardado el evento o no (tipo que solo registra el servidor, proyecto inexistente o sin
 * publicar...): así no se puede usar para averiguar qué proyectos existen.
 */
@RestController
@RequestMapping("/api/analitica")
@Tag(name = "Analítica", description = "Eventos de uso del sitio sin cookies ni identificadores de visitante")
public class AnaliticaController {

    private static final Logger log = LoggerFactory.getLogger(AnaliticaController.class);

    private final AnaliticaService analiticaService;

    @Autowired
    public AnaliticaController(AnaliticaService analiticaService) {
        this.analiticaService = analiticaService;
    }

    @PostMapping("/evento")
    @Operation(summary = "Registrar un evento de analítica",
            description = "Público. Cuerpo {tipo, proyectoId?, ruta?}. Tipos del navegador: VISTA_PAGINA, VISTA_PROYECTO, CLICK_WHATSAPP, COMPARTIR. "
                    + "FORMULARIO_ENVIADO, INICIO_CHAT y DESCARGA_BROCHURE los registra el servidor y aquí se ignoran. Responde 204 siempre que el cuerpo sea válido.")
    public ResponseEntity<Void> registrar(@Valid @RequestBody EventoAnaliticaRequestDTO dto) {
        try {
            analiticaService.registrarDelNavegador(dto.getTipo(), dto.getProyectoId(), dto.getRuta());
        } catch (Exception ex) {
            // La analítica no debe producir errores visibles en el sitio
            log.warn("Analítica: no se pudo registrar el evento {}: {}", dto.getTipo(), ex.getMessage());
        }
        return ResponseEntity.noContent().build();
    }
}
