package com.constructora_backend.service;

import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.enums.TipoRegistroProyecto;
import com.constructora_backend.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Brochure PDF con la base real: el PDF se abre con PDFBox y se lee su texto. Todo se revierte al terminar. */
@SpringBootTest
@Transactional
class BrochureIntegracionTest {

    @Autowired private BrochureService brochure;
    @PersistenceContext private EntityManager em;

    private static String texto(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
        }
    }

    private Proyecto proyectoConDatos(String nombre, TipoRegistroProyecto tipo) {
        Empresa empresa = FixturasContenido.empresa(em);
        Proyecto p = FixturasContenido.proyecto(em, empresa, nombre, true, true, tipo);
        FixturasContenido.ubicacion(em, p);
        FixturasContenido.villa(em, p, "Villa Samán");
        FixturasContenido.zona(em, p, "Piscina");
        FixturasContenido.zona(em, p, "Jacuzzi");
        Etapa etapa = FixturasContenido.etapa(em, p, "Etapa 1");
        var disponible = FixturasContenido.estadoLote(em, "DISPONIBLE");
        var vendido = FixturasContenido.estadoLote(em, "VENDIDO");
        FixturasContenido.lote(em, etapa, disponible, "L-1", "1000");
        FixturasContenido.lote(em, etapa, disponible, "L-2", "1500.50");
        FixturasContenido.lote(em, etapa, vendido, "L-3", "900");
        em.flush();
        em.clear();
        return p;
    }

    @Test
    @DisplayName("Un proyecto de venta: PDF válido con nombre, empresa, ubicación, villas, amenidades, lotes disponibles, contacto y aviso; sin precios")
    void brochureDeUnaOferta() throws Exception {
        String nombre = "El Encanto " + FixturasContenido.sufijo();
        Proyecto p = proyectoConDatos(nombre, TipoRegistroProyecto.OFERTA_COMERCIAL);
        Object anterior = ReflectionTestUtils.getField(brochure, "sitioUrl");
        ReflectionTestUtils.setField(brochure, "sitioUrl", "https://www.arquinova.test/");
        try {
            BrochureService.Brochure b = brochure.generar(p.getId());

            assertEquals("%PDF", new String(b.pdf(), 0, 4, StandardCharsets.US_ASCII));
            assertTrue(b.nombreArchivo().startsWith("Brochure-prueba-") && b.nombreArchivo().endsWith(".pdf"), b.nombreArchivo());
            assertEquals(p.getId(), b.proyectoId());

            String t = texto(b.pdf());
            assertTrue(t.contains(nombre), "el nombre del proyecto");
            assertTrue(t.contains("Grupo Prueba"), "el nombre de la empresa en el encabezado");
            assertTrue(t.contains("Diseño sostenible en el Quindío"), "tildes y ñ correctas en la descripción");
            assertTrue(t.contains("En construcción") && t.contains("Residencial"), "etapa y tipo legibles");
            assertTrue(t.contains("Vereda La Esperanza, Armenia, Quindío"), "ubicación");
            assertTrue(t.contains("Villa Samán") && t.contains("180,5") && t.contains("Baños"), "villas con m², habitaciones y baños");
            assertTrue(t.contains("Piscina") && t.contains("Jacuzzi"), "amenidades");
            assertTrue(t.contains("Lotes disponibles") && t.contains("Etapa 1"), "resumen de lotes");
            assertTrue(t.contains("1.000 a 1.500,5") || t.contains("1.000 a 1.500,50"), "rango de área de los disponibles: " + t);
            assertTrue(t.contains("+57 300 000 0000") && t.contains("comercial@prueba.test") && t.contains("https://prueba.test")
                    && t.contains("Calle 1 # 2-3, Armenia"), "datos de contacto");
            assertTrue(t.contains("https://www.arquinova.test/proyectos/" + p.getId()), "URL del proyecto");
            assertTrue(t.contains("Información sujeta a cambios; los valores y la disponibilidad los confirma el equipo comercial"), "aviso del pie");
            assertFalse(t.contains("$") || t.toLowerCase().contains("precio:"), "nunca precios");
            assertFalse(t.contains("caso de portafolio"));
        } finally {
            ReflectionTestUtils.setField(brochure, "sitioUrl", anterior);
        }
    }

    @Test
    @DisplayName("Un caso de portafolio se presenta como tal, no como oferta de venta, y no lista lotes")
    void brochureDePortafolio() throws Exception {
        Proyecto p = proyectoConDatos("Casa Referencia " + FixturasContenido.sufijo(), TipoRegistroProyecto.PORTAFOLIO);

        String t = texto(brochure.generar(p.getId()).pdf());

        assertTrue(t.contains("Caso de portafolio"));
        assertTrue(t.contains("No es una oferta de venta"));
        assertFalse(t.contains("Lotes disponibles"));
        assertTrue(t.contains("Información sujeta a cambios"));
    }

    @Test
    @DisplayName("Solo proyectos publicados y activos: sin publicar, inactivo o inexistente es 404")
    void soloPublicados() {
        Empresa empresa = FixturasContenido.empresa(em);
        Proyecto sinPublicar = FixturasContenido.proyecto(em, empresa, "Borrador " + FixturasContenido.sufijo(), false, true, TipoRegistroProyecto.OFERTA_COMERCIAL);
        Proyecto inactivo = FixturasContenido.proyecto(em, empresa, "Inactivo " + FixturasContenido.sufijo(), true, false, TipoRegistroProyecto.OFERTA_COMERCIAL);

        assertThrows(ResourceNotFoundException.class, () -> brochure.generar(sinPublicar.getId()));
        assertThrows(ResourceNotFoundException.class, () -> brochure.generar(inactivo.getId()));
        assertThrows(ResourceNotFoundException.class, () -> brochure.generar(987654321L));
    }

    @Test
    @DisplayName("Un proyecto casi vacío (sin ubicación, villas, amenidades ni lotes) también genera un PDF válido")
    void proyectoVacio() throws Exception {
        Empresa empresa = FixturasContenido.empresa(em);
        Proyecto p = FixturasContenido.proyecto(em, empresa, "Solo Nombre " + FixturasContenido.sufijo(), true, true, TipoRegistroProyecto.OFERTA_COMERCIAL);
        em.clear();

        String t = texto(brochure.generar(p.getId()).pdf());

        assertTrue(t.contains("Solo Nombre"));
        assertFalse(t.contains("Lotes disponibles"));
        assertFalse(t.contains("Amenidades"));
    }

    @Test
    @DisplayName("El nombre del archivo sale del slug y siempre es seguro")
    void nombreDeArchivo() {
        Proyecto p = new Proyecto();
        p.setId(6L);
        p.setSlug("El Encanto/../Ñandú \"x\"");
        assertEquals("Brochure-el-encanto-and-x.pdf", BrochureService.nombreArchivo(p));
        p.setSlug(null);
        assertEquals("Brochure-proyecto-6.pdf", BrochureService.nombreArchivo(p));
        p.setSlug("el-encanto");
        assertEquals("Brochure-el-encanto.pdf", BrochureService.nombreArchivo(p));
    }
}
