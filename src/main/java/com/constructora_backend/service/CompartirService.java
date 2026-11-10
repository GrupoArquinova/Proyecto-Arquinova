package com.constructora_backend.service;

import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.repository.ProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Página mínima con las etiquetas para compartir (Open Graph y X/Twitter) de un proyecto.
 * <p>
 * WhatsApp, Facebook, LinkedIn y los demás rastreadores de vista previa no ejecutan JavaScript: del sitio (una aplicación de
 * una sola página) solo ven el HTML inicial, igual para todas las direcciones. Vercel les entrega esta página en lugar del
 * HTML inicial cuando piden /proyectos/{id}, así el enlace de un proyecto se ve con su nombre, descripción y portada. Las
 * personas nunca la reciben.
 */
@Service
public class CompartirService {

    /** El logo en 1200×630 sobre fondo blanco: la misma imagen por defecto que lleva el index.html del sitio. */
    static final String IMAGEN_POR_DEFECTO =
            "https://res.cloudinary.com/dn3s7utod/image/upload/c_pad,w_1200,h_630,b_rgb:FFFFFF,f_jpg,q_auto/v1790258444/logo-Arquinova_dgkosg.jpg";
    static final String NOMBRE_SITIO = "Grupo Arquinova";
    static final String DESCRIPCION_POR_DEFECTO =
            "Grupo Arquinova S.A.S.: arquitectura, ingeniería y gestión de proyectos residenciales, rurales y turísticos en el Quindío.";
    private static final int MAX_DESCRIPCION = 160;
    /** Una imagen de Cloudinary sin transformaciones: se recorta a la proporción 1,91:1 que piden las redes. */
    private static final Pattern CLOUDINARY = Pattern.compile("^(https://res\\.cloudinary\\.com/[^/]+/image/upload/)(v\\d+/.+)$");

    private final ProyectoRepository proyectoRepository;

    @Value("${app.sitio-url:}")
    private String sitioUrl = "";

    @Autowired
    public CompartirService(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    /** El HTML del proyecto, o vacío si no existe o no está publicado y activo (no se anuncia lo que el sitio no muestra). */
    @Transactional(readOnly = true)
    public Optional<String> paginaDelProyecto(Long id) {
        return proyectoRepository.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.getPublicado()) && !Boolean.FALSE.equals(p.getActivo()))
                .map(this::armar);
    }

    private String armar(Proyecto proyecto) {
        String nombre = textoLimpio(proyecto.getNombre());
        String titulo = nombre.isEmpty() ? NOMBRE_SITIO : nombre + " | " + NOMBRE_SITIO;
        String descripcion = recortar(textoLimpio(proyecto.getDescripcion()));
        if (descripcion.isEmpty()) {
            descripcion = DESCRIPCION_POR_DEFECTO;
        }
        String imagen = imagenParaCompartir(proyecto.getImagenUrl());
        String direccion = baseDelSitio().isEmpty() ? null : baseDelSitio() + "/proyectos/" + proyecto.getId();

        StringBuilder html = new StringBuilder(1400);
        html.append("<!doctype html>\n<html lang=\"es\">\n<head>\n<meta charset=\"utf-8\">\n");
        html.append("<title>").append(esc(titulo)).append("</title>\n");
        meta(html, "name", "description", descripcion);
        meta(html, "property", "og:site_name", NOMBRE_SITIO);
        meta(html, "property", "og:locale", "es_CO");
        meta(html, "property", "og:type", "website");
        meta(html, "property", "og:title", titulo);
        meta(html, "property", "og:description", descripcion);
        meta(html, "property", "og:image", imagen);
        meta(html, "property", "og:image:alt", nombre.isEmpty() ? NOMBRE_SITIO : nombre);
        if (direccion != null) {
            meta(html, "property", "og:url", direccion);
            html.append("<link rel=\"canonical\" href=\"").append(esc(direccion)).append("\">\n");
        }
        meta(html, "name", "twitter:card", "summary_large_image");
        meta(html, "name", "twitter:title", titulo);
        meta(html, "name", "twitter:description", descripcion);
        meta(html, "name", "twitter:image", imagen);
        html.append("</head>\n<body>\n<h1>").append(esc(titulo)).append("</h1>\n<p>").append(esc(descripcion)).append("</p>\n");
        if (direccion != null) {
            html.append("<p><a href=\"").append(esc(direccion)).append("\">").append(esc(NOMBRE_SITIO)).append("</a></p>\n");
        }
        html.append("</body>\n</html>\n");
        return html.toString();
    }

    private static void meta(StringBuilder html, String tipo, String nombre, String contenido) {
        html.append("<meta ").append(tipo).append("=\"").append(nombre).append("\" content=\"").append(esc(contenido)).append("\">\n");
    }

    /** Portada del proyecto recortada a 1200×630 si es de Cloudinary; otra dirección https se usa tal cual; si no, la imagen de la marca. */
    static String imagenParaCompartir(String url) {
        if (url == null || url.isBlank()) {
            return IMAGEN_POR_DEFECTO;
        }
        String limpia = url.trim();
        Matcher m = CLOUDINARY.matcher(limpia);
        if (m.matches()) {
            return m.group(1) + "c_fill,g_auto,w_1200,h_630,f_jpg,q_auto/" + m.group(2);
        }
        return limpia.startsWith("https://") ? limpia : IMAGEN_POR_DEFECTO;
    }

    /** Una sola línea, sin saltos ni espacios repetidos. */
    private static String textoLimpio(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }

    /** Máximo {@value #MAX_DESCRIPCION} letras, cortado en una palabra completa. */
    static String recortar(String texto) {
        if (texto.length() <= MAX_DESCRIPCION) {
            return texto;
        }
        String corte = texto.substring(0, MAX_DESCRIPCION - 1);
        int espacio = corte.lastIndexOf(' ');
        String base = espacio > MAX_DESCRIPCION * 0.6 ? corte.substring(0, espacio) : corte;
        return base.replaceAll("[\\s.,;:]+$", "") + "…";
    }

    private String baseDelSitio() {
        String base = sitioUrl == null ? "" : sitioUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private static String esc(String texto) {
        return SitemapService.escapar(texto);
    }
}
