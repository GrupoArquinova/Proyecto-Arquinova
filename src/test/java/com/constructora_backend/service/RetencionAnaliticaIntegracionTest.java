package com.constructora_backend.service;

import com.constructora_backend.entity.EventoAnalitica;
import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.repository.EventoAnaliticaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Analítica con la base real: retención y esquema sin datos de la persona. Todo se revierte al terminar. */
@SpringBootTest
@Transactional
class RetencionAnaliticaIntegracionTest {

    @Autowired private AnaliticaService analitica;
    @Autowired private EventoAnaliticaRepository eventos;
    @PersistenceContext private EntityManager em;

    private EventoAnalitica nuevo() {
        EventoAnalitica e = new EventoAnalitica();
        e.setTipo(TipoEventoAnalitica.VISTA_PAGINA);
        e.setRuta("/prueba");
        return eventos.saveAndFlush(e);
    }

    @Test
    @DisplayName("Borra los eventos de más de 24 meses y conserva los recientes")
    void borraSoloLosAntiguos() {
        EventoAnalitica viejo = nuevo();
        EventoAnalitica reciente = nuevo();
        em.createNativeQuery("UPDATE eventos_analitica SET creado_en = ?1 WHERE id = ?2")
                .setParameter(1, LocalDateTime.now().minusMonths(30)).setParameter(2, viejo.getId()).executeUpdate();
        em.clear();

        int borrados = analitica.eliminarAnterioresA(LocalDateTime.now().minusMonths(24));
        em.clear();

        assertTrue(borrados >= 1);
        assertFalse(eventos.findById(viejo.getId()).isPresent());
        assertTrue(eventos.findById(reciente.getId()).isPresent());
    }

    @Test
    @DisplayName("El esquema real de eventos_analitica y chat_preguntas no tiene columnas de IP, navegador ni identificador de visitante")
    void esquemaSinDatosDeLaPersona() {
        @SuppressWarnings("unchecked")
        List<String> eventosColumnas = em.createNativeQuery(
                "select column_name from information_schema.columns where table_schema = database() and table_name = 'eventos_analitica' order by ordinal_position")
                .getResultList();
        @SuppressWarnings("unchecked")
        List<String> chatColumnas = em.createNativeQuery(
                "select column_name from information_schema.columns where table_schema = database() and table_name = 'chat_preguntas' order by ordinal_position")
                .getResultList();

        assertEquals(List.of("id", "tipo", "proyecto_id", "ruta", "creado_en"), eventosColumnas.stream().map(String::toLowerCase).toList());
        assertEquals(List.of("id", "pregunta", "idioma", "tema", "creado_en"), chatColumnas.stream().map(String::toLowerCase).toList());
    }

    @Test
    @DisplayName("El servidor registra un evento propio en su transacción y la ruta guardada no lleva querystring")
    void guardaConLaRutaLimpia() {
        EventoAnalitica e = new EventoAnalitica();
        e.setTipo(TipoEventoAnalitica.COMPARTIR);
        e.setRuta(AnaliticaService.sanitizarRuta("https://x.test/proyectos/3?utm=1#a"));
        e = eventos.saveAndFlush(e);
        em.clear();

        assertEquals("/proyectos/3", eventos.findById(e.getId()).orElseThrow().getRuta());
        assertEquals(TipoEventoAnalitica.COMPARTIR, eventos.findById(e.getId()).orElseThrow().getTipo());
    }
}
