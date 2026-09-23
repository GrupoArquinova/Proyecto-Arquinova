package com.constructora_backend.security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de mitigación contra ataques de fuerza bruta y diccionario.
 * Bloquea temporalmente el inicio de sesión para un correo específico si acumula
 * 5 intentos fallidos consecutivos durante un período de 15 minutos.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_INTENTOS = 5;
    public static final int BLOQUEO_MINUTOS = 15;

    private static class IntentoInfo {
        int intentos;
        LocalDateTime ultimoFallo;
        LocalDateTime bloqueadoHasta;

        IntentoInfo() {
            this.intentos = 1;
            this.ultimoFallo = LocalDateTime.now();
        }
    }

    private final Map<String, IntentoInfo> intentosPorUsuario = new ConcurrentHashMap<>();

    public void registrarFallo(String correo) {
        if (correo == null || correo.isBlank()) return;
        String key = correo.trim().toLowerCase();

        intentosPorUsuario.compute(key, (k, info) -> {
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
        if (correo != null) {
            intentosPorUsuario.remove(correo.trim().toLowerCase());
        }
    }

    public boolean estaBloqueado(String correo) {
        if (correo == null || correo.isBlank()) return false;
        String key = correo.trim().toLowerCase();
        IntentoInfo info = intentosPorUsuario.get(key);
        if (info == null || info.bloqueadoHasta == null) {
            return false;
        }

        if (LocalDateTime.now().isBefore(info.bloqueadoHasta)) {
            return true;
        }

        intentosPorUsuario.remove(key);
        return false;
    }
}
