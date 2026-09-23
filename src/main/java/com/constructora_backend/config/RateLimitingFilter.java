package com.constructora_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filtro HTTP de Rate Limiting que se ejecuta ANTES de la autenticación JWT.
 *
 * Identifica al cliente por IP, clasifica la ruta en una categoría de límite,
 * y aplica el algoritmo Token Bucket para decidir si la petición pasa o se rechaza
 * con HTTP 429 Too Many Requests.
 *
 * Headers de respuesta incluidos:
 * - X-RateLimit-Remaining: tokens disponibles tras esta petición
 * - X-RateLimit-Limit: capacidad máxima del bucket
 * - Retry-After: segundos hasta que se libere un token (solo en 429)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public RateLimitingFilter(@org.springframework.beans.factory.annotation.Autowired(required = false) RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig != null ? rateLimitConfig : new RateLimitConfig();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        RateLimitConfig.RateCategory category = rateLimitConfig.categorize(uri);

        // Si la categoría es NONE, no aplicar rate limiting
        if (category == RateLimitConfig.RateCategory.NONE) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = rateLimitConfig.resolveBucket(clientIp, category);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // Agregar headers informativos de Rate Limiting
        long limit = getLimitForCategory(category);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            // La petición fue aceptada, continuar la cadena de filtros
            filterChain.doFilter(request, response);
        } else {
            // Límite excedido → 429 Too Many Requests
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000 + 1;

            log.warn("Rate Limit excedido: IP={}, ruta={}, categoría={}, retry en {}s",
                    clientIp, uri, category, waitSeconds);

            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", LocalDateTime.now().toString());
            errorBody.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            errorBody.put("error", "Too Many Requests");
            errorBody.put("mensaje", "Has excedido el límite de peticiones. Intenta de nuevo en " + waitSeconds + " segundos.");
            errorBody.put("path", uri);

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
        }
    }

    /**
     * Resuelve la IP real del cliente, considerando proxies/load balancers.
     */
    private String resolveClientIp(HttpServletRequest request) {
        // Verificar headers de proxy en orden de confiabilidad
        String[] headerNames = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For puede contener múltiples IPs: "clientIP, proxy1, proxy2"
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Retorna la capacidad máxima de un bucket según su categoría.
     */
    private long getLimitForCategory(RateLimitConfig.RateCategory category) {
        return switch (category) {
            case AUTH -> RateLimitConfig.AUTH_LIMIT;
            case PUBLIC -> RateLimitConfig.PUBLIC_LIMIT;
            case GENERAL -> RateLimitConfig.GENERAL_LIMIT;
            case NONE -> Long.MAX_VALUE;
        };
    }
}
