package com.constructora_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro HTTP que gestiona el Correlation ID (Request ID) para observabilidad.
 * Extrae el header 'X-Correlation-ID' si viene del cliente/gateway o genera un nuevo UUID,
 * lo inyecta en el contexto MDC de SLF4J (para que cada log contenga el correlationId),
 * y lo añade a la respuesta HTTP como header de retorno.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_KEY = "correlationId";

    private static final java.util.regex.Pattern FORMATO_VALIDO = java.util.regex.Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    /** Letras, números y guiones, de 1 a 64 caracteres (cubre los UUID); cualquier otra cosa se reemplaza por uno generado. */
    public static boolean esValido(String correlationId) {
        return correlationId != null && FORMATO_VALIDO.matcher(correlationId).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        // El valor llega de fuera y va a los registros y a la respuesta: solo se acepta uno con forma de identificador
        if (!esValido(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }
}
