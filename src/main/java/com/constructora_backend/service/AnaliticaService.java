package com.constructora_backend.service;

import com.constructora_backend.entity.EventoAnalitica;
import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.repository.EventoAnaliticaRepository;
import com.constructora_backend.repository.ProyectoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Analítica propia sin cookies. Solo se guarda QUÉ pasó, en qué proyecto, en qué ruta y cuándo: nada de IP, user-agent ni
 * identificadores de visitante. Cada tipo de evento lo registra una sola parte (ver {@link TipoEventoAnalitica}).
 * <p>
 * Registrar un evento nunca debe romper la operación que lo origina (guardar una solicitud, entregar un PDF, responder
 * el chat): por eso corre en su propia transacción y los errores se capturan y se dejan en el log.
 */
@Service
@Slf4j
public class AnaliticaService {

    /** Largo de la columna ruta. */
    public static final int MAX_RUTA = 200;

    private final EventoAnaliticaRepository eventoRepository;
    private final ProyectoRepository proyectoRepository;

    @Autowired
    public AnaliticaService(EventoAnaliticaRepository eventoRepository, ProyectoRepository proyectoRepository) {
        this.eventoRepository = eventoRepository;
        this.proyectoRepository = proyectoRepository;
    }

    /**
     * Evento que manda el navegador. Se ignora en silencio (devuelve false) si el tipo lo registra el servidor, si apunta a un
     * proyecto que no existe o no está publicado y activo, o si es un evento de proyecto sin proyecto.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean registrarDelNavegador(TipoEventoAnalitica tipo, Long proyectoId, String ruta) {
        if (tipo == null || tipo.esSoloServidor()) {
            return false;
        }
        if (proyectoId == null && tipo != TipoEventoAnalitica.VISTA_PAGINA && tipo != TipoEventoAnalitica.COMPARTIR) {
            // Ver un proyecto o abrir WhatsApp desde él no tienen sentido sin proyecto
            return false;
        }
        if (proyectoId != null && !proyectoRepository.existsByIdAndPublicadoTrueAndActivoTrue(proyectoId)) {
            return false;
        }
        guardar(tipo, proyectoId, ruta);
        return true;
    }

    /** Evento que registra el servidor (formulario enviado, inicio del chat, descarga del brochure). Nunca lanza excepciones. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarDelServidor(TipoEventoAnalitica tipo, Long proyectoId) {
        try {
            guardar(tipo, proyectoId, null);
        } catch (Exception ex) {
            log.warn("Analítica: no se pudo registrar {}: {}", tipo, ex.getMessage());
        }
    }

    private void guardar(TipoEventoAnalitica tipo, Long proyectoId, String ruta) {
        EventoAnalitica evento = new EventoAnalitica();
        evento.setTipo(tipo);
        evento.setProyectoId(proyectoId);
        evento.setRuta(sanitizarRuta(ruta));
        eventoRepository.save(evento);
    }

    /** Borra los eventos anteriores a la fecha (retención); devuelve cuántos eran. */
    @Transactional
    public int eliminarAnterioresA(LocalDateTime limite) {
        return eventoRepository.eliminarAnterioresA(limite);
    }

    /**
     * Deja solo la ruta: sin dominio, sin querystring (puede traer datos personales o tokens), sin fragmento, sin caracteres
     * de control y con máximo 200 caracteres. Devuelve null si no queda nada.
     */
    public static String sanitizarRuta(String ruta) {
        if (ruta == null) {
            return null;
        }
        String limpia = ruta.replaceAll("\\p{Cntrl}", "").trim();
        // Si mandan una URL completa, solo interesa el camino
        int esquema = limpia.indexOf("://");
        if (esquema >= 0) {
            int inicioCamino = limpia.indexOf('/', esquema + 3);
            limpia = inicioCamino >= 0 ? limpia.substring(inicioCamino) : "/";
        }
        int corte = primeraPosicion(limpia, '?', '#');
        if (corte >= 0) {
            limpia = limpia.substring(0, corte);
        }
        if (limpia.isEmpty()) {
            return null;
        }
        return limpia.length() > MAX_RUTA ? limpia.substring(0, MAX_RUTA) : limpia;
    }

    private static int primeraPosicion(String texto, char a, char b) {
        int ia = texto.indexOf(a);
        int ib = texto.indexOf(b);
        if (ia < 0) return ib;
        if (ib < 0) return ia;
        return Math.min(ia, ib);
    }
}
