package com.constructora_backend.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evita que el formulario público sirva para llenar de correos la bandeja de un tercero:
 * a cada dirección se le envía como máximo una confirmación por hora.
 */
@Component
public class LimitadorConfirmaciones {

    static final Duration ESPERA = Duration.ofHours(1);

    private final Map<String, Instant> ultimoEnvio = new ConcurrentHashMap<>();

    /** true si ya se puede enviar a esta dirección (y deja registrado el envío); false si se envió hace menos de una hora. */
    public boolean permitir(String correo) {
        return permitir(correo, Instant.now());
    }

    boolean permitir(String correo, Instant ahora) {
        if (correo == null || correo.isBlank()) return false;
        // Se limpian los registros viejos para que el mapa no crezca sin fin
        ultimoEnvio.values().removeIf(instante -> instante.plus(ESPERA).isBefore(ahora));

        String clave = correo.trim().toLowerCase(Locale.ROOT);
        boolean[] concedido = {false};
        ultimoEnvio.compute(clave, (k, anterior) -> {
            if (anterior == null || !anterior.plus(ESPERA).isAfter(ahora)) {
                concedido[0] = true;
                return ahora;
            }
            return anterior;
        });
        return concedido[0];
    }
}
