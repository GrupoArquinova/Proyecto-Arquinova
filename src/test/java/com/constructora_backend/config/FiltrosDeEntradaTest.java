package com.constructora_backend.config;

import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Filtros de entrada: tamaño máximo del cuerpo (413) y validación del identificador de correlación. */
class FiltrosDeEntradaTest {

    // ───────── Tamaño del cuerpo ─────────

    private MockHttpServletRequest conCuerpo(String uri, int bytes) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setContent(new byte[bytes]);
        return request;
    }

    @Test
    @DisplayName("Un cuerpo de más de 64 KB en /api/** se rechaza con 413 sin llegar al controlador")
    void cuerpoGrandeSeRechazaCon413() throws Exception {
        LimiteTamanoFilter filtro = new LimiteTamanoFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(conCuerpo("/api/proyectos", 64 * 1024 + 1), response, cadena);

        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("Payload Too Large"));
        assertNull(cadena.getRequest(), "la petición no avanzó por la cadena");
    }

    @Test
    @DisplayName("Un cuerpo de hasta 64 KB pasa")
    void cuerpoDentroDelTopePasa() throws Exception {
        LimiteTamanoFilter filtro = new LimiteTamanoFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(conCuerpo("/api/proyectos", 64 * 1024), response, cadena);

        assertEquals(200, response.getStatus());
        assertNotNull(cadena.getRequest());
    }

    @Test
    @DisplayName("Fuera de /api/** no se aplica el tope")
    void fueraDeApiNoSeAplica() throws Exception {
        LimiteTamanoFilter filtro = new LimiteTamanoFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(conCuerpo("/actuator/health", 200_000), response, cadena);

        assertEquals(200, response.getStatus());
        assertNotNull(cadena.getRequest());
    }

    @Test
    @DisplayName("Sin Content-Length (transferencia por partes) se cuenta lo que se lee y se corta al pasar el tope")
    void cuerpoPorPartesSeCortaAlPasarElTope() throws Exception {
        LimiteTamanoFilter filtro = new LimiteTamanoFilter(100);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/proyectos") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.addHeader("Transfer-Encoding", "chunked");
        request.setContent(new byte[500]);
        AtomicReference<Throwable> error = new AtomicReference<>();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, (req, res) -> {
            try {
                ServletInputStream in = req.getInputStream();
                byte[] buffer = new byte[64];
                while (in.read(buffer) >= 0) {
                    // leyendo
                }
            } catch (IOException e) {
                error.set(e);
            }
        });

        assertNotNull(error.get(), "la lectura debe cortarse al superar el tope");
    }

    // ───────── Correlation ID ─────────

    private String correlationIdTrasFiltrar(String encabezado) throws Exception {
        CorrelationIdFilter filtro = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/proyectos");
        if (encabezado != null) request.addHeader("X-Correlation-ID", encabezado);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> enMdc = new AtomicReference<>();

        filtro.doFilter(request, response, (req, res) -> enMdc.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)));

        assertEquals(enMdc.get(), response.getHeader("X-Correlation-ID"), "lo que va a los logs y a la respuesta es lo mismo");
        return response.getHeader("X-Correlation-ID");
    }

    @Test
    @DisplayName("Un X-Correlation-ID con forma de identificador se conserva")
    void correlationIdValidoSeConserva() throws Exception {
        assertEquals("abc-123-DEF", correlationIdTrasFiltrar("abc-123-DEF"));
        String uuid = java.util.UUID.randomUUID().toString();
        assertEquals(uuid, correlationIdTrasFiltrar(uuid));
    }

    @Test
    @DisplayName("Uno con caracteres raros, saltos de línea o más de 64 caracteres se reemplaza por uno generado")
    void correlationIdInvalidoSeReemplaza() throws Exception {
        for (String malo : new String[]{"id con espacios", "id\r\nSet-Cookie: x=1", "<script>", "a".repeat(65), "id;drop table", "ñandú"}) {
            String resultado = correlationIdTrasFiltrar(malo);
            assertNotEquals(malo, resultado);
            assertTrue(CorrelationIdFilter.esValido(resultado), "se generó uno válido para: " + malo.replaceAll("[\\r\\n]", "_"));
        }
    }

    @Test
    @DisplayName("Sin encabezado o vacío se genera uno")
    void sinCorrelationIdSeGenera() throws Exception {
        assertTrue(CorrelationIdFilter.esValido(correlationIdTrasFiltrar(null)));
        assertTrue(CorrelationIdFilter.esValido(correlationIdTrasFiltrar("   ")));
    }

    @Test
    @DisplayName("esValido: 1 a 64 letras, números o guiones")
    void formatoDelCorrelationId() {
        assertTrue(CorrelationIdFilter.esValido("a"));
        assertTrue(CorrelationIdFilter.esValido("a".repeat(64)));
        assertFalse(CorrelationIdFilter.esValido(""));
        assertFalse(CorrelationIdFilter.esValido(null));
        assertFalse(CorrelationIdFilter.esValido("a_b"));
        assertFalse(CorrelationIdFilter.esValido("a".repeat(65)));
    }
}
