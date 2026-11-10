package com.constructora_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador de "máximo N veces por ventana de tiempo" para una clave cualquiera (por ejemplo "recuperacion:correo" o
 * "chat:dia"). Usa Redis si está disponible, para que el límite valga entre varias instancias; si no, memoria local con
 * tope de claves y purga de las vencidas. Si Redis falla a mitad de camino se cae a la memoria local.
 */
@Component
@Slf4j
public class LimitadorVentana {

    private static final String PREFIJO = "limite:";
    /** Tope de claves en memoria: las claves pueden salir de datos que escribe quien llama (un correo, por ejemplo). */
    static final int MAX_CLAVES_LOCALES = 50_000;

    private record Ventana(int cuenta, Instant expira) {
    }

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Ventana> locales = new ConcurrentHashMap<>();
    private volatile Instant ultimaPurga;

    public LimitadorVentana() {
        this(null);
    }

    @Autowired
    public LimitadorVentana(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** true y cuenta el uso si todavía hay cupo en la ventana; false si ya se alcanzó el máximo. */
    public boolean intentar(String clave, int maximo, Duration ventana) {
        return intentar(clave, maximo, ventana, Instant.now());
    }

    boolean intentar(String clave, int maximo, Duration ventana, Instant ahora) {
        if (maximo <= 0) {
            return false;
        }
        if (redisTemplate != null) {
            try {
                String key = PREFIJO + clave;
                Long cuenta = redisTemplate.opsForValue().increment(key);
                if (cuenta != null) {
                    if (cuenta == 1) {
                        redisTemplate.expire(key, ventana);
                    }
                    return cuenta <= maximo;
                }
            } catch (Exception ex) {
                log.warn("Redis no respondió al contar un límite; se usa la memoria local: {}", ex.getMessage());
            }
        }
        if (locales.size() >= MAX_CLAVES_LOCALES) {
            purgarSiHaPasadoUnSegundo(ahora);
            if (locales.size() >= MAX_CLAVES_LOCALES && !locales.containsKey(clave)) {
                // Sin sitio para una clave nueva: por prudencia se niega
                return false;
            }
        }
        boolean[] concedido = {false};
        locales.compute(clave, (k, actual) -> {
            if (actual == null || !actual.expira().isAfter(ahora)) {
                concedido[0] = true;
                return new Ventana(1, ahora.plus(ventana));
            }
            if (actual.cuenta() < maximo) {
                concedido[0] = true;
                return new Ventana(actual.cuenta() + 1, actual.expira());
            }
            return actual;
        });
        return concedido[0];
    }

    /** Devuelve un uso de la ventana (por ejemplo cuando lo que se iba a hacer finalmente no se hizo). */
    public void devolver(String clave) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().decrement(PREFIJO + clave);
                return;
            } catch (Exception ex) {
                log.warn("Redis no respondió al devolver un uso: {}", ex.getMessage());
            }
        }
        locales.computeIfPresent(clave, (k, actual) ->
                actual.cuenta() > 1 ? new Ventana(actual.cuenta() - 1, actual.expira()) : null);
    }

    /** Con el mapa lleno, purga a lo sumo una vez por segundo: así llenarlo no obliga a recorrerlo entero en cada llamada. */
    private void purgarSiHaPasadoUnSegundo(Instant ahora) {
        Instant ultima = ultimaPurga;
        if (ultima == null || !ahora.isBefore(ultima.plusSeconds(1))) {
            ultimaPurga = ahora;
            purgarVencidos(ahora);
        }
    }

    @Scheduled(fixedRate = 600_000)
    public void purgarVencidos() {
        purgarVencidos(Instant.now());
    }

    void purgarVencidos(Instant ahora) {
        locales.values().removeIf(ventana -> !ventana.expira().isAfter(ahora));
    }

    int tamanoLocal() {
        return locales.size();
    }
}
