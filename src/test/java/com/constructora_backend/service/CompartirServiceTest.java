package com.constructora_backend.service;

import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.repository.ProyectoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompartirServiceTest {

    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final CompartirService servicio = new CompartirService(proyectos);

    private Proyecto proyecto(String nombre, String descripcion, String imagen, boolean publicado, boolean activo) {
        Proyecto p = new Proyecto();
        p.setId(6L);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setImagenUrl(imagen);
        p.setPublicado(publicado);
        p.setActivo(activo);
        when(proyectos.findById(6L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    @DisplayName("Un proyecto publicado lleva título, descripción, portada 1200×630 y dirección canónica")
    void proyectoPublicado() {
        ReflectionTestUtils.setField(servicio, "sitioUrl", "https://www.arquinova.test/");
        proyecto("Villas El Encanto", "Lotes campestres\ncon vista al valle", "https://res.cloudinary.com/dn3s7utod/image/upload/v17/portada.jpg", true, true);

        String html = servicio.paginaDelProyecto(6L).orElseThrow();

        assertTrue(html.contains("<title>Villas El Encanto | Grupo Arquinova</title>"));
        assertTrue(html.contains("<meta property=\"og:title\" content=\"Villas El Encanto | Grupo Arquinova\">"));
        assertTrue(html.contains("<meta property=\"og:description\" content=\"Lotes campestres con vista al valle\">"));
        assertTrue(html.contains("<meta property=\"og:image\" content=\"https://res.cloudinary.com/dn3s7utod/image/upload/c_fill,g_auto,w_1200,h_630,f_jpg,q_auto/v17/portada.jpg\">"));
        assertTrue(html.contains("<meta property=\"og:url\" content=\"https://www.arquinova.test/proyectos/6\">"));
        assertTrue(html.contains("<link rel=\"canonical\" href=\"https://www.arquinova.test/proyectos/6\">"));
        assertTrue(html.contains("<meta name=\"twitter:card\" content=\"summary_large_image\">"));
    }

    @Test
    @DisplayName("Sin dirección pública configurada no hay og:url ni canonical (no se inventan)")
    void sinDireccionPublica() {
        ReflectionTestUtils.setField(servicio, "sitioUrl", "");
        proyecto("Villas", "Texto", null, true, true);

        String html = servicio.paginaDelProyecto(6L).orElseThrow();

        assertFalse(html.contains("og:url"));
        assertFalse(html.contains("canonical"));
    }

    @Test
    @DisplayName("Lo que escribe el administrador se escapa: un nombre con comillas o etiquetas no rompe el HTML")
    void textoEscapado() {
        proyecto("Casa \"Sol\" <script>alert(1)</script>", "Vista & aire <b>puro</b>", null, true, true);

        String html = servicio.paginaDelProyecto(6L).orElseThrow();

        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<b>"));
        assertTrue(html.contains("Casa &quot;Sol&quot; &lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(html.contains("Vista &amp; aire &lt;b&gt;puro&lt;/b&gt;"));
    }

    @Test
    @DisplayName("Sin portada o con una portada que no es https usa la imagen de la marca; sin descripción usa la de la empresa")
    void valoresPorDefecto() {
        proyecto("Villas", "  ", "http://inseguro.test/foto.jpg", true, true);

        String html = servicio.paginaDelProyecto(6L).orElseThrow();

        assertTrue(html.contains("<meta property=\"og:image\" content=\"" + CompartirService.IMAGEN_POR_DEFECTO + "\">"));
        assertTrue(html.contains("<meta property=\"og:description\" content=\"" + CompartirService.DESCRIPCION_POR_DEFECTO.replace("&", "&amp;") + "\">"));
        assertEquals(CompartirService.IMAGEN_POR_DEFECTO, CompartirService.imagenParaCompartir(null));
        assertEquals("https://otro.test/foto.jpg", CompartirService.imagenParaCompartir(" https://otro.test/foto.jpg "));
    }

    @Test
    @DisplayName("Una descripción larga se corta en una palabra completa con máximo 160 letras")
    void descripcionLarga() {
        String larga = "palabra ".repeat(60);
        String corta = CompartirService.recortar(larga.trim());

        assertTrue(corta.length() <= 160);
        assertTrue(corta.endsWith("palabra…"));
    }

    @Test
    @DisplayName("Un proyecto no publicado, inactivo o inexistente no se anuncia")
    void noSeAnuncia() {
        proyecto("Borrador", "Texto", null, false, true);
        assertTrue(servicio.paginaDelProyecto(6L).isEmpty());

        proyecto("Retirado", "Texto", null, true, false);
        assertTrue(servicio.paginaDelProyecto(6L).isEmpty());

        when(proyectos.findById(99L)).thenReturn(Optional.empty());
        assertTrue(servicio.paginaDelProyecto(99L).isEmpty());
    }
}
