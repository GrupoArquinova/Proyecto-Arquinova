package com.constructora_backend.controller;

import com.constructora_backend.service.CompartirService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Etiquetas para compartir un proyecto en WhatsApp, Facebook, LinkedIn y X. El sitio (Vercel) reescribe hacia aquí
 * /proyectos/{id} solo cuando quien pide es uno de esos rastreadores (ver vercel.json); las personas reciben la aplicación.
 */
@RestController
@RequestMapping("/api/publico/compartir")
@Tag(name = "Público", description = "Recursos públicos para buscadores y redes sociales")
public class CompartirController {

    private final CompartirService compartirService;

    @Autowired
    public CompartirController(CompartirService compartirService) {
        this.compartirService = compartirService;
    }

    @GetMapping("/proyectos/{id}")
    @Operation(summary = "Etiquetas para compartir un proyecto",
            description = "HTML mínimo con título, descripción y portada (Open Graph y X/Twitter) de un proyecto publicado y activo. "
                    + "404 si no existe o no está publicado. Caché de 5 minutos en el navegador y 1 hora en el CDN.")
    public ResponseEntity<String> proyecto(@PathVariable Long id) {
        return compartirService.paginaDelProyecto(id)
                .map(html -> ResponseEntity.ok()
                        .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()
                                .sMaxAge(Duration.ofHours(1)).staleWhileRevalidate(Duration.ofDays(1)))
                        .body(html))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
