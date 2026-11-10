package com.constructora_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Deja entrar a /actuator/prometheus solo a quien presente el token de métricas (variable METRICAS_TOKEN).
 * Prometheus lo envía como "Authorization: Bearer ..." (ver prometheus/prometheus.yml). Si la variable está vacía,
 * las métricas quedan cerradas para todos. El token solo sirve para esa ruta: no da acceso a nada más.
 */
@Component
public class MetricasTokenFilter extends OncePerRequestFilter {

    static final String RUTA = "/actuator/prometheus";

    private final byte[] tokenEsperado;

    public MetricasTokenFilter(@Value("${app.seguridad.metricas-token:}") String token) {
        this.tokenEsperado = token == null ? new byte[0] : token.trim().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !RUTA.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (tokenEsperado.length > 0 && cabecera != null && cabecera.startsWith("Bearer ")) {
            byte[] recibido = cabecera.substring(7).trim().getBytes(StandardCharsets.UTF_8);
            // Comparación en tiempo constante, para no filtrar el token por diferencias de tiempo de respuesta
            if (MessageDigest.isEqual(tokenEsperado, recibido)) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "prometheus", null, List.of(new SimpleGrantedAuthority("ROLE_METRICAS"))));
            }
        }
        chain.doFilter(request, response);
    }
}
