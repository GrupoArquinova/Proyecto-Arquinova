package com.constructora_backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio distribuido de mitigación contra ataques de fuerza bruta y diccionario.
 * <p>
 * Cuenta los fallos de inicio de sesión de dos maneras:
 * <ul>
 *   <li>Por correo + IP: 5 fallos seguidos en 15 minutos bloquean ese par. Así quien adivina contraseñas desde una IP
 *       no puede dejar fuera a la persona real que entra desde otra.</li>
 *   <li>Por correo en general: un umbral mucho más alto (30) frena los ataques repartidos entre muchas IP sin que
 *       bloquear una cuenta sea trivial.</li>
 * </ul>
 * Soporta Redis distribuido para arquitecturas multi-pod con fallback transparente en memoria.
 */
@Service
@Slf4j
public class LoginAttemptService {

    public static final int MAX_INTENTOS = 5;
    /** Umbral por correo sumando todas las IP (ataque distribuido). */
    public static final int MAX_INTENTOS_GLOBAL = 30;
    public static final int BLOQUEO_MINUTOS = 15;
    /** Tope de claves en memoria cuando no hay Redis (las claves salen de datos que escribe quien llama). */
    static final int MAX_CLAVES_LOCALES = 50_000;

    private static final String ATTEMPTS_PREFIX = "login:attempts:";
    private static final String BLOCKED_PREFIX = "login:blocked:";

    private final StringRedisTemplate redisTemplate;

    private static class IntentoInfo {
        int intentos;
        LocalDateTime ventanaDesde = LocalDateTime.now();
        LocalDateTime bloqueadoHasta;
    }

    private final Map<String, IntentoInfo> intentosLocales = new ConcurrentHashMap<>();
    private volatile long ultimaPurgaMs;

    public LoginAttemptService() {
        this(null);
    }

    @Autowired
    public LoginAttemptService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static String claveCorreo(String correo) {
        return "c:" + correo.trim().toLowerCase(Locale.ROOT);
    }

    private static String clavePar(String correo, String ip) {
        return "p:" + correo.trim().toLowerCase(Locale.ROOT) + "|" + (ip == null || ip.isBlank() ? "-" : ip.trim());
    }

    /** Registra un intento fallido de este correo desde esta IP. */
    public void registrarFallo(String correo, String ip) {
        if (correo == null || correo.isBlank()) return;
        contar(clavePar(correo, ip), MAX_INTENTOS);
        contar(claveCorreo(correo), MAX_INTENTOS_GLOBAL);
    }

    private void contar(String key, int maximo) {
        if (redisTemplate != null) {
            try {
                String attemptKey = ATTEMPTS_PREFIX + key;
                Long count = redisTemplate.opsForValue().increment(attemptKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(attemptKey, Duration.ofMinutes(BLOQUEO_MINUTOS));
                }
                if (count != null && count >= maximo) {
                    redisTemplate.opsForValue().set(BLOCKED_PREFIX + key, "blocked", Duration.ofMinutes(BLOQUEO_MINUTOS));
                }
                return;
            } catch (Exception ex) {
                log.warn("Error al registrar fallo de login en Redis, recurriendo a memoria local: {}", ex.getMessage());
            }
        }

        // Fallback local
        if (intentosLocales.size() >= MAX_CLAVES_LOCALES) {
            purgarSiHaPasadoUnSegundo();
            if (intentosLocales.size() >= MAX_CLAVES_LOCALES && !intentosLocales.containsKey(key)) {
                return;
            }
        }
        LocalDateTime ahora = LocalDateTime.now();
        intentosLocales.compute(key, (k, info) -> {
            if (info == null) {
                info = new IntentoInfo();
            }
            // La ventana de conteo y el bloqueo duran 15 minutos desde el primer fallo / desde el bloqueo
            boolean reiniciar = info.bloqueadoHasta != null
                    ? ahora.isAfter(info.bloqueadoHasta)
                    : info.ventanaDesde.plusMinutes(BLOQUEO_MINUTOS).isBefore(ahora);
            if (reiniciar) {
                info.intentos = 0;
                info.bloqueadoHasta = null;
                info.ventanaDesde = ahora;
            }
            info.intentos++;
            if (info.intentos >= maximo) {
                info.bloqueadoHasta = ahora.plusMinutes(BLOQUEO_MINUTOS);
            }
            return info;
        });
    }

    /** Tras un inicio de sesión correcto se borran los contadores del par correo+IP y los del correo. */
    public void limpiarIntentos(String correo, String ip) {
        if (correo == null || correo.isBlank()) return;
        for (String key : new String[]{clavePar(correo, ip), claveCorreo(correo)}) {
            if (redisTemplate != null) {
                try {
                    redisTemplate.delete(ATTEMPTS_PREFIX + key);
                    redisTemplate.delete(BLOCKED_PREFIX + key);
                } catch (Exception ex) {
                    log.warn("Error al limpiar intentos en Redis: {}", ex.getMessage());
                }
            }
            intentosLocales.remove(key);
        }
    }

    /** true si ese correo está bloqueado desde esa IP, o si acumuló demasiados fallos desde cualquier IP. */
    public boolean estaBloqueado(String correo, String ip) {
        if (correo == null || correo.isBlank()) return false;
        return bloqueada(clavePar(correo, ip)) || bloqueada(claveCorreo(correo));
    }

    private boolean bloqueada(String key) {
        if (redisTemplate != null) {
            try {
                Boolean isBlocked = redisTemplate.hasKey(BLOCKED_PREFIX + key);
                if (Boolean.TRUE.equals(isBlocked)) {
                    return true;
                }
            } catch (Exception ex) {
                log.warn("Error al verificar bloqueo en Redis, comprobando memoria local: {}", ex.getMessage());
            }
        }

        IntentoInfo info = intentosLocales.get(key);
        if (info == null || info.bloqueadoHasta == null) {
            return false;
        }
        if (LocalDateTime.now().isBefore(info.bloqueadoHasta)) {
            return true;
        }
        intentosLocales.remove(key);
        return false;
    }

    /** Con el mapa lleno, purga a lo sumo una vez por segundo: así llenarlo no obliga a recorrerlo entero en cada intento. */
    private void purgarSiHaPasadoUnSegundo() {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimaPurgaMs >= 1000) {
            ultimaPurgaMs = ahora;
            purgarVencidos();
        }
    }

    /** Quita de memoria los contadores cuya ventana de 15 minutos ya pasó. */
    @Scheduled(fixedRate = 600_000)
    public void purgarVencidos() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(BLOQUEO_MINUTOS);
        intentosLocales.values().removeIf(info -> info.ventanaDesde.isBefore(limite)
                && (info.bloqueadoHasta == null || info.bloqueadoHasta.isBefore(LocalDateTime.now())));
    }

    int tamanoLocal() {
        return intentosLocales.size();
    }
}
