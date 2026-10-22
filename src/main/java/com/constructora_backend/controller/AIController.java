package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ChatRequest;
import com.constructora_backend.exception.AIServicioNoDisponibleException;
import com.constructora_backend.service.AIService;
import com.constructora_backend.service.ChatContextoService;
import com.constructora_backend.service.LimitadorVentana;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Semaphore;

/**
 * Asistente de IA del sitio. Público, con límite de peticiones por IP (RateLimitConfig) y pregunta de máximo 500 caracteres.
 * Además protege el gasto de la IA: como mucho 5 consultas a la vez (si no hay cupo responde 429 de inmediato, sin
 * esperar) y una cuota diaria global (si se agota responde 503). Si la IA no puede responder devuelve 503 sin detalles:
 * el sitio muestra su propio aviso.
 */
@RestController
@RequestMapping("/api/chat")
@Slf4j
public class AIController {

    /** Consultas simultáneas máximas a la IA por instancia. */
    public static final int CONCURRENCIA_MAXIMA = 5;

    private final AIService aiService;
    private final ChatContextoService contextoService;
    private final LimitadorVentana limitador;
    private final Semaphore concurrencia;
    /** Preguntas al día entre todos los visitantes; 0 = sin tope (GEMINI_CUOTA_DIARIA). */
    private final int cuotaDiaria;

    @Autowired
    public AIController(AIService aiService,
                        ChatContextoService contextoService,
                        @Autowired(required = false) LimitadorVentana limitador,
                        @Value("${gemini.concurrencia-maxima:" + CONCURRENCIA_MAXIMA + "}") int concurrenciaMaxima,
                        @Value("${gemini.cuota-diaria:1000}") int cuotaDiaria) {
        this.aiService = aiService;
        this.contextoService = contextoService;
        this.limitador = limitador != null ? limitador : new LimitadorVentana();
        this.concurrencia = new Semaphore(Math.max(1, concurrenciaMaxima));
        this.cuotaDiaria = cuotaDiaria;
    }

    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> chatearConIA(@Valid @RequestBody ChatRequest request) {
        // Sin cupo de concurrencia no se hace esperar a nadie: respuesta inmediata
        if (!concurrencia.tryAcquire()) {
            log.warn("Asistente de IA saturado: {} consultas en curso", CONCURRENCIA_MAXIMA);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "10").build();
        }
        String claveDia = null;
        try {
            if (cuotaDiaria > 0) {
                claveDia = "chat:dia:" + LocalDate.now(ZoneId.of("America/Bogota"));
                if (!limitador.intentar(claveDia, cuotaDiaria, Duration.ofHours(26))) {
                    log.warn("Asistente de IA: se agotó la cuota diaria ({} preguntas)", cuotaDiaria);
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
                }
            }
            String respuesta = aiService.generarRespuesta(request.getPregunta().trim(), contextoService.obtener(), request.getIdioma());
            return ResponseEntity.ok(respuesta);
        } catch (AIServicioNoDisponibleException e) {
            // La consulta falló sin dar respuesta: no cuenta contra la cuota del día
            if (claveDia != null) {
                limitador.devolver(claveDia);
            }
            log.warn("Asistente de IA no disponible: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } finally {
            concurrencia.release();
        }
    }
}
