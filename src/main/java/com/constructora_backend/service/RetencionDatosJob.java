package com.constructora_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Retención de datos personales (Ley 1581): una vez al día elimina lo que supera su plazo de conservación.
 * <ul>
 *   <li>Solicitudes de contacto: app.retencion.solicitudes-meses (por defecto 24).</li>
 *   <li>Eventos de analítica sin cookies: app.retencion.analitica-meses (por defecto 24).</li>
 *   <li>Preguntas guardadas del asistente de IA: app.retencion.chat-meses (por defecto 12).</li>
 *   <li>Historial de cambios del panel (incluye IP y navegador de quien cambió algo): app.retencion.auditoria-meses (por defecto 24).</li>
 * </ul>
 * Poner un plazo en 0 desactiva la depuración de ese dato. En el registro solo queda la cantidad eliminada, nunca datos de las personas.
 * Cada depuración es independiente: si una falla, las demás se ejecutan igual.
 */
@Component
@Slf4j
public class RetencionDatosJob {

    private final SolicitudContactoService solicitudService;
    private final AnaliticaService analiticaService;
    private final ChatPreguntaService chatPreguntaService;
    private final AuditoriaService auditoriaService;

    @Value("${app.retencion.solicitudes-meses:24}")
    private int mesesSolicitudes = 24;

    @Value("${app.retencion.analitica-meses:24}")
    private int mesesAnalitica = 24;

    @Value("${app.retencion.chat-meses:12}")
    private int mesesChat = 12;

    @Value("${app.retencion.auditoria-meses:24}")
    private int mesesAuditoria = 24;

    /** Solo solicitudes (pruebas y usos mínimos). */
    public RetencionDatosJob(SolicitudContactoService solicitudService) {
        this(solicitudService, null, null, null);
    }

    @Autowired
    public RetencionDatosJob(SolicitudContactoService solicitudService,
                             @Autowired(required = false) AnaliticaService analiticaService,
                             @Autowired(required = false) ChatPreguntaService chatPreguntaService,
                             @Autowired(required = false) AuditoriaService auditoriaService) {
        this.solicitudService = solicitudService;
        this.analiticaService = analiticaService;
        this.chatPreguntaService = chatPreguntaService;
        this.auditoriaService = auditoriaService;
    }

    /** Todos los días a las 03:00 (hora de Bogotá). */
    @Scheduled(cron = "${app.retencion.cron:0 0 3 * * *}", zone = "America/Bogota")
    public void depurarSolicitudes() {
        LocalDateTime ahora = LocalDateTime.now();
        depurarSolicitudes(ahora);
        depurarAnalitica(ahora);
        depurarPreguntasChat(ahora);
        depurarAuditoria(ahora);
    }

    /** Devuelve cuántas solicitudes eliminó; 0 si la retención está desactivada. */
    int depurarSolicitudes(LocalDateTime ahora) {
        return depurar("solicitudes de contacto", mesesSolicitudes, ahora, solicitudService::eliminarAnterioresA);
    }

    /** Eventos de analítica con más de 24 meses (por defecto). */
    int depurarAnalitica(LocalDateTime ahora) {
        return analiticaService == null ? 0
                : depurar("eventos de analítica", mesesAnalitica, ahora, analiticaService::eliminarAnterioresA);
    }

    /** Preguntas del asistente con más de 12 meses (por defecto). */
    int depurarPreguntasChat(LocalDateTime ahora) {
        return chatPreguntaService == null ? 0
                : depurar("preguntas del asistente", mesesChat, ahora, chatPreguntaService::eliminarAnterioresA);
    }

    /** Registros del historial de cambios con más de 24 meses (por defecto). */
    int depurarAuditoria(LocalDateTime ahora) {
        return auditoriaService == null ? 0
                : depurar("registros del historial de cambios", mesesAuditoria, ahora, auditoriaService::eliminarAnterioresA);
    }

    @FunctionalInterface
    private interface Borrado {
        int eliminar(LocalDateTime limite);
    }

    private int depurar(String que, int meses, LocalDateTime ahora, Borrado borrado) {
        if (meses <= 0) {
            return 0;
        }
        try {
            int eliminados = borrado.eliminar(ahora.minusMonths(meses));
            log.info("Retención de datos: {} {} con más de {} meses eliminados", eliminados, que, meses);
            return eliminados;
        } catch (Exception ex) {
            log.error("Retención de datos: no se pudo depurar {}: {}", que, ex.getMessage());
            return 0;
        }
    }
}
