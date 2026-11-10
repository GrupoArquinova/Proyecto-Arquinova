package com.constructora_backend.service;

import com.constructora_backend.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Verificación de Turnstile con un servidor simulado: ninguna prueba llama a Cloudflare de verdad. */
class CaptchaServiceTest {

    private static final String URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private MockRestServiceServer servidor;

    private CaptchaService servicio(String secreto, boolean falloAbierto) {
        RestTemplate restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.createServer(restTemplate);
        return new CaptchaService(restTemplate, secreto, falloAbierto, URL);
    }

    @Test
    @DisplayName("Sin secreto el captcha está desactivado: todo pasa y no se llama a Cloudflare")
    void desactivado() {
        CaptchaService s = servicio("", false);
        assertFalse(s.estaActivo());
        assertTrue(s.verificar(null, "1.2.3.4"));
        assertTrue(s.verificar("cualquier-cosa", null));
        servidor.verify(); // ninguna petición esperada ni recibida
    }

    @Test
    @DisplayName("Con captcha activo, un token ausente o vacío se rechaza sin llamar a Cloudflare")
    void tokenVacio() {
        CaptchaService s = servicio("secreto-de-prueba", false);
        assertFalse(s.verificar(null, "1.2.3.4"));
        assertFalse(s.verificar("", "1.2.3.4"));
        assertFalse(s.verificar("   ", "1.2.3.4"));
        servidor.verify();
    }

    @Test
    @DisplayName("Éxito: Cloudflare responde success=true y se le envían secret, response y remoteip")
    void exito() {
        CaptchaService s = servicio("secreto-de-prueba", false);
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("secret=secreto-de-prueba"),
                        org.hamcrest.Matchers.containsString("response=token-bueno"),
                        org.hamcrest.Matchers.containsString("remoteip=203.0.113.9"))))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertTrue(s.verificar("token-bueno", "203.0.113.9"));
        servidor.verify();
    }

    @Test
    @DisplayName("Fallo: Cloudflare responde success=false")
    void fallo() {
        CaptchaService s = servicio("secreto-de-prueba", false);
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}", MediaType.APPLICATION_JSON));

        assertFalse(s.verificar("token-malo", "203.0.113.9"));
        servidor.verify();
    }

    @Test
    @DisplayName("Sin IP no se envía remoteip")
    void sinIp() {
        CaptchaService s = servicio("secreto-de-prueba", false);
        servidor.expect(requestTo(URL))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("remoteip"))))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertTrue(s.verificar("tok", null));
        servidor.verify();
    }

    @Test
    @DisplayName("Error de red con fallo-abierto=false: 503 con mensaje claro")
    void errorDeRedFalloCerrado() {
        CaptchaService s = servicio("secreto-de-prueba", false);
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class, () -> s.verificar("tok", "1.2.3.4"));
        assertEquals("No se pudo verificar; intenta de nuevo", ex.getMessage());
    }

    @Test
    @DisplayName("Error de red con fallo-abierto=true: deja pasar")
    void errorDeRedFalloAbierto() {
        CaptchaService s = servicio("secreto-de-prueba", true);
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertTrue(s.verificar("tok", "1.2.3.4"));
    }

    @Test
    @DisplayName("Respuesta vacía de Cloudflare se trata como error de red según la política")
    void respuestaVacia() {
        CaptchaService cerrado = servicio("secreto-de-prueba", false);
        servidor.expect(requestTo(URL)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThrows(ServicioNoDisponibleException.class, () -> cerrado.verificar("tok", "1.2.3.4"));
    }
}
