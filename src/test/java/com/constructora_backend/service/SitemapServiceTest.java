package com.constructora_backend.service;

import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.repository.ProyectoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SitemapServiceTest {

    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final SitemapService servicio = new SitemapService(proyectos);

    private static Proyecto proyecto(long id, LocalDateTime actualizado) {
        Proyecto p = new Proyecto();
        p.setId(id);
        p.setActualizadoEn(actualizado);
        return p;
    }

    @Test
    @DisplayName("Sin app.sitio-url no hay sitemap (el controlador responde 404): un sitemap con direcciones inventadas sería inválido")
    void sinDireccionPublicaNoHaySitemap() {
        ReflectionTestUtils.setField(servicio, "sitioUrl", "");
        assertTrue(servicio.generar().isEmpty());
        ReflectionTestUtils.setField(servicio, "sitioUrl", "   ");
        assertTrue(servicio.generar().isEmpty());
    }

    @Test
    @DisplayName("Quita las barras finales de la dirección base y usa la fecha más reciente para el inicio y /proyectos")
    void baseSinBarraYFechaDelInicio() {
        ReflectionTestUtils.setField(servicio, "sitioUrl", "https://www.arquinova.test//");
        when(proyectos.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(
                proyecto(1, LocalDateTime.of(2026, 3, 1, 10, 0)), proyecto(2, LocalDateTime.of(2026, 9, 9, 10, 0))));

        String xml = servicio.generar().orElseThrow();

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset"));
        assertTrue(xml.contains("<loc>https://www.arquinova.test/</loc>\n    <lastmod>2026-09-09</lastmod>"));
        assertTrue(xml.contains("<loc>https://www.arquinova.test/proyectos</loc>\n    <lastmod>2026-09-09</lastmod>"));
        assertTrue(xml.contains("<loc>https://www.arquinova.test/proyectos/1</loc>\n    <lastmod>2026-03-01</lastmod>"));
        assertFalse(xml.contains("//proyectos"));
        assertEquals(4, xml.split("<url>").length - 1);
    }

    @Test
    @DisplayName("Los caracteres especiales de XML se escapan")
    void escapaXml() {
        assertEquals("a&amp;b&lt;c&gt;&quot;d&quot;&apos;e&apos;", SitemapService.escapar("a&b<c>\"d\"'e'"));
        ReflectionTestUtils.setField(servicio, "sitioUrl", "https://x.test/?a=1&b=2");
        when(proyectos.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        assertTrue(servicio.generar().orElseThrow().contains("a=1&amp;b=2"));
    }
}
