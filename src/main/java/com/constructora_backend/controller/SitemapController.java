package com.constructora_backend.controller;

import com.constructora_backend.service.SitemapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Sitemap para buscadores. El sitio (Vercel) lo expone como /sitemap.xml con una reescritura hacia este endpoint.
 * Caché de 1 hora (navegadores y CDN). Responde 404 si APP_SITIO_URL no está configurada (ver {@link SitemapService}).
 */
@RestController
@RequestMapping("/api/publico")
@Tag(name = "Público", description = "Recursos públicos para buscadores")
public class SitemapController {

    private final SitemapService sitemapService;

    @Autowired
    public SitemapController(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @GetMapping("/sitemap.xml")
    @Operation(summary = "Sitemap XML",
            description = "Inicio, /proyectos y cada /proyectos/{id} publicado y activo, con lastmod si hay fecha de actualización. "
                    + "Content-Type application/xml, caché de 1 hora. 404 si el sitio no tiene app.sitio-url configurada.")
    public ResponseEntity<String> sitemap() {
        return sitemapService.generar()
                .map(xml -> ResponseEntity.ok()
                        .contentType(new MediaType("application", "xml", java.nio.charset.StandardCharsets.UTF_8))
                        // s-maxage y stale-*: el CDN de Vercel lo guarda y lo sigue entregando aunque el backend esté dormido
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic().sMaxAge(Duration.ofHours(1))
                                .staleWhileRevalidate(Duration.ofDays(1)).staleIfError(Duration.ofDays(7)))
                        .body(xml))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
