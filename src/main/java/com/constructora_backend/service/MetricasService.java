package com.constructora_backend.service;

import com.constructora_backend.dto.response.MetricasDTO;
import com.constructora_backend.entity.ChatPregunta;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.EstadoSolicitud;
import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.repository.ChatPreguntaRepository;
import com.constructora_backend.repository.EstadoLoteRepository;
import com.constructora_backend.repository.EstadoSolicitudRepository;
import com.constructora_backend.repository.EventoAnaliticaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Métricas del dashboard. Todo se agrega en la base de datos (GROUP BY); en memoria solo se completan los huecos de las
 * series (semanas y días sin datos van con 0) y se pivotan los lotes por estado.
 */
@Service
public class MetricasService {

    /** Días del periodo por defecto (incluye el día de hoy). */
    public static final int DIAS_POR_DEFECTO = 90;
    /** Periodo máximo consultable (para que las series y las consultas no crezcan sin tope). */
    public static final int MAX_DIAS = 731;
    /** Preguntas recientes del asistente que se devuelven. */
    public static final int MAX_RECIENTES = 30;

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @PersistenceContext
    private EntityManager em;

    private final EventoAnaliticaRepository eventoRepository;
    private final ChatPreguntaRepository chatRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final EstadoLoteRepository estadoLoteRepository;

    @Autowired
    public MetricasService(EventoAnaliticaRepository eventoRepository,
                           ChatPreguntaRepository chatRepository,
                           EstadoSolicitudRepository estadoSolicitudRepository,
                           EstadoLoteRepository estadoLoteRepository) {
        this.eventoRepository = eventoRepository;
        this.chatRepository = chatRepository;
        this.estadoSolicitudRepository = estadoSolicitudRepository;
        this.estadoLoteRepository = estadoLoteRepository;
    }

    /**
     * @param desde          primer día incluido; null = 89 días antes de {@code hasta} (90 días con el último)
     * @param hasta          último día incluido; null = hoy (hora de Bogotá)
     * @param incluirPreguntas si se incluyen las últimas preguntas del asistente (solo administradores)
     */
    @Transactional(readOnly = true)
    public MetricasDTO obtener(LocalDate desde, LocalDate hasta, boolean incluirPreguntas) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now(BOGOTA);
        LocalDate inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO - 1L);
        if (inicio.isAfter(fin)) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) >= MAX_DIAS) {
            throw new BadRequestException("El periodo no puede superar " + MAX_DIAS + " días");
        }
        LocalDateTime d = inicio.atStartOfDay();
        LocalDateTime h = fin.plusDays(1).atStartOfDay();

        return new MetricasDTO(
                new MetricasDTO.Periodo(inicio, fin),
                solicitudes(inicio, fin, d, h),
                lotes(),
                analitica(inicio, fin, d, h),
                chat(d, h, incluirPreguntas));
    }

    // ------------------------------------------------------------------ solicitudes

    private MetricasDTO.Solicitudes solicitudes(LocalDate inicio, LocalDate fin, LocalDateTime d, LocalDateTime h) {
        long total = numero(em.createQuery(
                        "select count(s) from SolicitudContacto s where s.creadoEn >= :d and s.creadoEn < :h")
                .setParameter("d", d).setParameter("h", h).getSingleResult());

        // Por día en la base y por semana ISO aquí: son a lo sumo MAX_DIAS filas
        @SuppressWarnings("unchecked")
        List<Object[]> porDia = em.createQuery(
                        "select cast(s.creadoEn as LocalDate), count(s) from SolicitudContacto s "
                                + "where s.creadoEn >= :d and s.creadoEn < :h group by cast(s.creadoEn as LocalDate)")
                .setParameter("d", d).setParameter("h", h).getResultList();
        Map<LocalDate, Long> conteoDiario = new TreeMap<>();
        for (Object[] fila : porDia) {
            conteoDiario.put((LocalDate) fila[0], numero(fila[1]));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> porProyecto = em.createQuery(
                        "select p.id, p.nombre, count(s) from SolicitudContacto s left join s.proyecto p "
                                + "where s.creadoEn >= :d and s.creadoEn < :h group by p.id, p.nombre order by count(s) desc, p.nombre")
                .setParameter("d", d).setParameter("h", h).getResultList();
        List<MetricasDTO.PorProyecto> proyectos = new ArrayList<>();
        for (Object[] fila : porProyecto) {
            Long id = fila[0] == null ? null : ((Number) fila[0]).longValue();
            proyectos.add(new MetricasDTO.PorProyecto(id, id == null ? "Sin proyecto" : (String) fila[1], numero(fila[2])));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> porEstado = em.createQuery(
                        "select e.nombre, count(s) from SolicitudContacto s join s.estado e "
                                + "where s.creadoEn >= :d and s.creadoEn < :h group by e.id, e.nombre")
                .setParameter("d", d).setParameter("h", h).getResultList();
        Map<String, Long> conteoEstados = new LinkedHashMap<>();
        for (Object[] fila : porEstado) {
            conteoEstados.put((String) fila[0], numero(fila[1]));
        }
        // Todos los estados activos aparecen (con 0 si no hay solicitudes), en el orden del catálogo
        List<MetricasDTO.PorEstado> estados = new ArrayList<>();
        for (EstadoSolicitud estado : estadoSolicitudRepository.findAllByOrderByOrdenAsc()) {
            Long cantidad = conteoEstados.remove(estado.getNombre());
            if (Boolean.TRUE.equals(estado.getActivo()) || cantidad != null) {
                estados.add(new MetricasDTO.PorEstado(estado.getNombre(), cantidad == null ? 0 : cantidad));
            }
        }
        conteoEstados.forEach((nombre, cantidad) -> estados.add(new MetricasDTO.PorEstado(nombre, cantidad)));

        return new MetricasDTO.Solicitudes(total, semanas(inicio, fin, conteoDiario), proyectos, estados);
    }

    /** Una entrada por cada semana ISO que toca el periodo, con 0 en las que no tienen datos. */
    static List<MetricasDTO.PorSemana> semanas(LocalDate inicio, LocalDate fin, Map<LocalDate, Long> conteoDiario) {
        Map<LocalDate, Long> porLunes = new TreeMap<>();
        LocalDate lunes = lunesDe(inicio);
        LocalDate ultimoLunes = lunesDe(fin);
        for (LocalDate l = lunes; !l.isAfter(ultimoLunes); l = l.plusWeeks(1)) {
            porLunes.put(l, 0L);
        }
        conteoDiario.forEach((dia, total) -> porLunes.merge(lunesDe(dia), total, Long::sum));
        List<MetricasDTO.PorSemana> semanas = new ArrayList<>();
        porLunes.forEach((l, total) -> semanas.add(new MetricasDTO.PorSemana(etiquetaSemana(l), l, total)));
        return semanas;
    }

    static LocalDate lunesDe(LocalDate dia) {
        return dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** "2026-W44": año y número de semana ISO. */
    static String etiquetaSemana(LocalDate lunes) {
        return String.format(Locale.ROOT, "%d-W%02d", lunes.get(IsoFields.WEEK_BASED_YEAR), lunes.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    // ------------------------------------------------------------------ lotes

    private MetricasDTO.Lotes lotes() {
        @SuppressWarnings("unchecked")
        List<Object[]> porEstado = em.createQuery(
                        "select e.nombre, count(l) from Lote l join l.estado e where l.activo = true group by e.id, e.nombre")
                .getResultList();
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (Object[] fila : porEstado) {
            conteo.put((String) fila[0], numero(fila[1]));
        }
        List<MetricasDTO.PorEstado> estados = new ArrayList<>();
        for (EstadoLote estado : estadoLoteRepository.findAllByOrderByOrdenAsc()) {
            Long cantidad = conteo.remove(estado.getNombre());
            if (Boolean.TRUE.equals(estado.getActivo()) || cantidad != null) {
                estados.add(new MetricasDTO.PorEstado(estado.getNombre(), cantidad == null ? 0 : cantidad));
            }
        }
        conteo.forEach((nombre, cantidad) -> estados.add(new MetricasDTO.PorEstado(nombre, cantidad)));

        // Por proyecto: una fila por (proyecto, estado) y aquí se reparte en disponibles / reservados / vendidos / total
        @SuppressWarnings("unchecked")
        List<Object[]> porProyecto = em.createQuery(
                        "select p.id, p.nombre, e.nombre, count(l) from Lote l join l.etapa et join et.proyecto p join l.estado e "
                                + "where l.activo = true and p.activo = true group by p.id, p.nombre, e.id, e.nombre order by p.nombre")
                .getResultList();
        Map<Long, long[]> acumulado = new LinkedHashMap<>();
        Map<Long, String> nombres = new LinkedHashMap<>();
        for (Object[] fila : porProyecto) {
            Long id = ((Number) fila[0]).longValue();
            nombres.put(id, (String) fila[1]);
            long[] cifras = acumulado.computeIfAbsent(id, k -> new long[4]);
            long cantidad = numero(fila[3]);
            String estado = ((String) fila[2]).toUpperCase(Locale.ROOT);
            switch (estado) {
                case "DISPONIBLE" -> cifras[0] += cantidad;
                case "RESERVADO" -> cifras[1] += cantidad;
                case "VENDIDO" -> cifras[2] += cantidad;
                default -> { }
            }
            cifras[3] += cantidad;
        }
        List<MetricasDTO.LotesDeProyecto> proyectos = new ArrayList<>();
        acumulado.forEach((id, c) -> proyectos.add(new MetricasDTO.LotesDeProyecto(id, nombres.get(id), c[0], c[1], c[2], c[3])));
        return new MetricasDTO.Lotes(estados, proyectos);
    }

    // ------------------------------------------------------------------ analítica

    private MetricasDTO.Analitica analitica(LocalDate inicio, LocalDate fin, LocalDateTime d, LocalDateTime h) {
        List<MetricasDTO.VistasDeProyecto> vistas = new ArrayList<>();
        for (Object[] fila : eventoRepository.vistasPorProyecto(d, h)) {
            vistas.add(new MetricasDTO.VistasDeProyecto(((Number) fila[0]).longValue(), (String) fila[1], numero(fila[2])));
        }

        // Todos los tipos aparecen, con 0 si no hubo eventos
        Map<TipoEventoAnalitica, Long> porTipo = new EnumMap<>(TipoEventoAnalitica.class);
        for (TipoEventoAnalitica tipo : TipoEventoAnalitica.values()) {
            porTipo.put(tipo, 0L);
        }
        for (Object[] fila : eventoRepository.contarPorTipo(d, h)) {
            porTipo.put((TipoEventoAnalitica) fila[0], numero(fila[1]));
        }
        List<MetricasDTO.PorTipo> tipos = new ArrayList<>();
        porTipo.forEach((tipo, total) -> tipos.add(new MetricasDTO.PorTipo(tipo.name(), total)));

        Map<LocalDate, Long> diario = new TreeMap<>();
        for (Object[] fila : eventoRepository.vistasPaginaPorDia(d, h)) {
            diario.put((LocalDate) fila[0], numero(fila[1]));
        }
        List<MetricasDTO.PorDia> dias = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            dias.add(new MetricasDTO.PorDia(dia, diario.getOrDefault(dia, 0L)));
        }

        MetricasDTO.Embudo embudo = new MetricasDTO.Embudo(
                porTipo.get(TipoEventoAnalitica.VISTA_PROYECTO),
                porTipo.get(TipoEventoAnalitica.CLICK_WHATSAPP),
                porTipo.get(TipoEventoAnalitica.FORMULARIO_ENVIADO),
                porTipo.get(TipoEventoAnalitica.DESCARGA_BROCHURE));
        return new MetricasDTO.Analitica(vistas, tipos, dias, embudo);
    }

    // ------------------------------------------------------------------ chat

    private MetricasDTO.Chat chat(LocalDateTime d, LocalDateTime h, boolean incluirPreguntas) {
        long total = chatRepository.countByCreadoEnGreaterThanEqualAndCreadoEnLessThan(d, h);
        List<MetricasDTO.PorTema> temas = new ArrayList<>();
        for (Object[] fila : chatRepository.contarPorTema(d, h)) {
            temas.add(new MetricasDTO.PorTema(((Enum<?>) fila[0]).name(), numero(fila[1])));
        }
        temas.sort((a, b) -> Long.compare(b.total(), a.total()));

        List<MetricasDTO.PreguntaReciente> recientes = new ArrayList<>();
        if (incluirPreguntas) {
            for (ChatPregunta p : chatRepository.findTop30ByCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnDescIdDesc(d, h)) {
                recientes.add(new MetricasDTO.PreguntaReciente(p.getCreadoEn(), p.getPregunta(), p.getTema().name()));
            }
        }
        return new MetricasDTO.Chat(total, temas, recientes);
    }

    private static long numero(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }
}
