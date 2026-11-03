package com.constructora_backend.service;

import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.repository.ProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Sitemap XML estándar (https://www.sitemaps.org) con la página de inicio, el listado de proyectos y cada proyecto publicado y
 * activo, usando como base la dirección pública del sitio (app.sitio-url / APP_SITIO_URL).
 * <p>
 * Si app.sitio-url no está configurada NO hay sitemap (el controlador responde 404): un sitemap con direcciones relativas o
 * inventadas sería inválido y los buscadores lo rechazarían sin avisar. Un sitemap vacío válido ocultaría el problema de
 * configuración; el 404 lo hace visible.
 */
@Service
public class SitemapService {

    private final ProyectoRepository proyectoRepository;

    @Value("${app.sitio-url:}")
    private String sitioUrl = "";

    @Autowired
    public SitemapService(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    /** El XML del sitemap, o vacío si no hay dirección pública configurada. */
    @Transactional(readOnly = true)
    public Optional<String> generar() {
        String base = baseDelSitio();
        if (base.isEmpty()) {
            return Optional.empty();
        }
        List<Proyecto> proyectos = proyectoRepository.findByPublicadoTrueAndActivoTrue();
        // La portada y el listado cambian cuando cambia cualquier proyecto: toman la fecha más reciente
        LocalDate ultima = proyectos.stream().map(SitemapService::modificadoEn).filter(f -> f != null)
                .max(LocalDate::compareTo).orElse(null);

        StringBuilder xml = new StringBuilder(512 + proyectos.size() * 160);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        agregar(xml, base + "/", ultima);
        agregar(xml, base + "/proyectos", ultima);
        for (Proyecto proyecto : proyectos) {
            agregar(xml, base + "/proyectos/" + proyecto.getId(), modificadoEn(proyecto));
        }
        xml.append("</urlset>\n");
        return Optional.of(xml.toString());
    }

    private String baseDelSitio() {
        String base = sitioUrl == null ? "" : sitioUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private static LocalDate modificadoEn(Proyecto proyecto) {
        LocalDateTime fecha = proyecto.getActualizadoEn() != null ? proyecto.getActualizadoEn() : proyecto.getCreadoEn();
        return fecha == null ? null : fecha.toLocalDate();
    }

    private static void agregar(StringBuilder xml, String direccion, LocalDate modificado) {
        xml.append("  <url>\n    <loc>").append(escapar(direccion)).append("</loc>\n");
        if (modificado != null) {
            xml.append("    <lastmod>").append(modificado).append("</lastmod>\n");
        }
        xml.append("  </url>\n");
    }

    static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
