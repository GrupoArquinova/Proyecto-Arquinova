package com.constructora_backend.service;

import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.enums.EstadoProyecto;
import com.constructora_backend.enums.TipoRegistroProyecto;
import com.constructora_backend.repository.CasaModeloRepository;
import com.constructora_backend.repository.EmpresaRepository;
import com.constructora_backend.repository.EtapaRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.UbicacionRepository;
import com.constructora_backend.repository.ZonaComunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Arma el texto con los datos reales y públicos del sitio (empresa; proyectos publicados con tipo de registro, enlace, fases, villas,
 * amenidades y ubicación; y lotes con conteo por estado y detalle de los disponibles) que se le da al asistente de IA. No incluye precios porque el sistema no los maneja. El texto se
 * guarda unos minutos para no consultar la base en cada pregunta.
 */
@Service
@RequiredArgsConstructor
public class ChatContextoService {

    static final Duration VIGENCIA = Duration.ofMinutes(10);
    private static final int MAX_PROYECTOS = 15;
    /** Tope del texto completo (caracteres): lo que no cabe se omite para no inflar cada consulta a la IA. */
    static final int MAX_CONTEXTO = 12_000;
    static final int MAX_LOTES_DETALLE = 15;
    private static final int MAX_EMPRESA = 600;
    private static final int MAX_CORTO = 100;
    private static final int MAX_DESCRIPCION = 450;

    private final ProyectoRepository proyectoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final CasaModeloRepository casaModeloRepository;
    private final ZonaComunRepository zonaComunRepository;
    private final LoteRepository loteRepository;
    private final EmpresaRepository empresaRepository;
    private final EtapaRepository etapaRepository;

    @Value("${app.contacto.whatsapp:}")
    private String whatsappEmpresa;

    @Value("${app.sitio-url:}")
    private String sitioUrl;

    private volatile String guardado;
    private volatile Instant guardadoEn = Instant.EPOCH;

    /**
     * La transacción va aquí, en el método que se llama desde fuera: la de {@link #construir()} no se aplica porque se invoca
     * desde esta misma clase, y sin transacción las relaciones perezosas (lote → etapa → proyecto) fallan con LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public String obtener() {
        return obtener(Instant.now());
    }

    String obtener(Instant ahora) {
        String actual = guardado;
        if (actual != null && Duration.between(guardadoEn, ahora).compareTo(VIGENCIA) < 0) {
            return actual;
        }
        String nuevo = construir();
        guardado = nuevo;
        guardadoEn = ahora;
        return nuevo;
    }

    @Transactional(readOnly = true)
    String construir() {
        StringBuilder t = new StringBuilder();
        t.append("Empresa: Grupo Arquinova S.A.S. (NIT 901.397.504-2).\n");
        agregarEmpresa(t);

        // Lotes publicados agrupados por proyecto
        Map<Long, List<Lote>> lotesPorProyecto = loteRepository.findByPublicadoTrueAndActivoTrue().stream()
                .filter(l -> l.getEtapa() != null && l.getEtapa().getProyecto() != null && l.getEstado() != null)
                .collect(Collectors.groupingBy(l -> l.getEtapa().getProyecto().getId()));

        List<Proyecto> proyectos = proyectoRepository.findByPublicadoTrueAndActivoTrue().stream()
                .sorted(Comparator.comparing((Proyecto p) -> !Boolean.TRUE.equals(p.getDestacado())).thenComparing(Proyecto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_PROYECTOS)
                .toList();

        if (proyectos.isEmpty()) {
            t.append("\nPor ahora no hay proyectos publicados en el sitio.\n");
            return t.toString();
        }

        int omitidos = 0;
        for (Proyecto p : proyectos) {
            String bloque = describirProyecto(p, lotesPorProyecto.getOrDefault(p.getId(), List.of()));
            // Control del tamaño total: lo que no cabe se omite y se avisa
            if (t.length() + bloque.length() > MAX_CONTEXTO) {
                omitidos++;
                continue;
            }
            t.append(bloque);
        }
        if (omitidos > 0) {
            t.append("\n(Hay ").append(omitidos).append(" proyecto(s) más en el sitio que no se detallan aquí; el equipo puede dar más información.)\n");
        }
        return t.toString();
    }

    /**
     * Datos de la primera empresa activa. A propósito NO se incluye {@code trayectoria} (puede traer afirmaciones sin soporte
     * confirmado, como años de experiencia) ni los contenidos institucionales (misión, visión, valores): siguen pendientes de
     * aprobación de la empresa y el asistente no debe afirmarlos. Cuando se aprueben se pueden agregar aquí.
     */
    private void agregarEmpresa(StringBuilder t) {
        Empresa e = empresaRepository.findByActivoTrue().stream().findFirst().orElse(null);
        String whatsapp = e != null && tiene(e.getWhatsapp()) ? e.getWhatsapp() : whatsappEmpresa;
        String sitio = e != null && tiene(e.getSitioWeb()) ? e.getSitioWeb() : sitioUrl;
        if (e != null) {
            if (tiene(e.getNombre())) t.append("Nombre comercial: ").append(e.getNombre().trim()).append("\n");
            if (tiene(e.getDescripcion())) t.append("Sobre la empresa: ").append(recortar(e.getDescripcion(), MAX_EMPRESA)).append("\n");
            if (tiene(e.getServicios())) t.append("Servicios que ofrece: ").append(recortar(e.getServicios(), MAX_EMPRESA)).append("\n");
            if (tiene(e.getDireccion())) t.append("Oficina de atención: ").append(recortar(e.getDireccion(), 200)).append("\n");
            if (tiene(e.getCorreoComercial())) t.append("Correo comercial: ").append(e.getCorreoComercial().trim()).append("\n");
            if (tiene(e.getTelefono())) t.append("Teléfono: ").append(e.getTelefono().trim()).append("\n");
        }
        if (tiene(whatsapp)) t.append("WhatsApp del equipo: ").append(whatsapp.trim()).append("\n");
        if (tiene(sitio)) t.append("Sitio web: ").append(sitio.trim()).append("\n");
    }

    private String describirProyecto(Proyecto p, List<Lote> lotes) {
        StringBuilder t = new StringBuilder();
        t.append("\n== Proyecto: ").append(p.getNombre());
        if (Boolean.TRUE.equals(p.getDestacado())) t.append(" (proyecto destacado)");
        t.append(" ==\n");

        boolean portafolio = p.getTipoRegistro() == TipoRegistroProyecto.PORTAFOLIO;
        t.append("Tipo de registro: ").append(portafolio
                ? "Caso de portafolio: Arquinova participó con estudios, diseño o gestión; no es una oferta de venta"
                : "Oferta comercial: tiene unidades a la venta").append("\n");
        if (tiene(p.getParticipacion())) t.append("Participación de Arquinova: ").append(recortar(p.getParticipacion(), 255)).append("\n");
        if (p.getTipoProyecto() != null) t.append("Tipo de proyecto: ").append(p.getTipoProyecto().name().toLowerCase()).append("\n");
        if (tiene(sitioUrl)) t.append("Enlace: ").append(sitioUrl.trim().replaceAll("/+$", "")).append("/proyectos/").append(p.getId()).append("\n");
        if (p.getEstadoProyecto() != null) t.append("Etapa del proyecto: ").append(describirEtapa(p.getEstadoProyecto())).append("\n");
        if (tiene(p.getDescripcion())) t.append("Descripción: ").append(recortar(p.getDescripcion())).append("\n");

        ubicacionRepository.findByProyectoId(p.getId()).ifPresent(u -> t.append("Ubicación: ").append(describirUbicacion(u)).append("\n"));

        List<Etapa> etapas = etapaRepository.findByProyectoIdAndActivoTrueOrderByOrdenAsc(p.getId());
        if (!etapas.isEmpty()) {
            t.append("Fases del proyecto: ").append(etapas.stream().map(ChatContextoService::describirFase).collect(Collectors.joining("; "))).append("\n");
        }

        List<CasaModelo> villas = casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(p.getId());
        for (CasaModelo c : villas) {
            t.append("Tipología/villa ").append(c.getNombre()).append(": ").append(describirVilla(c)).append("\n");
        }

        List<ZonaComun> zonas = zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(p.getId());
        if (!zonas.isEmpty()) {
            t.append("Amenidades: ").append(zonas.stream().map(ChatContextoService::describirAmenidad).collect(Collectors.joining("; "))).append("\n");
        }

        if (!lotes.isEmpty()) {
            agregarLotes(t, lotes);
        }
        return t.toString();
    }

    private static String describirFase(Etapa e) {
        return tiene(e.getDescripcion()) ? e.getNombre() + " (" + recortar(e.getDescripcion(), MAX_CORTO) + ")" : e.getNombre();
    }

    private static String describirAmenidad(ZonaComun z) {
        return tiene(z.getDescripcion()) ? z.getNombre() + " (" + recortar(z.getDescripcion(), MAX_CORTO) + ")" : z.getNombre();
    }

    /** Conteos por estado, rango de áreas y detalle (código, nombre, área) de los lotes disponibles, hasta {@link #MAX_LOTES_DETALLE}. */
    private static void agregarLotes(StringBuilder t, List<Lote> lotes) {
        Map<String, Long> porEstado = lotes.stream()
                .collect(Collectors.groupingBy(l -> l.getEstado().getNombre(), TreeMap::new, Collectors.counting()));
        t.append("Lotes por estado: ").append(porEstado.entrySet().stream().map(e -> e.getValue() + " " + e.getKey().toLowerCase()).collect(Collectors.joining(", ")))
                .append(" (la disponibilidad cambia; confirmar siempre con el equipo)\n");

        List<BigDecimal> areas = lotes.stream().map(Lote::getAreaM2).filter(Objects::nonNull).sorted().toList();
        if (!areas.isEmpty()) {
            t.append("Áreas de los lotes: de ").append(m2(areas.get(0))).append(" a ").append(m2(areas.get(areas.size() - 1))).append("\n");
        }

        List<Lote> disponibles = lotes.stream()
                .filter(l -> "disponible".equalsIgnoreCase(l.getEstado().getNombre()))
                .sorted(Comparator.comparing(Lote::getCodigo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        if (!disponibles.isEmpty()) {
            String detalle = disponibles.stream().limit(MAX_LOTES_DETALLE).map(ChatContextoService::describirLote).collect(Collectors.joining("; "));
            t.append("Lotes disponibles: ").append(detalle);
            if (disponibles.size() > MAX_LOTES_DETALLE) t.append("; y ").append(disponibles.size() - MAX_LOTES_DETALLE).append(" más");
            t.append("\n");
        }
    }

    private static String describirLote(Lote l) {
        StringBuilder s = new StringBuilder(l.getCodigo() == null ? "s/c" : l.getCodigo());
        if (tiene(l.getNombre())) s.append(" (").append(l.getNombre().trim()).append(")");
        if (l.getAreaM2() != null) s.append(" ").append(m2(l.getAreaM2()));
        return s.toString();
    }

    private static String m2(BigDecimal area) {
        return area.stripTrailingZeros().toPlainString() + " m²";
    }

    private static boolean tiene(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static String describirEtapa(EstadoProyecto estado) {
        return switch (estado) {
            case EN_DISENO -> "en diseño";
            case EN_TRAMITE -> "en trámite";
            case EN_CONSTRUCCION -> "en construcción";
            case FINALIZADO -> "finalizado";
        };
    }

    private static String describirUbicacion(Ubicacion u) {
        StringBuilder s = new StringBuilder();
        if (u.getCiudad() != null) s.append(u.getCiudad());
        if (u.getDepartamento() != null) s.append(s.length() > 0 ? ", " : "").append(u.getDepartamento());
        if (u.getReferencias() != null && !u.getReferencias().isBlank()) s.append(". ").append(recortar(u.getReferencias()));
        return s.toString();
    }

    private static String describirVilla(CasaModelo c) {
        StringBuilder s = new StringBuilder();
        if (c.getAreaConstruidaM2() != null) s.append(c.getAreaConstruidaM2().stripTrailingZeros().toPlainString()).append(" m² construidos");
        if (c.getNumeroHabitaciones() != null) s.append(s.length() > 0 ? ", " : "").append(c.getNumeroHabitaciones()).append(" habitaciones");
        if (c.getNumeroBanos() != null) s.append(s.length() > 0 ? ", " : "").append(c.getNumeroBanos()).append(" baños");
        // Solo se dice si existen; nunca se incluyen las direcciones de los archivos
        s.append(s.length() > 0 ? ". " : "").append(tiene(c.getTourVirtualUrl()) ? "tiene tour virtual" : "sin tour virtual")
                .append(", ").append(tiene(c.getPlanoUrl()) ? "tiene planos" : "sin planos");
        if (c.getDescripcion() != null && !c.getDescripcion().isBlank()) s.append(". ").append(recortar(c.getDescripcion()));
        return s.toString();
    }

    private static String recortar(String texto) {
        return recortar(texto, MAX_DESCRIPCION);
    }

    private static String recortar(String texto, int maximo) {
        String limpio = texto.trim().replaceAll("\\s+", " ");
        return limpio.length() > maximo ? limpio.substring(0, maximo).trim() + "…" : limpio;
    }
}
