package com.constructora_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rechaza con 413 las peticiones a /api/** cuyo cuerpo supere el tope (64 KB por defecto). Ningún endpoint recibe archivos
 * (las imágenes van directo a Cloudinary con subida firmada): todo es JSON pequeño, así que un cuerpo grande solo sirve
 * para gastar memoria. Si la petición no declara Content-Length (transferencia por partes) se cuenta mientras se lee y se
 * corta al pasar el tope.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class LimiteTamanoFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LimiteTamanoFilter.class);

    /** 64 KB. */
    public static final long MAX_BYTES_POR_DEFECTO = 64 * 1024;

    @Value("${app.seguridad.max-cuerpo-bytes:65536}")
    private long maxBytes = MAX_BYTES_POR_DEFECTO;

    public LimiteTamanoFilter() {
    }

    public LimiteTamanoFilter(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long declarado = request.getContentLengthLong();
        if (declarado > maxBytes) {
            log.warn("Petición rechazada por tamaño ({} bytes) en {}", declarado, request.getRequestURI().replaceAll("[\\r\\n]", "_"));
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":413,\"error\":\"Payload Too Large\",\"mensaje\":\"El cuerpo de la petición supera el tamaño máximo permitido\"}");
            return;
        }
        if (declarado < 0 && request.getHeader("Transfer-Encoding") != null) {
            filterChain.doFilter(new PeticionLimitada(request, maxBytes), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Cuenta los bytes que se leen y corta la lectura al pasar el tope. */
    private static final class PeticionLimitada extends HttpServletRequestWrapper {

        private final long tope;
        private ServletInputStream flujo;

        PeticionLimitada(HttpServletRequest request, long tope) {
            super(request);
            this.tope = tope;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (flujo == null) {
                flujo = new FlujoLimitado(super.getInputStream(), tope);
            }
            return flujo;
        }
    }

    private static final class FlujoLimitado extends ServletInputStream {

        private final ServletInputStream origen;
        private final long tope;
        private long leidos;

        FlujoLimitado(ServletInputStream origen, long tope) {
            this.origen = origen;
            this.tope = tope;
        }

        private void contar(long n) throws IOException {
            if (n > 0) {
                leidos += n;
                if (leidos > tope) {
                    throw new IOException("El cuerpo de la petición supera el tamaño máximo permitido");
                }
            }
        }

        @Override
        public int read() throws IOException {
            int b = origen.read();
            if (b >= 0) contar(1);
            return b;
        }

        @Override
        public int read(byte[] buffer, int desde, int largo) throws IOException {
            int n = origen.read(buffer, desde, largo);
            contar(n);
            return n;
        }

        @Override
        public boolean isFinished() {
            return origen.isFinished();
        }

        @Override
        public boolean isReady() {
            return origen.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            origen.setReadListener(listener);
        }
    }
}
