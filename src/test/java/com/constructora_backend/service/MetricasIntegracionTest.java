package com.constructora_backend.service;

import com.constructora_backend.dto.response.MetricasDTO;
import com.constructora_backend.entity.ChatPregunta;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.EventoAnalitica;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.SolicitudContacto;
import com.constructora_backend.enums.TemaChat;
import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.enums.TipoRegistroProyecto;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.repository.ChatPreguntaRepository;
import com.constructora_backend.repository.EstadoSolicitudRepository;
import com.constructora_backend.repository.EventoAnaliticaRepository;
import com.constructora_backend.repository.SolicitudContactoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Métricas con la base real. Los datos de prueba se fechan en marzo de 2020 para que no se mezclen con lo que haya en la base
 * local, y todo se revierte al terminar.
 */
@SpringBootTest
@Transactional
class MetricasIntegracionTest {

    private static final LocalDate DESDE = LocalDate.of(2020, 3, 2);   // lunes
    private static final LocalDate HASTA = LocalDate.of(2020, 3, 22);  // domingo

    @Autowired private MetricasService metricas;
    @Autowired private EventoAnaliticaRepository eventos;
    @Autowired private ChatPreguntaRepository chat;
    @Autowired private SolicitudContactoRepository solicitudes;
    @Autowired private EstadoSolicitudRepository estados;
    @PersistenceContext private EntityManager em;

    private void fechar(String tabla, Long id, LocalDateTime fecha) {
        em.createNativeQuery("UPDATE " + tabla + " SET creado_en = ?1 WHERE id = ?2").setParameter(1, fecha).setParameter(2, id).executeUpdate();
    }

    private EventoAnalitica evento(TipoEventoAnalitica tipo, Long proyectoId, LocalDateTime fecha) {
        EventoAnalitica e = new EventoAnalitica();
        e.setTipo(tipo);
        e.setProyectoId(proyectoId);
        e = eventos.saveAndFlush(e);
        fechar("eventos_analitica", e.getId(), fecha);
        return e;
    }

    private ChatPregunta pregunta(String texto, TemaChat tema, LocalDateTime fecha) {
        ChatPregunta p = new ChatPregunta();
        p.setPregunta(texto);
        p.setIdioma("es");
        p.setTema(tema);
        p = chat.saveAndFlush(p);
        fechar("chat_preguntas", p.getId(), fecha);
        return p;
    }

    private SolicitudContacto solicitud(Proyecto proyecto, LocalDateTime fecha) {
        SolicitudContacto s = new SolicitudContacto();
        s.setEstado(estados.findFirstByNombreIgnoreCase("NUEVO").orElseThrow());
        s.setProyecto(proyecto);
        s.setNombre("Prueba métricas");
        s.setCorreo("metricas@prueba.test");
        s.setIdioma("es");
        s.setCosentimientoDatos(true);
        s = solicitudes.saveAndFlush(s);
        fechar("solicitudes_contacto", s.getId(), fecha);
        return s;
    }

    @Test
    @DisplayName("Solicitudes: total, por semana sin huecos, por proyecto y por estado")
    void solicitudes() {
        Empresa empresa = FixturasContenido.empresa(em);
        Proyecto proyecto = FixturasContenido.proyecto(em, empresa, "Proyecto Métricas " + FixturasContenido.sufijo(), true, true, TipoRegistroProyecto.OFERTA_COMERCIAL);
        solicitud(proyecto, LocalDateTime.of(2020, 3, 10, 12, 0)); // semana que empieza el 9
        solicitud(proyecto, LocalDateTime.of(2020, 3, 17, 12, 0)); // semana que empieza el 16
        solicitud(null, LocalDateTime.of(2020, 3, 18, 12, 0));      // sin proyecto
        em.flush();
        em.clear();

        MetricasDTO m = metricas.obtener(DESDE, HASTA, false);

        assertEquals(DESDE, m.periodo().desde());
        assertEquals(HASTA, m.periodo().hasta());
        assertEquals(3, m.solicitudes().total());

        // Tres semanas ISO, la primera sin datos (con 0)
        List<MetricasDTO.PorSemana> semanas = m.solicitudes().porSemana();
        assertEquals(3, semanas.size());
        assertEquals("2020-W10", semanas.get(0).semana());
        assertEquals(LocalDate.of(2020, 3, 2), semanas.get(0).inicio());
        assertEquals(0, semanas.get(0).total());
        assertEquals("2020-W11", semanas.get(1).semana());
        assertEquals(1, semanas.get(1).total());
        assertEquals("2020-W12", semanas.get(2).semana());
        assertEquals(2, semanas.get(2).total());

        var porProyecto = m.solicitudes().porProyecto();
        assertEquals(2, porProyecto.get(0).total());
        assertEquals(proyecto.getId(), porProyecto.get(0).proyectoId());
        assertEquals(proyecto.getNombre(), porProyecto.get(0).proyecto());
        assertEquals("Sin proyecto", porProyecto.get(1).proyecto());

        Map<String, Long> estadosPorNombre = new TreeMap<>();
        m.solicitudes().porEstado().forEach(e -> estadosPorNombre.put(e.estado().toUpperCase(), e.total()));
        assertEquals(3L, estadosPorNombre.get("NUEVO"));
    }

    @Test
    @DisplayName("Analítica: vistas por proyecto, eventos por tipo (todos los tipos), vistas por día sin huecos y embudo")
    void analitica() {
        Empresa empresa = FixturasContenido.empresa(em);
        Proyecto proyecto = FixturasContenido.proyecto(em, empresa, "Proyecto Vistas " + FixturasContenido.sufijo(), true, true, TipoRegistroProyecto.OFERTA_COMERCIAL);
        for (int i = 0; i < 3; i++) evento(TipoEventoAnalitica.VISTA_PAGINA, null, LocalDateTime.of(2020, 3, 3, 12, i));
        evento(TipoEventoAnalitica.VISTA_PAGINA, null, LocalDateTime.of(2020, 3, 20, 12, 0));
        evento(TipoEventoAnalitica.VISTA_PROYECTO, proyecto.getId(), LocalDateTime.of(2020, 3, 4, 12, 0));
        evento(TipoEventoAnalitica.VISTA_PROYECTO, proyecto.getId(), LocalDateTime.of(2020, 3, 5, 12, 0));
        evento(TipoEventoAnalitica.CLICK_WHATSAPP, proyecto.getId(), LocalDateTime.of(2020, 3, 5, 13, 0));
        evento(TipoEventoAnalitica.FORMULARIO_ENVIADO, proyecto.getId(), LocalDateTime.of(2020, 3, 6, 12, 0));
        evento(TipoEventoAnalitica.DESCARGA_BROCHURE, proyecto.getId(), LocalDateTime.of(2020, 3, 6, 13, 0));
        evento(TipoEventoAnalitica.VISTA_PAGINA, null, LocalDateTime.of(2020, 4, 1, 12, 0)); // fuera del periodo
        em.flush();
        em.clear();

        MetricasDTO.Analitica a = metricas.obtener(DESDE, HASTA, false).analitica();

        assertEquals(1, a.visitasPorProyecto().size());
        assertEquals(proyecto.getId(), a.visitasPorProyecto().get(0).proyectoId());
        assertEquals(2, a.visitasPorProyecto().get(0).vistas());

        assertEquals(TipoEventoAnalitica.values().length, a.eventosPorTipo().size());
        Map<String, Long> porTipo = new TreeMap<>();
        a.eventosPorTipo().forEach(t -> porTipo.put(t.tipo(), t.total()));
        assertEquals(4L, porTipo.get("VISTA_PAGINA"));
        assertEquals(0L, porTipo.get("COMPARTIR"));
        assertEquals(0L, porTipo.get("INICIO_CHAT"));

        assertEquals(21, a.visitasPorDia().size(), "una entrada por cada día del periodo, con 0 donde no hubo");
        assertEquals(LocalDate.of(2020, 3, 2), a.visitasPorDia().get(0).fecha());
        assertEquals(0, a.visitasPorDia().get(0).total());
        assertEquals(3, a.visitasPorDia().get(1).total());
        assertEquals(1, a.visitasPorDia().get(18).total());
        assertEquals(LocalDate.of(2020, 3, 22), a.visitasPorDia().get(20).fecha());

        assertEquals(new MetricasDTO.Embudo(2, 1, 1, 1), a.embudo());
    }

    @Test
    @DisplayName("Chat: el administrador ve las últimas preguntas; el comercial recibe la lista vacía pero sí los totales y temas")
    void chat() {
        pregunta("¿Dónde queda?", TemaChat.UBICACION, LocalDateTime.of(2020, 3, 3, 12, 0));
        pregunta("¿Cuánto cuesta?", TemaChat.PRECIOS, LocalDateTime.of(2020, 3, 4, 12, 0));
        pregunta("¿Cuánto cuesta una villa?", TemaChat.PRECIOS, LocalDateTime.of(2020, 3, 5, 12, 0));
        pregunta("fuera del periodo", TemaChat.OTRO, LocalDateTime.of(2019, 1, 1, 12, 0));
        em.flush();
        em.clear();

        MetricasDTO.Chat admin = metricas.obtener(DESDE, HASTA, true).chat();
        assertEquals(3, admin.totalPreguntas());
        assertEquals(3, admin.recientes().size());
        assertEquals("¿Cuánto cuesta una villa?", admin.recientes().get(0).pregunta(), "la más reciente primero");
        assertEquals("PRECIOS", admin.recientes().get(0).tema());
        assertNotNull(admin.recientes().get(0).fecha());
        assertEquals("PRECIOS", admin.temas().get(0).tema(), "temas de más a menos frecuente");
        assertEquals(2, admin.temas().get(0).total());

        MetricasDTO.Chat comercial = metricas.obtener(DESDE, HASTA, false).chat();
        assertEquals(3, comercial.totalPreguntas());
        assertEquals(2, comercial.temas().size());
        assertTrue(comercial.recientes().isEmpty(), "el texto de las preguntas solo lo ve el administrador");
    }

    @Test
    @DisplayName("Lotes: por estado y por proyecto (disponibles, reservados, vendidos y total)")
    void lotes() {
        Empresa empresa = FixturasContenido.empresa(em);
        Proyecto proyecto = FixturasContenido.proyecto(em, empresa, "Proyecto Lotes " + FixturasContenido.sufijo(), true, true, TipoRegistroProyecto.OFERTA_COMERCIAL);
        Etapa etapa = FixturasContenido.etapa(em, proyecto, "Etapa 1");
        var disponible = FixturasContenido.estadoLote(em, "DISPONIBLE");
        var vendido = FixturasContenido.estadoLote(em, "VENDIDO");
        FixturasContenido.lote(em, etapa, disponible, "A-1", "1000");
        FixturasContenido.lote(em, etapa, disponible, "A-2", "1200");
        FixturasContenido.lote(em, etapa, vendido, "A-3", "900");
        em.flush();
        em.clear();

        MetricasDTO.Lotes lotes = metricas.obtener(DESDE, HASTA, false).lotes();

        var fila = lotes.porProyecto().stream().filter(l -> proyecto.getId().equals(l.proyectoId())).findFirst().orElseThrow();
        assertEquals(proyecto.getNombre(), fila.proyecto());
        assertEquals(2, fila.disponibles());
        assertEquals(0, fila.reservados());
        assertEquals(1, fila.vendidos());
        assertEquals(3, fila.total());
        assertTrue(lotes.porEstado().stream().anyMatch(e -> e.estado().equalsIgnoreCase("DISPONIBLE") && e.total() >= 2));
    }

    @Test
    @DisplayName("Por defecto cubre los últimos 90 días hasta hoy; rechaza fechas invertidas y periodos de más de 731 días")
    void periodoPorDefectoYValidaciones() {
        MetricasDTO m = metricas.obtener(null, null, false);

        assertEquals(90, java.time.temporal.ChronoUnit.DAYS.between(m.periodo().desde(), m.periodo().hasta()) + 1);
        assertEquals(90, m.analitica().visitasPorDia().size());
        assertTrue(m.solicitudes().porSemana().size() >= 13 && m.solicitudes().porSemana().size() <= 14);

        assertThrows(BadRequestException.class, () -> metricas.obtener(HASTA, DESDE, false));
        assertThrows(BadRequestException.class, () -> metricas.obtener(LocalDate.of(2018, 1, 1), HASTA, false));
    }

    @Test
    @DisplayName("Semanas ISO: etiqueta, lunes y relleno de las semanas vacías (también al cruzar de año)")
    void semanasIso() {
        var semanas = MetricasService.semanas(LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 11), Map.of(LocalDate.of(2026, 1, 5), 4L));

        assertEquals(2, semanas.size());
        assertEquals("2026-W01", semanas.get(0).semana()); // la semana del 29/12/2025 ya es la W01 de 2026
        assertEquals(LocalDate.of(2025, 12, 29), semanas.get(0).inicio());
        assertEquals(0, semanas.get(0).total());
        assertEquals("2026-W02", semanas.get(1).semana());
        assertEquals(4, semanas.get(1).total());
        assertEquals("2026-W44", MetricasService.etiquetaSemana(LocalDate.of(2026, 10, 26)));
    }
}
