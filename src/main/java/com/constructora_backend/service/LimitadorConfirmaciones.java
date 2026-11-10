package com.constructora_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evita que el formulario público sirva para llenar de correos la bandeja de un tercero:
 * a cada dirección se le envía como máximo una confirmación por hora y, además, hay un tope global de confirmaciones
 * por hora y por día (para que el abuso repartido entre muchas direcciones tampoco queme la cuota del correo).
 */
@Component
public class LimitadorConfirmaciones {

    static final Duration ESPERA = Duration.ofHours(1);
    /** Tope de direcciones recordadas a la vez; pasado este número se descartan primero las vencidas. */
    static final int MAX_DIRECCIONES = 50_000;

    private static final String CLAVE_HORA = "confirmaciones:hora";
    private static final String CLAVE_DIA = "confirmaciones:dia";

    private final Map<String, Instant> ultimoEnvio = new ConcurrentHashMap<>();
    private final LimitadorVentana ventanas;

    /** Máximo de confirmaciones de todo el sitio por hora y por día (APP_CONTACTO_CONFIRMACIONES_MAX_HORA / _MAX_DIA). */
    @Value("${app.contacto.confirmaciones-max-hora:60}")
    private int maxPorHora = 60;
    @Value("${app.contacto.confirmaciones-max-dia:300}")
    private int maxPorDia = 300;

    public LimitadorConfirmaciones() {
        this(new LimitadorVentana());
    }

    @Autowired
    public LimitadorConfirmaciones(LimitadorVentana ventanas) {
        this.ventanas = ventanas;
    }

    /** Para pruebas: topes globales explícitos. */
    LimitadorConfirmaciones(LimitadorVentana ventanas, int maxPorHora, int maxPorDia) {
        this(ventanas);
        this.maxPorHora = maxPorHora;
        this.maxPorDia = maxPorDia;
    }

    /** true si ya se puede enviar a esta dirección (y deja registrado el envío); false si se envió hace menos de una hora o se llegó al tope global. */
    public boolean permitir(String correo) {
        return permitir(correo, Instant.now());
    }

    boolean permitir(String correo, Instant ahora) {
        if (correo == null || correo.isBlank()) return false;
        // Se limpian los registros viejos para que el mapa no crezca sin fin
        ultimoEnvio.values().removeIf(instante -> instante.plus(ESPERA).isBefore(ahora));
        String clave = correo.trim().toLowerCase(Locale.ROOT);
        if (ultimoEnvio.size() >= MAX_DIRECCIONES && !ultimoEnvio.containsKey(clave)) {
            return false;
        }
        boolean[] concedido = {false};
        ultimoEnvio.compute(clave, (k, anterior) -> {
            if (anterior == null || !anterior.plus(ESPERA).isAfter(ahora)) {
                concedido[0] = true;
                return ahora;
            }
            return anterior;
        });
        if (!concedido[0]) {
            return false;
        }
        // Topes globales: si se agotaron, esta dirección no queda marcada como atendida
        if (!ventanas.intentar(CLAVE_HORA, maxPorHora, Duration.ofHours(1), ahora)) {
            ultimoEnvio.remove(clave);
            return false;
        }
        if (!ventanas.intentar(CLAVE_DIA, maxPorDia, Duration.ofDays(1), ahora)) {
            ventanas.devolver(CLAVE_HORA);
            ultimoEnvio.remove(clave);
            return false;
        }
        return true;
    }
}
