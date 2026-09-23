package com.constructora_backend.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de revocación de sesiones JWT (Token Blacklist).
 * Permite invalidar tokens al invocar el endpoint de Logout para que no puedan
 * ser reutilizados incluso antes de su tiempo de expiración original.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    public void revocarToken(String token, long expiracionMillis) {
        if (token != null && !token.isBlank()) {
            blacklistedTokens.put(token, expiracionMillis);
        }
    }

    public boolean estaRevocado(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long expira = blacklistedTokens.get(token);
        if (expira == null) {
            return false;
        }

        if (System.currentTimeMillis() > expira) {
            blacklistedTokens.remove(token);
            return false;
        }

        return true;
    }
}
