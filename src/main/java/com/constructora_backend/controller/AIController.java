package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ChatRequest;
import com.constructora_backend.exception.AIServicioNoDisponibleException;
import com.constructora_backend.service.AIService;
import com.constructora_backend.service.ChatContextoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asistente de IA del sitio. Público, con límite de peticiones por IP (RateLimitConfig) y pregunta de máximo 500 caracteres.
 * Si la IA no puede responder devuelve 503 sin detalles: el sitio muestra su propio aviso.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIService aiService;
    private final ChatContextoService contextoService;

    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> chatearConIA(@Valid @RequestBody ChatRequest request) {
        try {
            String respuesta = aiService.generarRespuesta(request.getPregunta().trim(), contextoService.obtener(), request.getIdioma());
            return ResponseEntity.ok(respuesta);
        } catch (AIServicioNoDisponibleException e) {
            log.warn("Asistente de IA no disponible: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
