package com.constructora_backend.service;

import com.constructora_backend.documento.Columna;
import com.constructora_backend.documento.Etiquetas;
import com.constructora_backend.documento.PdfReporte;
import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.enums.TipoRegistroProyecto;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.repository.CasaModeloRepository;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.UbicacionRepository;
import com.constructora_backend.repository.ZonaComunRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Brochure PDF de un proyecto, armado al momento con los datos publicados (no hay archivos subidos que mantener).
 * <ul>
 *   <li>Solo proyectos publicados y activos; cualquier otro caso es 404 (no se revela si existe).</li>
 *   <li>NUNCA lleva precios: los valores y la disponibilidad los confirma el equipo comercial.</li>
 *   <li>Un proyecto de PORTAFOLIO se presenta como caso realizado, no como oferta de venta, y no lista lotes.</li>
 *   <li>No carga nada de internet: solo texto, tablas y el logo que viene dentro de la aplicación.</li>
 * </ul>
 */
@Service
public class BrochureService {

    /** Aviso del pie de todas las hojas. */
    public static final String AVISO = "Información sujeta a cambios; los valores y la disponibilidad los confirma el equipo comercial.";

    private static final Locale CO = Locale.forLanguageTag("es-CO");
    private static final int MAX_DESCRIPCION = 1500;
    private static final int MAX_TEXTO_CORTO = 400;

    @PersistenceContext
    private EntityManager em;

    private final ProyectoRepository proyectoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final CasaModeloRepository casaModeloRepository;
    private final ZonaComunRepository zonaComunRepository;

    /** Dirección pública del sitio (APP_SITIO_URL); sin ella el brochure no lleva el enlace al proyecto. */
    @Value("${app.sitio-url:}")
    private String sitioUrl = "";

    /** WhatsApp de la empresa por defecto, si la empresa no tiene uno propio. */
    @Value("${app.contacto.whatsapp:}")
    private String whatsappPorDefecto = "";

    @Autowired
    public BrochureService(ProyectoRepository proyectoRepository,
                           UbicacionRepository ubicacionRepository,
                           CasaModeloRepository casaModeloRepository,
                           ZonaComunRepository zonaComunRepository) {
        this.proyectoRepository = proyectoRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.casaModeloRepository = casaModeloRepository;
        this.zonaComunRepository = zonaComunRepository;
    }

    /** El PDF y el nombre sugerido para el archivo. */
    public record Brochure(byte[] pdf, String nombreArchivo, Long proyectoId) {
    }

    /** Fila de la tabla de disponibilidad de lotes (sin precios). */
    private record ResumenEtapa(String etapa, long disponibles, long total, String areas) {
    }

    @Transactional(readOnly = true)
    public Brochure generar(Long proyectoId) throws IOException {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .filter(Proyecto::visibleAlPublico)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + proyectoId));
        Empresa empresa = proyecto.getEmpresa();
        String nombreEmpresa = empresa != null && empresa.getNombre() != null ? empresa.getNombre() : "Arquinova";
        boolean portafolio = proyecto.getTipoRegistro() == TipoRegistroProyecto.PORTAFOLIO;

        Ubicacion ubicacion = ubicacionRepository.findByProyectoId(proyectoId).orElse(null);
        List<CasaModelo> villas = casaModeloRepository.findVisiblesAlPublicoPorProyecto(proyectoId);
        List<ZonaComun> zonas = zonaComunRepository.findVisiblesAlPublicoPorProyecto(proyectoId);
        List<ResumenEtapa> lotes = portafolio ? List.of() : resumenDeLotes(proyectoId);

        try (PdfReporte pdf = new PdfReporte(false, nombreEmpresa)) {
            pdf.avisoPie(AVISO);
            pdf.portada(proyecto.getNombre(), nombreEmpresa + "  ·  Brochure del proyecto");

            if (portafolio) {
                pdf.seccion("Caso de portafolio");
                String participacion = proyecto.getParticipacion() == null || proyecto.getParticipacion().isBlank() ? ""
                        : " Participación de " + nombreEmpresa + ": " + proyecto.getParticipacion().trim() + ".";
                pdf.parrafo("Este proyecto es un caso de portafolio: un trabajo realizado por " + nombreEmpresa
                        + " que se presenta como referencia de su experiencia. No es una oferta de venta." + participacion, 10);
            }

            Map<String, String> datos = new LinkedHashMap<>();
            datos.put("Tipo de proyecto", proyecto.getTipoProyecto() == null ? "" : Etiquetas.legible(proyecto.getTipoProyecto().name()));
            datos.put("Etapa", proyecto.getEstadoProyecto() == null ? "" : Etiquetas.legible(proyecto.getEstadoProyecto().name()));
            datos.put("Ubicación", ubicacion == null ? "" : lugar(ubicacion));
            datos.put("Referencias", ubicacion == null ? "" : recortar(ubicacion.getReferencias(), MAX_TEXTO_CORTO));
            pdf.seccion("Datos del proyecto");
            pdf.clavesValor(datos);

            if (proyecto.getDescripcion() != null && !proyecto.getDescripcion().isBlank()) {
                pdf.seccion("Descripción");
                pdf.parrafo(recortar(proyecto.getDescripcion(), MAX_DESCRIPCION), 10);
            }

            if (!villas.isEmpty()) {
                pdf.seccion("Villas y tipologías");
                pdf.tabla(List.of(
                        Columna.<CasaModelo>texto("Tipología", 30, CasaModelo::getNombre),
                        Columna.<CasaModelo>decimal("Área construida (m²)", 22, CasaModelo::getAreaConstruidaM2, false),
                        Columna.<CasaModelo>entero("Habitaciones", 16, CasaModelo::getNumeroHabitaciones, false),
                        Columna.<CasaModelo>entero("Baños", 12, CasaModelo::getNumeroBanos, false)), villas);
            }

            if (!zonas.isEmpty()) {
                pdf.seccion("Amenidades");
                List<String> nombres = new ArrayList<>();
                for (ZonaComun zona : zonas) {
                    if (zona.getNombre() != null && !zona.getNombre().isBlank()) nombres.add(zona.getNombre().trim());
                }
                pdf.parrafo(String.join("  ·  ", nombres), 10);
            }

            if (!lotes.isEmpty()) {
                pdf.seccion("Lotes disponibles");
                pdf.tabla(List.of(
                        Columna.<ResumenEtapa>texto("Etapa", 26, ResumenEtapa::etapa),
                        Columna.<ResumenEtapa>entero("Disponibles", 14, ResumenEtapa::disponibles, true),
                        Columna.<ResumenEtapa>entero("Total de lotes", 14, ResumenEtapa::total, true),
                        Columna.<ResumenEtapa>texto("Áreas disponibles (m²)", 22, ResumenEtapa::areas)), lotes);
            }

            pdf.seccion("Contacto");
            Map<String, String> contacto = new LinkedHashMap<>();
            String whatsapp = empresa != null && notBlank(empresa.getWhatsapp()) ? empresa.getWhatsapp() : whatsappPorDefecto;
            contacto.put("WhatsApp", whatsapp);
            contacto.put("Correo", empresa == null ? "" : empresa.getCorreoComercial());
            String sitio = empresa != null && notBlank(empresa.getSitioWeb()) ? empresa.getSitioWeb() : sitioUrl;
            contacto.put("Sitio web", sitio);
            contacto.put("Oficina", empresa == null ? "" : empresa.getDireccion());
            contacto.put("Este proyecto en línea", enlaceDelProyecto(proyecto.getId()));
            pdf.clavesValor(contacto);

            return new Brochure(pdf.bytes(), nombreArchivo(proyecto), proyecto.getId());
        }
    }

    /** "Brochure-el-encanto.pdf": el slug del proyecto, reducido a letras minúsculas, números y guiones. */
    static String nombreArchivo(Proyecto proyecto) {
        String base = proyecto.getSlug() == null ? "" : proyecto.getSlug().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) {
            base = "proyecto-" + proyecto.getId();
        }
        return "Brochure-" + base + ".pdf";
    }

    private String enlaceDelProyecto(Long id) {
        if (sitioUrl == null || sitioUrl.isBlank()) {
            return "";
        }
        String base = sitioUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/proyectos/" + id;
    }

    private static String lugar(Ubicacion u) {
        List<String> partes = new ArrayList<>();
        for (String parte : new String[]{u.getDireccion(), u.getCiudad(), u.getDepartamento()}) {
            if (notBlank(parte)) partes.add(parte.trim());
        }
        return String.join(", ", partes);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String recortar(String texto, int max) {
        if (texto == null) return "";
        String t = texto.trim();
        return t.length() > max ? t.substring(0, max) + "..." : t;
    }

    /** Lotes publicados y activos agrupados por etapa: cuántos hay, cuántos disponibles y en qué rango de área. Una sola consulta agregada. */
    private List<ResumenEtapa> resumenDeLotes(Long proyectoId) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createQuery(
                        "select et.id, et.nombre, e.nombre, count(l), min(l.areaM2), max(l.areaM2) "
                                + "from Lote l join l.etapa et join l.estado e "
                                + "where et.proyecto.id = :proyectoId and et.activo = true and l.activo = true and l.publicado = true "
                                + "group by et.id, et.nombre, et.orden, e.id, e.nombre order by et.orden, et.id")
                .setParameter("proyectoId", proyectoId).getResultList();

        Map<Long, String> nombres = new LinkedHashMap<>();
        Map<Long, long[]> cifras = new LinkedHashMap<>(); // [disponibles, total]
        Map<Long, BigDecimal[]> areas = new LinkedHashMap<>(); // [min, max] de los disponibles
        for (Object[] fila : filas) {
            Long etapaId = ((Number) fila[0]).longValue();
            nombres.put(etapaId, (String) fila[1]);
            long cantidad = ((Number) fila[3]).longValue();
            long[] c = cifras.computeIfAbsent(etapaId, k -> new long[2]);
            c[1] += cantidad;
            if ("DISPONIBLE".equalsIgnoreCase((String) fila[2])) {
                c[0] += cantidad;
                BigDecimal[] rango = areas.computeIfAbsent(etapaId, k -> new BigDecimal[]{null, null});
                BigDecimal minimo = (BigDecimal) fila[4];
                BigDecimal maximo = (BigDecimal) fila[5];
                if (minimo != null && (rango[0] == null || minimo.compareTo(rango[0]) < 0)) rango[0] = minimo;
                if (maximo != null && (rango[1] == null || maximo.compareTo(rango[1]) > 0)) rango[1] = maximo;
            }
        }
        List<ResumenEtapa> resumen = new ArrayList<>();
        cifras.forEach((id, c) -> resumen.add(new ResumenEtapa(nombres.get(id), c[0], c[1], rango(areas.get(id)))));
        return resumen;
    }

    private static String rango(BigDecimal[] area) {
        if (area == null || area[0] == null || area[1] == null) return "";
        NumberFormat nf = NumberFormat.getNumberInstance(CO);
        nf.setMaximumFractionDigits(2);
        return area[0].compareTo(area[1]) == 0 ? nf.format(area[0]) : nf.format(area[0]) + " a " + nf.format(area[1]);
    }
}
