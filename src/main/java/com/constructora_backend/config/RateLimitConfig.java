package com.constructora_backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuración central de Rate Limiting.
 */
@Configuration
@EnableScheduling
public class RateLimitConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);

    // ───── Constantes de límites ajustadas para entorno de desarrollo/pruebas ─────
    public static final int AUTH_LIMIT = 10;
    public static final int PUBLIC_LIMIT = 100;
    public static final int GENERAL_LIMIT = 500;
    public static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public enum RateCategory {
        AUTH, PUBLIC, GENERAL, NONE
    }

    public Bucket resolveBucket(String clientIp, RateCategory category) {
        String key = clientIp + ":" + category.name();
        return buckets.computeIfAbsent(key, k -> createBucket(category));
    }

    public RateCategory categorize(String uri) {
        if (uri.startsWith("/api/auth/login") ||
                uri.startsWith("/api/auth/forgot-password") ||
                uri.startsWith("/api/auth/reset-password")) {
            return RateCategory.NONE;
        }

        if (uri.startsWith("/api/solicitudes-contacto/publico")) {
            return RateCategory.PUBLIC;
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
            case NONE -> Bandwidth.builder()
                    .capacity(Long.MAX_VALUE)
                    .refillGreedy(Long.MAX_VALUE, Duration.ofDays(1))
                    .build();
        };

        return Bucket.builder().addLimit(bandwidth).build();
    }

    @Scheduled(fixedRate = 600_000)
    public void purgeInactiveBuckets() {
        int sizeBefore = buckets.size();
        buckets.entrySet().removeIf(entry -> {
            Bucket bucket = entry.getValue();
            return bucket.getAvailableTokens() >= getCapacityForKey(entry.getKey());
        });
        int removed = sizeBefore - buckets.size();
        if (removed > 0) {
            log.debug("Rate Limiting: purgados {} buckets inactivos de {} totales", removed, sizeBefore);
        }
    }

    private long getCapacityForKey(String key) {
        if (key.endsWith(":AUTH")) return AUTH_LIMIT;
        if (key.endsWith(":PUBLIC")) return PUBLIC_LIMIT;
        if (key.endsWith(":GENERAL")) return GENERAL_LIMIT;
        return Long.MAX_VALUE;
    }
}