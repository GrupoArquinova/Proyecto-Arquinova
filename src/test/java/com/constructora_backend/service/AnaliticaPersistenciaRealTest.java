package com.constructora_backend.service;

import com.constructora_backend.entity.EventoAnalitica;
import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.repository.EventoAnaliticaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sin transacción de prueba: ejercita el camino real (proxy de Spring, transacción propia REQUIRES_NEW y confirmación en la base).
 * Los registros que crea llevan una ruta única y se borran al final.
 */
@SpringBootTest
class AnaliticaPersistenciaRealTest {

    @Autowired private AnaliticaService analitica;
    @Autowired private EventoAnaliticaRepository eventos;

    private List<EventoAnalitica> conRuta(String ruta) {
        return eventos.findAll().stream().filter(e -> ruta.equals(e.getRuta())).toList();
    }

    @Test
    @DisplayName("Un evento del navegador queda guardado de verdad, con la ruta limpia y sin ningún dato de la persona")
    void guardaDeVerdad() {
        String ruta = "/zz-prueba-" + UUID.randomUUID();
        List<EventoAnalitica> creados = List.of();
        try {
            assertTrue(analitica.registrarDelNavegador(TipoEventoAnalitica.VISTA_PAGINA, null, ruta + "?correo=a@b.co#x"));

            creados = conRuta(ruta);
            assertEquals(1, creados.size());
            assertEquals(TipoEventoAnalitica.VISTA_PAGINA, creados.get(0).getTipo());
            assertNull(creados.get(0).getProyectoId());
            assertTrue(creados.get(0).getCreadoEn() != null, "la fecha la pone la base");
        } finally {
            eventos.deleteAll(creados);
        }
    }

    @Test
    @DisplayName("Un proyecto que no existe se ignora: no queda nada guardado")
    void proyectoInexistenteNoGuarda() {
        String ruta = "/zz-prueba-" + UUID.randomUUID();

        assertFalse(analitica.registrarDelNavegador(TipoEventoAnalitica.VISTA_PROYECTO, 987654321L, ruta));
        assertTrue(conRuta(ruta).isEmpty());
    }

    @Test
    @DisplayName("El evento del servidor (FORMULARIO_ENVIADO) se guarda sin ruta ni proyecto inventados")
    void eventoDelServidor() {
        long antes = eventos.findAll().stream().filter(e -> e.getTipo() == TipoEventoAnalitica.INICIO_CHAT).count();
        analitica.registrarDelServidor(TipoEventoAnalitica.INICIO_CHAT, null);

        List<EventoAnalitica> todos = eventos.findAll();
        assertEquals(antes + 1, todos.stream().filter(e -> e.getTipo() == TipoEventoAnalitica.INICIO_CHAT).count());
        EventoAnalitica ultimo = todos.stream().filter(e -> e.getTipo() == TipoEventoAnalitica.INICIO_CHAT)
                .max(java.util.Comparator.comparing(EventoAnalitica::getId)).orElseThrow();
        try {
            assertNull(ultimo.getRuta());
            assertNull(ultimo.getProyectoId());
        } finally {
            eventos.delete(ultimo);
        }
    }
}
