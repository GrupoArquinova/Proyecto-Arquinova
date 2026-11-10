package com.constructora_backend.controller;

import com.constructora_backend.config.SecurityConfig;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.security.JwtFilter;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.AnaliticaService;
import com.constructora_backend.service.BrochureService;
import com.constructora_backend.service.SitemapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Brochure PDF y sitemap: públicos (seguridad real), cabeceras y registro de la descarga en la analítica. */
@WebMvcTest({BrochureController.class, SitemapController.class})
@Import({SecurityConfig.class, JwtFilter.class, SitemapService.class})
@TestPropertySource(properties = "app.sitio-url=https://www.arquinova.test/")
class BrochureYSitemapControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private BrochureService brochureService;
    @MockBean private AnaliticaService analiticaService;
    @MockBean private ProyectoRepository proyectoRepository;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private static final byte[] PDF = "%PDF-1.7 contenido".getBytes();

    @Test
    @DisplayName("Brochure: público, application/pdf, adjunto 'Brochure-<slug>.pdf' y registra DESCARGA_BROCHURE desde el servidor")
    void descargaElBrochure() throws Exception {
        when(brochureService.generar(6L)).thenReturn(new BrochureService.Brochure(PDF, "Brochure-el-encanto.pdf", 6L));

        mockMvc.perform(get("/api/proyectos/6/brochure.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Brochure-el-encanto.pdf\""))
                .andExpect(content().bytes(PDF));

        verify(analiticaService).registrarDelServidor(TipoEventoAnalitica.DESCARGA_BROCHURE, 6L);
    }

    @Test
    @DisplayName("Brochure: un proyecto no publicado o inexistente es 404 y no cuenta una descarga")
    void noPublicadoEs404() throws Exception {
        when(brochureService.generar(7L)).thenThrow(new ResourceNotFoundException("Proyecto no encontrado con ID: 7"));

        mockMvc.perform(get("/api/proyectos/7/brochure.pdf")).andExpect(status().isNotFound());

        verify(analiticaService, never()).registrarDelServidor(any(), any());
    }

    @Test
    @DisplayName("Brochure: si la analítica falla, el PDF se entrega igual")
    void fallaDeLaAnaliticaNoImpideLaDescarga() throws Exception {
        when(brochureService.generar(6L)).thenReturn(new BrochureService.Brochure(PDF, "Brochure-x.pdf", 6L));
        doThrow(new RuntimeException("BD caída")).when(analiticaService).registrarDelServidor(any(), any());

        mockMvc.perform(get("/api/proyectos/6/brochure.pdf")).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ sitemap

    private static Proyecto proyecto(long id, LocalDateTime actualizado) {
        Proyecto p = new Proyecto();
        p.setId(id);
        p.setActualizadoEn(actualizado);
        return p;
    }

    @Test
    @DisplayName("Sitemap: público, application/xml, caché de 1 hora; inicio, /proyectos y cada proyecto publicado con lastmod")
    void sitemapPublico() throws Exception {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(
                proyecto(6, LocalDateTime.of(2026, 10, 20, 8, 0)),
                proyecto(9, LocalDateTime.of(2026, 10, 25, 9, 30)),
                proyecto(11, null)));

        mockMvc.perform(get("/api/publico/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/xml")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=3600")))
                // El CDN de Vercel lo guarda y lo entrega aunque Render esté dormido o caído
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("s-maxage=3600")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("stale-while-revalidate=86400")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("stale-if-error=604800")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://www.arquinova.test/</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://www.arquinova.test/proyectos</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://www.arquinova.test/proyectos/6</loc>\n    <lastmod>2026-10-20</lastmod>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://www.arquinova.test/proyectos/9</loc>\n    <lastmod>2026-10-25</lastmod>")))
                // Sin fecha de actualización no hay lastmod (no se inventa)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://www.arquinova.test/proyectos/11</loc>\n  </url>")));
    }

    @Test
    @DisplayName("Sitemap: sin proyectos publicados sigue siendo un XML válido con inicio y /proyectos")
    void sitemapSinProyectos() throws Exception {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());

        mockMvc.perform(get("/api/publico/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://www.arquinova.test/proyectos</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.endsWith("</urlset>\n")));
    }
}
