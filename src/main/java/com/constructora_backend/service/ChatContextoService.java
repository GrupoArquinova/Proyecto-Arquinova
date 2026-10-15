package com.constructora_backend.service;

import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.enums.EstadoProyecto;
import com.constructora_backend.repository.CasaModeloRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.UbicacionRepository;
import com.constructora_backend.repository.ZonaComunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Arma el texto con los datos reales y públicos del sitio (proyectos publicados, villas, amenidades, ubicación y cuántos
 * lotes hay por estado) que se le da al asistente de IA. No incluye precios porque el sistema no los maneja. El texto se
 * guarda unos minutos para no consultar la base en cada pregunta.
 */
@Service
@RequiredArgsConstructor
public class ChatContextoService {

    static final Duration VIGENCIA = Duration.ofMinutes(10);
    private static final int MAX_PROYECTOS = 12;
    private static final int MAX_DESCRIPCION = 450;

    private final ProyectoRepository proyectoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final CasaModeloRepository casaModeloRepository;
    private final ZonaComunRepository zonaComunRepository;
    private final LoteRepository loteRepository;

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
        if (whatsappEmpresa != null && !whatsappEmpresa.isBlank()) t.append("WhatsApp del equipo: ").append(whatsappEmpresa.trim()).append("\n");
        if (sitioUrl != null && !sitioUrl.isBlank()) t.append("Sitio web: ").append(sitioUrl.trim()).append("\n");

        // Lotes publicados agrupados por proyecto y estado
        Map<Long, Map<String, Long>> lotesPorProyecto = loteRepository.findByPublicadoTrueAndActivoTrue().stream()
                .filter(l -> l.getEtapa() != null && l.getEtapa().getProyecto() != null && l.getEstado() != null)
                .collect(Collectors.groupingBy(l -> l.getEtapa().getProyecto().getId(),
                        Collectors.groupingBy(l -> l.getEstado().getNombre(), TreeMap::new, Collectors.counting())));

        List<Proyecto> proyectos = proyectoRepository.findByPublicadoTrueAndActivoTrue().stream()
                .sorted(Comparator.comparing((Proyecto p) -> !Boolean.TRUE.equals(p.getDestacado())).thenComparing(Proyecto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_PROYECTOS)
                .toList();

        if (proyectos.isEmpty()) {
            t.append("\nPor ahora no hay proyectos publicados en el sitio.\n");
            return t.toString();
        }

        for (Proyecto p : proyectos) {
            t.append("\n== Proyecto: ").append(p.getNombre());
            if (Boolean.TRUE.equals(p.getDestacado())) t.append(" (proyecto destacado)");
            t.append(" ==\n");
            if (p.getEstadoProyecto() != null) t.append("Etapa del proyecto: ").append(describirEtapa(p.getEstadoProyecto())).append("\n");
            if (p.getDescripcion() != null && !p.getDescripcion().isBlank()) t.append("Descripción: ").append(recortar(p.getDescripcion())).append("\n");

            ubicacionRepository.findByProyectoId(p.getId()).ifPresent(u -> t.append("Ubicación: ").append(describirUbicacion(u)).append("\n"));

            List<CasaModelo> villas = casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(p.getId());
            for (CasaModelo c : villas) {
                t.append("Tipología/villa ").append(c.getNombre()).append(": ").append(describirVilla(c)).append("\n");
            }

            List<ZonaComun> zonas = zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(p.getId());
            if (!zonas.isEmpty()) {
                t.append("Amenidades: ").append(zonas.stream().map(ZonaComun::getNombre).collect(Collectors.joining(", "))).append("\n");
            }

            Map<String, Long> lotes = lotesPorProyecto.get(p.getId());
            if (lotes != null && !lotes.isEmpty()) {
                t.append("Lotes por estado: ").append(lotes.entrySet().stream().map(e -> e.getValue() + " " + e.getKey().toLowerCase()).collect(Collectors.joining(", ")))
                        .append(" (la disponibilidad cambia; confirmar siempre con el equipo)\n");
            }
        }
        return t.toString();
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
        if (c.getDescripcion() != null && !c.getDescripcion().isBlank()) s.append(s.length() > 0 ? ". " : "").append(recortar(c.getDescripcion()));
        return s.toString();
    }

    private static String recortar(String texto) {
        String limpio = texto.trim().replaceAll("\\s+", " ");
        return limpio.length() > MAX_DESCRIPCION ? limpio.substring(0, MAX_DESCRIPCION).trim() + "…" : limpio;
    }
}
