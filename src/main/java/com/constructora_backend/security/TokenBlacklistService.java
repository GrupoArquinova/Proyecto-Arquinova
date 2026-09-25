package com.constructora_backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Servicio distribuido de revocación de sesiones JWT (Token Blacklist).
 * Utiliza Redis con TTL automático para escalabilidad horizontal entre múltiples pods/instancias.
 * Incluye fallback transparente en memoria para entornos de testing y resiliencia offline.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private final StringRedisTemplate redisTemplate;
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

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "revoked", ttlMillis, TimeUnit.MILLISECONDS);
                log.debug("Token revocado en Redis con TTL de {} ms", ttlMillis);
                return;
            } catch (Exception ex) {
                log.warn("Error al conectar con Redis para revocar token, usando fallback en memoria: {}", ex.getMessage());
            }
        }

        localBlacklist.put(token, expiracionMillis);
    }

    public boolean estaRevocado(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        if (redisTemplate != null) {
            try {
                Boolean hasKey = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
                if (Boolean.TRUE.equals(hasKey)) {
                    return true;
                }
            } catch (Exception ex) {
                log.warn("Error al verificar token en Redis, verificando fallback en memoria: {}", ex.getMessage());
            }
        }

        Long expira = localBlacklist.get(token);
        if (expira == null) {
            return false;
        }

        if (System.currentTimeMillis() > expira) {
            localBlacklist.remove(token);
            return false;
        }

        return true;
    }
}
