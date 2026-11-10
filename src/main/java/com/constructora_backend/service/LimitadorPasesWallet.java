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
 * Evita que el envío de la tarjeta de Google Wallet sirva para llenar de correos la bandeja de un tercero:
 * a cada dirección se le manda como máximo un correo por hora y hay un tope global por hora y por día, para que el abuso
 * repartido entre muchas direcciones tampoco queme la cuota del correo. Es independiente del de las confirmaciones del
 * formulario de contacto: abusar de uno no deja sin servicio al otro.
 */
@Component
public class LimitadorPasesWallet {

    static final Duration ESPERA = Duration.ofHours(1);
    static final int MAX_DIRECCIONES = 50_000;

    private static final String CLAVE_HORA = "wallet:hora";
    private static final String CLAVE_DIA = "wallet:dia";

    /** Motivo por el que no se pudo enviar. */
    public enum Resultado { PERMITIDO, DIRECCION_RECIENTE, TOPE_GLOBAL }

    private final Map<String, Instant> ultimoEnvio = new ConcurrentHashMap<>();
    private final LimitadorVentana ventanas;

    @Value("${google.wallet.max-por-hora:30}")
    private int maxPorHora = 30;
    @Value("${google.wallet.max-por-dia:200}")
    private int maxPorDia = 200;

    public LimitadorPasesWallet() {
        this(new LimitadorVentana());
    }

    @Autowired
    public LimitadorPasesWallet(LimitadorVentana ventanas) {
        this.ventanas = ventanas;
    }

    /** Para pruebas: topes globales explícitos. */
    LimitadorPasesWallet(LimitadorVentana ventanas, int maxPorHora, int maxPorDia) {
        this(ventanas);
        this.maxPorHora = maxPorHora;
        this.maxPorDia = maxPorDia;
    }

    public Resultado permitir(String correo) {
        return permitir(correo, Instant.now());
    }

    Resultado permitir(String correo, Instant ahora) {
        if (correo == null || correo.isBlank()) return Resultado.DIRECCION_RECIENTE;
        ultimoEnvio.values().removeIf(instante -> instante.plus(ESPERA).isBefore(ahora));
        String clave = correo.trim().toLowerCase(Locale.ROOT);
        if (ultimoEnvio.size() >= MAX_DIRECCIONES && !ultimoEnvio.containsKey(clave)) {
            return Resultado.TOPE_GLOBAL;
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
            return Resultado.DIRECCION_RECIENTE;
        }
        if (!ventanas.intentar(CLAVE_HORA, maxPorHora, Duration.ofHours(1), ahora)) {
            ultimoEnvio.remove(clave);
            return Resultado.TOPE_GLOBAL;
        }
        if (!ventanas.intentar(CLAVE_DIA, maxPorDia, Duration.ofDays(1), ahora)) {
            ventanas.devolver(CLAVE_HORA);
            ultimoEnvio.remove(clave);
            return Resultado.TOPE_GLOBAL;
        }
        return Resultado.PERMITIDO;
    }

    /** Si el envío falló después de reservar el cupo, la dirección puede volver a intentarlo. */
    public void liberar(String correo) {
        if (correo != null) ultimoEnvio.remove(correo.trim().toLowerCase(Locale.ROOT));
    }
}
