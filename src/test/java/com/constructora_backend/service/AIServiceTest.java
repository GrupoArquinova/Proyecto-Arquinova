package com.constructora_backend.service;

import com.constructora_backend.exception.AIServicioNoDisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Asistente de IA: pide a Gemini con la clave en un encabezado y convierte cualquier fallo en "no disponible" sin filtrar datos. */
class AIServiceTest {

    private static final String BASE = "https://gemini.test/v1beta/models";
    private static final String CLAVE = "clave-secreta-de-prueba";

    private RestTemplate restTemplate;
    private MockRestServiceServer servidor;
    private AIService servicio;
    private final java.util.List<Long> pausas = new java.util.ArrayList<>();

    @BeforeEach
    void preparar() {
        restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.createServer(restTemplate);
        servicio = new AIService(restTemplate, CLAVE, "gemini-3.1-flash-lite", "", BASE + "/");
        servicio.pausa = pausas::add; // sin esperar de verdad
    }

    private static String respuestaConTexto(String texto) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + texto + "\"}]}}]}";
    }

    @Test
    void laClaveViajaEnUnEncabezadoYNuncaEnLaDireccion() {
        servidor.expect(requestTo(BASE + "/gemini-3.1-flash-lite:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", CLAVE))
                .andRespond(withSuccess(respuestaConTexto("Hola, soy el asistente"), MediaType.APPLICATION_JSON));

        assertEquals("Hola, soy el asistente", servicio.generarRespuesta("hola", "datos", "es"));
        servidor.verify();
    }

    @Test
    void laPreguntaVaComoMensajeDelUsuarioYLasReglasComoInstruccionDeSistema() throws Exception {
        servidor.expect(requestTo(BASE + "/gemini-3.1-flash-lite:generateContent"))
                .andExpect(request -> {
                    JsonNode cuerpo = new ObjectMapper().readTree(((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsString());
                    assertEquals("¿Qué villas hay?", cuerpo.at("/contents/0/parts/0/text").asText());
                    assertEquals("user", cuerpo.at("/contents/0/role").asText());
                    String reglas = cuerpo.at("/systemInstruction/parts/0/text").asText();
                    assertTrue(reglas.contains("Villa Samán"), "las reglas deben incluir los datos reales");
                    assertTrue(reglas.contains("Nunca indiques ni estimes precios"));
                    assertTrue(cuerpo.at("/generationConfig/thinkingConfig").isMissingNode());
                })
                .andRespond(withSuccess(respuestaConTexto("ok"), MediaType.APPLICATION_JSON));

        servicio.generarRespuesta("¿Qué villas hay?", "Tipología/villa Villa Samán: 293 m²", "es");
        servidor.verify();
    }

    @Test
    void sinClaveNoSeLlamaAGoogleYSeAvisaQueNoEstaDisponible() {
        AIService sinClave = new AIService(restTemplate, "  ", "gemini-3.1-flash-lite", "", BASE);

        assertThrows(AIServicioNoDisponibleException.class, () -> sinClave.generarRespuesta("hola", "datos", "es"));
        servidor.verify(); // no hubo ninguna petición
    }

    @Test
    void siGoogleResponde404ElErrorNoLlevaLaClaveNiElCuerpo() {
        servidor.expect(requestTo(BASE + "/gemini-3.1-flash-lite:generateContent"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"models/x is not found " + CLAVE + "\"}"));

        AIServicioNoDisponibleException e = assertThrows(AIServicioNoDisponibleException.class, () -> servicio.generarRespuesta("hola", "datos", "es"));

        assertFalse(e.getMessage().contains(CLAVE));
        assertFalse(e.getMessage().contains("is not found"));
        assertTrue(e.getMessage().contains("404"));
    }

    private static final String URL_PRINCIPAL = BASE + "/gemini-3.1-flash-lite:generateContent";
    private static final String URL_RESPALDO = BASE + "/gemini-3.6-flash:generateContent";

    private AIService conRespaldo() {
        AIService s = new AIService(restTemplate, CLAVE, "gemini-3.1-flash-lite", "gemini-3.6-flash", BASE);
        s.pausa = pausas::add;
        return s;
    }

    @Test
    void siGoogleSeCaeSeReintentaYAlFinalSeConvierteEnNoDisponible() {
        for (int i = 0; i < AIService.INTENTOS_POR_MODELO; i++) {
            servidor.expect(requestTo(URL_PRINCIPAL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }

        assertThrows(AIServicioNoDisponibleException.class, () -> servicio.generarRespuesta("hola", "datos", "es"));
        servidor.verify();
        assertEquals(AIService.INTENTOS_POR_MODELO - 1, pausas.size(), "espera entre un intento y el siguiente");
    }

    @Test
    void unaAltaDemandaPasajeraSeResuelveSolaConUnReintento() {
        servidor.expect(requestTo(URL_PRINCIPAL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidor.expect(requestTo(URL_PRINCIPAL)).andRespond(withSuccess(respuestaConTexto("Ahora sí"), MediaType.APPLICATION_JSON));

        assertEquals("Ahora sí", servicio.generarRespuesta("q proyectos tienen", "datos", "es"));
        servidor.verify();
        assertEquals(1, pausas.size());
    }

    @Test
    void tambienSeReintentaCuandoGoogleDiceDemasiadasPeticiones429() {
        servidor.expect(requestTo(URL_PRINCIPAL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(requestTo(URL_PRINCIPAL)).andRespond(withSuccess(respuestaConTexto("ok"), MediaType.APPLICATION_JSON));

        assertEquals("ok", servicio.generarRespuesta("hola", "datos", "es"));
    }

    @Test
    void siElPrincipalSigueFallandoSeUsaElModeloDeRespaldo() {
        AIService s = conRespaldo();
        for (int i = 0; i < AIService.INTENTOS_POR_MODELO; i++) {
            servidor.expect(requestTo(URL_PRINCIPAL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }
        servidor.expect(requestTo(URL_RESPALDO)).andRespond(withSuccess(respuestaConTexto("Respuesta del respaldo"), MediaType.APPLICATION_JSON));

        assertEquals("Respuesta del respaldo", s.generarRespuesta("hola", "datos", "es"));
        servidor.verify();
    }

    @Test
    void siGoogleRetiraElModeloPrincipalPasaAlRespaldoSinReintentar() {
        AIService s = conRespaldo();
        servidor.expect(requestTo(URL_PRINCIPAL)).andRespond(withStatus(HttpStatus.NOT_FOUND)); // modelo retirado: repetirlo no sirve
        servidor.expect(requestTo(URL_RESPALDO)).andRespond(withSuccess(respuestaConTexto("ok"), MediaType.APPLICATION_JSON));

        assertEquals("ok", s.generarRespuesta("hola", "datos", "es"));
        servidor.verify();
        assertTrue(pausas.isEmpty());
    }

    @Test
    void unaClaveInvalidaNoSeReintenta() {
        servidor.expect(requestTo(URL_PRINCIPAL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(AIServicioNoDisponibleException.class, () -> servicio.generarRespuesta("hola", "datos", "es"));
        servidor.verify();
        assertTrue(pausas.isEmpty());
    }

    @Test
    void siFallanElPrincipalYElRespaldoSeAvisaQueNoEstaDisponible() {
        AIService s = conRespaldo();
        for (int i = 0; i < AIService.INTENTOS_POR_MODELO; i++) {
            servidor.expect(requestTo(URL_PRINCIPAL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }
        for (int i = 0; i < AIService.INTENTOS_POR_MODELO; i++) {
            servidor.expect(requestTo(URL_RESPALDO)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }

        assertThrows(AIServicioNoDisponibleException.class, () -> s.generarRespuesta("hola", "datos", "es"));
        servidor.verify();
    }

    @Test
    void lasInstruccionesPidenInterpretarLaOrtografiaDeLaGente() {
        String reglas = AIService.instrucciones("d", "es");

        assertTrue(reglas.contains("faltas de ortografía"));
        assertTrue(reglas.contains("keda"));
        assertTrue(reglas.contains("pregunta brevemente"));
    }

    @Test
    void unaRespuestaSinTextoSeTrataComoNoDisponible() {
        servidor.expect(requestTo(BASE + "/gemini-3.1-flash-lite:generateContent"))
                .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[]}}]}", MediaType.APPLICATION_JSON));

        assertThrows(AIServicioNoDisponibleException.class, () -> servicio.generarRespuesta("hola", "datos", "es"));
    }

    @Test
    void unaPreguntaBloqueadaPorGoogleSeResponderCortesmenteEnElIdioma() {
        servidor.expect(requestTo(BASE + "/gemini-3.1-flash-lite:generateContent"))
                .andRespond(withSuccess("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/gemini-3.1-flash-lite:generateContent"))
                .andRespond(withSuccess("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", MediaType.APPLICATION_JSON));

        assertEquals(AIService.MENSAJE_BLOQUEADO_ES, servicio.generarRespuesta("x", "datos", "es"));
        assertEquals(AIService.MENSAJE_BLOQUEADO_EN, servicio.generarRespuesta("x", "datos", "en"));
    }

    @Test
    void unidadDeFragmentosDeTextoYSinDatos() throws Exception {
        ObjectMapper m = new ObjectMapper();
        assertEquals("Hola mundo", AIService.extraerTexto(m.readTree("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hola \"},{\"text\":\"mundo\"}]}}]}")));
        assertNull(AIService.extraerTexto(m.readTree("{}")));
        assertNull(AIService.extraerTexto(null));
    }

    @Test
    void lasInstruccionesPidenResponderEnInglesCuandoElSitioEstaEnIngles() {
        assertTrue(AIService.instrucciones("d", "en").contains("responde siempre en inglés"));
        assertTrue(AIService.instrucciones("d", "es").contains("mismo idioma"));
        assertTrue(AIService.instrucciones("d", null).contains("mismo idioma"));
    }
}
