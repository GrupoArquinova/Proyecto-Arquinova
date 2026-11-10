package com.constructora_backend.service;

import com.constructora_backend.exception.ServicioNoDisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Verificación en servidor del captcha de Cloudflare Turnstile.
 * Sin secreto configurado (TURNSTILE_SECRET vacío) el captcha está DESACTIVADO y todo token es válido, para que el sitio
 * siga funcionando hasta que se creen las llaves. El token y el secreto nunca se escriben en el log.
 */
@Service
@Slf4j
public class CaptchaService {

    static final String MENSAJE_NO_DISPONIBLE = "No se pudo verificar; intenta de nuevo";

    private final RestTemplate restTemplate;
    private final String secreto;
    private final boolean falloAbierto;
    private final String urlVerificacion;

    public CaptchaService(@Qualifier("captchaRestTemplate") RestTemplate restTemplate,
                          @Value("${app.captcha.turnstile-secret:}") String secreto,
                          @Value("${app.captcha.fallo-abierto:false}") boolean falloAbierto,
                          @Value("${app.captcha.url-verificacion:https://challenges.cloudflare.com/turnstile/v0/siteverify}") String urlVerificacion) {
        this.restTemplate = restTemplate;
        this.secreto = secreto == null ? "" : secreto.trim();
        this.falloAbierto = falloAbierto;
        this.urlVerificacion = urlVerificacion;
    }

    /** true si hay secreto configurado (el captcha se exige). */
    public boolean estaActivo() {
        return !secreto.isEmpty();
    }

    /**
     * @return true si el token es válido o el captcha está desactivado; false si falta el token o Cloudflare lo rechaza.
     * @throws ServicioNoDisponibleException si Cloudflare no responde y la política no es de fallo abierto (503).
     */
    public boolean verificar(String token, String ipCliente) {
        if (!estaActivo()) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }

        MultiValueMap<String, String> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("secret", secreto);
        cuerpo.add("response", token);
        if (ipCliente != null && !ipCliente.isBlank()) {
            cuerpo.add("remoteip", ipCliente);
        }
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            JsonNode respuesta = restTemplate.postForObject(urlVerificacion, new HttpEntity<>(cuerpo, cabeceras), JsonNode.class);
            if (respuesta == null) {
                return resolverFalloDeRed("respuesta vacía");
            }
            boolean exito = respuesta.path("success").asBoolean(false);
            if (!exito) {
                log.info("Captcha rechazado por Cloudflare");
            }
            return exito;
        } catch (RestClientException ex) {
            // Solo el tipo de error: el mensaje podría arrastrar la URL o el cuerpo de la petición
            return resolverFalloDeRed(ex.getClass().getSimpleName());
        }
    }

    private boolean resolverFalloDeRed(String motivo) {
        if (falloAbierto) {
            log.warn("Cloudflare Turnstile no respondió ({}); se deja pasar (app.captcha.fallo-abierto=true)", motivo);
            return true;
        }
        log.warn("Cloudflare Turnstile no respondió ({}); se rechaza con 503", motivo);
        throw new ServicioNoDisponibleException(MENSAJE_NO_DISPONIBLE);
    }
}
