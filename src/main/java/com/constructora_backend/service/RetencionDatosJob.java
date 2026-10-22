package com.constructora_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Retención de datos personales (Ley 1581): una vez al día elimina las solicitudes de contacto que superan el plazo de
 * conservación. El plazo se define en meses con app.retencion.solicitudes-meses (APP_RETENCION_SOLICITUDES_MESES);
 * 0 desactiva la tarea. En el registro solo queda la cantidad eliminada, nunca datos de las personas.
 */
@Component
@Slf4j
public class RetencionDatosJob {

    private final SolicitudContactoService solicitudService;

    @Value("${app.retencion.solicitudes-meses:24}")
    private int mesesSolicitudes = 24;

    @Autowired
    public RetencionDatosJob(SolicitudContactoService solicitudService) {
        this.solicitudService = solicitudService;
    }

    /** Todos los días a las 03:00 (hora de Bogotá). */
    @Scheduled(cron = "${app.retencion.cron:0 0 3 * * *}", zone = "America/Bogota")
    public void depurarSolicitudes() {
        depurarSolicitudes(LocalDateTime.now());
    }

    /** Devuelve cuántas solicitudes eliminó; 0 si la retención está desactivada. */
    int depurarSolicitudes(LocalDateTime ahora) {
        if (mesesSolicitudes <= 0) {
            return 0;
        }
        try {
            int eliminadas = solicitudService.eliminarAnterioresA(ahora.minusMonths(mesesSolicitudes));
            log.info("Retención de datos: {} solicitudes de contacto con más de {} meses eliminadas", eliminadas, mesesSolicitudes);
            return eliminadas;
        } catch (Exception ex) {
            log.error("Retención de datos: no se pudo depurar las solicitudes antiguas: {}", ex.getMessage());
            return 0;
        }
    }
}
