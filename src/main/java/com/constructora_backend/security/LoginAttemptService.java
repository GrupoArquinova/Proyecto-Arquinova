package com.constructora_backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio distribuido de mitigación contra ataques de fuerza bruta y diccionario.
 * Bloquea temporalmente el inicio de sesión para un correo específico si acumula
 * 5 intentos fallidos consecutivos durante un período de 15 minutos.
 * Soporta Redis distribuido para arquitecturas multi-pod con fallback transparente en memoria.
 */
@Service
@Slf4j
public class LoginAttemptService {

    public static final int MAX_INTENTOS = 5;
    public static final int BLOQUEO_MINUTOS = 15;

    private static final String ATTEMPTS_PREFIX = "login:attempts:";
    private static final String BLOCKED_PREFIX = "login:blocked:";

    private final StringRedisTemplate redisTemplate;

    private static class IntentoInfo {
        int intentos;
        LocalDateTime ultimoFallo;
        LocalDateTime bloqueadoHasta;

        IntentoInfo() {
            this.intentos = 1;
            this.ultimoFallo = LocalDateTime.now();
        }
    }

    private final Map<String, IntentoInfo> intentosLocales = new ConcurrentHashMap<>();

    public LoginAttemptService() {
        this(null);
    }

    @Autowired
    public LoginAttemptService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public void registrarFallo(String correo) {
        if (correo == null || correo.isBlank()) return;
        String key = correo.trim().toLowerCase();

        if (redisTemplate != null) {
            try {
                String attemptKey = ATTEMPTS_PREFIX + key;
                Long count = redisTemplate.opsForValue().increment(attemptKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(attemptKey, Duration.ofMinutes(BLOQUEO_MINUTOS));
                }

                if (count != null && count >= MAX_INTENTOS) {
                    redisTemplate.opsForValue().set(BLOCKED_PREFIX + key, "blocked", Duration.ofMinutes(BLOQUEO_MINUTOS));
                }
                return;
            } catch (Exception ex) {
                log.warn("Error al registrar fallo de login en Redis, recurriendo a memoria local: {}", ex.getMessage());
            }
        }

        // Fallback local
        intentosLocales.compute(key, (k, info) -> {
            if (info == null) {
                return new IntentoInfo();
            }
            if (info.bloqueadoHasta != null && LocalDateTime.now().isAfter(info.bloqueadoHasta)) {
                info.intentos = 1;
                info.bloqueadoHasta = null;
                info.ultimoFallo = LocalDateTime.now();
                return info;
            }

            info.intentos++;
            info.ultimoFallo = LocalDateTime.now();

            if (info.intentos >= MAX_INTENTOS) {
                info.bloqueadoHasta = LocalDateTime.now().plusMinutes(BLOQUEO_MINUTOS);
            }
            return info;
        });
    }

    public void limpiarIntentos(String correo) {
        if (correo == null || correo.isBlank()) return;
        String key = correo.trim().toLowerCase();

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

    public boolean estaBloqueado(String correo) {
        if (correo == null || correo.isBlank()) return false;
        String key = correo.trim().toLowerCase();

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
}
