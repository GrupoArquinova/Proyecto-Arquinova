package com.constructora_backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Servicio distribuido de revocación de sesiones JWT (Token Blacklist).
 * Utiliza Redis con TTL automático para escalabilidad horizontal entre múltiples pods/instancias.
 * Incluye fallback transparente en memoria para entornos de testing y resiliencia offline.
 * Nunca guarda el token completo: solo su huella SHA-256, así un volcado de Redis o de memoria no entrega tokens usables.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private final StringRedisTemplate redisTemplate;
    /** Huella del token → momento en que vence (milisegundos). */
    private final Map<String, Long> localBlacklist = new ConcurrentHashMap<>();

    public TokenBlacklistService() {
        this(null);
    }

    @Autowired
    public TokenBlacklistService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revocarToken(String token, long expiracionMillis) {
        if (token == null || token.isBlank()) {
            return;
        }

        long ttlMillis = expiracionMillis - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }

        String huella = huella(token);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + huella, "revoked", ttlMillis, TimeUnit.MILLISECONDS);
                log.debug("Token revocado en Redis con TTL de {} ms", ttlMillis);
                return;
            } catch (Exception ex) {
                log.warn("Error al conectar con Redis para revocar token, usando fallback en memoria: {}", ex.getMessage());
            }
        }

        localBlacklist.put(huella, expiracionMillis);
    }

    public boolean estaRevocado(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String huella = huella(token);
        if (redisTemplate != null) {
            try {
                Boolean hasKey = redisTemplate.hasKey(BLACKLIST_PREFIX + huella);
                if (Boolean.TRUE.equals(hasKey)) {
                    return true;
                }
            } catch (Exception ex) {
                log.warn("Error al verificar token en Redis, verificando fallback en memoria: {}", ex.getMessage());
            }
        }

        Long expira = localBlacklist.get(huella);
        if (expira == null) {
            return false;
        }
        if (System.currentTimeMillis() > expira) {
            localBlacklist.remove(huella);
            return false;
        }
        return true;
    }

    /** Quita de la memoria las revocaciones de tokens que ya vencieron solos, para que el mapa no crezca sin fin. */
    @Scheduled(fixedRate = 600_000)
    public void purgarVencidos() {
        long ahora = System.currentTimeMillis();
        int antes = localBlacklist.size();
        localBlacklist.values().removeIf(expira -> expira < ahora);
        if (antes != localBlacklist.size()) {
            log.debug("Lista de revocados: purgados {} tokens vencidos", antes - localBlacklist.size());
        }
    }

    /** Cantidad de revocaciones que hay en memoria (para pruebas y métricas). */
    int tamanoLocal() {
        return localBlacklist.size();
    }

    /** SHA-256 en hexadecimal del token. */
    static String huella(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
