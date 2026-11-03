package com.constructora_backend.controller;

import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.service.AnaliticaService;
import com.constructora_backend.service.BrochureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Brochure PDF de un proyecto, generado al momento desde sus datos publicados. Público, con cupo por IP (categoría
 * BROCHURE de RateLimitConfig). Al entregarlo el servidor registra DESCARGA_BROCHURE en la analítica: el navegador NO debe
 * mandar ese evento.
 */
@RestController
@RequestMapping("/api/proyectos")
@Tag(name = "Brochure", description = "Brochure PDF por proyecto, generado automáticamente")
public class BrochureController {

    private static final Logger log = LoggerFactory.getLogger(BrochureController.class);

    private final BrochureService brochureService;
    private final AnaliticaService analiticaService;

    @Autowired
    public BrochureController(BrochureService brochureService,
                              @Autowired(required = false) AnaliticaService analiticaService) {
        this.brochureService = brochureService;
        this.analiticaService = analiticaService;
    }

    @GetMapping("/{id}/brochure.pdf")
    @Operation(summary = "Descargar el brochure PDF de un proyecto",
            description = "Público. Solo proyectos publicados y activos (404 si no). Sin precios. Content-Disposition: attachment; filename=\"Brochure-<slug>.pdf\". "
                    + "Registra DESCARGA_BROCHURE en la analítica (el navegador no debe enviarlo).")
    public ResponseEntity<byte[]> descargar(
            @Parameter(description = "ID del proyecto", example = "6") @PathVariable Long id) throws IOException {
        BrochureService.Brochure brochure = brochureService.generar(id);

        if (analiticaService != null) {
            try {
                analiticaService.registrarDelServidor(TipoEventoAnalitica.DESCARGA_BROCHURE, brochure.proyectoId());
            } catch (Exception ex) {
                log.warn("Analítica: no se pudo registrar la descarga del brochure: {}", ex.getMessage());
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(brochure.nombreArchivo()).build().toString())
                .contentLength(brochure.pdf().length)
                .body(brochure.pdf());
    }
}
