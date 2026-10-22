package com.constructora_backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuración central de Rate Limiting.
 * <p>
 * Cada IP tiene un cupo por categoría. Además, el chat y el formulario de contacto tienen un cupo global (de todas las
 * IP juntas) como respaldo: protege el gasto de la IA y la bandeja de entrada aunque alguien reparta el abuso entre
 * muchas direcciones. El mapa de cupos por IP tiene un tope de entradas y las inactivas expiran, así que no se puede
 * llenar la memoria variando la IP.
 */
@Configuration
@EnableScheduling
public class RateLimitConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);

    // ───── Constantes de límites ajustadas para entorno de desarrollo/pruebas ─────
    /** Inicio de sesión, recuperación y restablecimiento de contraseña y renovación de sesión: por IP y minuto. */
    public static final int AUTH_LIMIT = 10;
    public static final int PUBLIC_LIMIT = 100;
    public static final int GENERAL_LIMIT = 500;
    /** El asistente de IA gasta cuota de pago por uso: pocas preguntas por minuto y por IP. */
    public static final int CHAT_LIMIT = 10;
    /** Respaldo global (todas las IP juntas) por minuto para el chat y para el formulario de contacto. */
    public static final int CHAT_GLOBAL_LIMIT = 120;
    public static final int PUBLIC_GLOBAL_LIMIT = 60;
    public static final Duration WINDOW = Duration.ofMinutes(1);

    /** Tope de cupos por IP que se guardan a la vez, y tiempo sin uso tras el cual un cupo se descarta. */
    public static final int MAX_BUCKETS = 20_000;
    static final long INACTIVIDAD_MS = Duration.ofMinutes(10).toMillis();

    private static final class Entrada {
        final Bucket bucket;
        volatile long ultimoUso;

        Entrada(Bucket bucket) {
            this.bucket = bucket;
            this.ultimoUso = System.currentTimeMillis();
        }
    }

    private final int maxBuckets;
    /** Con orden de acceso: cuando se llena, sale primero el menos usado recientemente. */
    private final Map<String, Entrada> buckets;
    private final Map<RateCategory, Bucket> globales = new ConcurrentHashMap<>();

    public enum RateCategory {
        AUTH, PUBLIC, GENERAL, CHAT, NONE
    }

    public RateLimitConfig() {
        this(MAX_BUCKETS);
    }

    RateLimitConfig(int maxBuckets) {
        this.maxBuckets = maxBuckets;
        this.buckets = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entrada> eldest) {
                return size() > RateLimitConfig.this.maxBuckets;
            }
        };
    }

    public Bucket resolveBucket(String clientIp, RateCategory category) {
        String key = clientIp + ":" + category.name();
        synchronized (buckets) {
            Entrada entrada = buckets.computeIfAbsent(key, k -> new Entrada(createBucket(category)));
            entrada.ultimoUso = System.currentTimeMillis();
            return entrada.bucket;
        }
    }

    /** Cupo global de la categoría (todas las IP juntas), o null si esa categoría no tiene respaldo global. */
    public Bucket resolveGlobalBucket(RateCategory category) {
        long limite = limiteGlobal(category);
        if (limite <= 0) {
            return null;
        }
        return globales.computeIfAbsent(category, c -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(limite).refillGreedy(limite, WINDOW).build())
                .build());
    }

    public static long limiteGlobal(RateCategory category) {
        return switch (category) {
            case CHAT -> CHAT_GLOBAL_LIMIT;
            case PUBLIC -> PUBLIC_GLOBAL_LIMIT;
            default -> 0;
        };
    }

    public RateCategory categorize(String uri) {
        // Inicio de sesión, recuperación y restablecimiento de contraseña y renovación: fuerza bruta y abuso del correo
        if (uri.startsWith("/api/auth/login") ||
                uri.startsWith("/api/auth/forgot-password") ||
                uri.startsWith("/api/auth/reset-password") ||
                uri.startsWith("/api/auth/renovar")) {
            return RateCategory.AUTH;
        }

        if (uri.startsWith("/api/solicitudes-contacto/publico")) {
            return RateCategory.PUBLIC;
        }

        if (uri.startsWith("/api/chat")) {
            return RateCategory.CHAT;
        }

        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui")) {
            return RateCategory.NONE;
        }

        if (uri.startsWith("/api/")) {
            return RateCategory.GENERAL;
        }

        return RateCategory.NONE;
    }

    private Bucket createBucket(RateCategory category) {
        Bandwidth bandwidth = switch (category) {
            case AUTH -> Bandwidth.builder()
                    .capacity(AUTH_LIMIT)
                    .refillGreedy(AUTH_LIMIT, WINDOW)
                    .build();
            case PUBLIC -> Bandwidth.builder()
                    .capacity(PUBLIC_LIMIT)
                    .refillGreedy(PUBLIC_LIMIT, WINDOW)
                    .build();
            case GENERAL -> Bandwidth.builder()
                    .capacity(GENERAL_LIMIT)
                    .refillGreedy(GENERAL_LIMIT, WINDOW)
                    .build();
            case CHAT -> Bandwidth.builder()
                    .capacity(CHAT_LIMIT)
                    .refillGreedy(CHAT_LIMIT, WINDOW)
                    .build();
            case NONE -> Bandwidth.builder()
                    .capacity(Long.MAX_VALUE)
                    .refillGreedy(Long.MAX_VALUE, Duration.ofDays(1))
                    .build();
        };

        return Bucket.builder().addLimit(bandwidth).build();
    }

    /** Descarta los cupos que llevan un rato sin usarse (ya recuperaron su capacidad completa o nadie los toca). */
    @Scheduled(fixedRate = 600_000)
    public void purgeInactiveBuckets() {
        long ahora = System.currentTimeMillis();
        int removed;
        int sizeBefore;
        synchronized (buckets) {
            sizeBefore = buckets.size();
            buckets.entrySet().removeIf(entry ->
                    ahora - entry.getValue().ultimoUso > INACTIVIDAD_MS
                            || entry.getValue().bucket.getAvailableTokens() >= getCapacityForKey(entry.getKey()));
            removed = sizeBefore - buckets.size();
        }
        if (removed > 0) {
            log.debug("Rate Limiting: purgados {} buckets inactivos de {} totales", removed, sizeBefore);
        }
    }

    /** Cantidad de cupos por IP que hay guardados (para pruebas y métricas). */
    public int tamanoBuckets() {
        synchronized (buckets) {
            return buckets.size();
        }
    }

    private long getCapacityForKey(String key) {
        if (key.endsWith(":AUTH")) return AUTH_LIMIT;
        if (key.endsWith(":PUBLIC")) return PUBLIC_LIMIT;
        if (key.endsWith(":GENERAL")) return GENERAL_LIMIT;
        if (key.endsWith(":CHAT")) return CHAT_LIMIT;
        return Long.MAX_VALUE;
    }
}
