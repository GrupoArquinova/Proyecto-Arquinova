package com.constructora_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/** Cliente HTTP para verificar el captcha (Cloudflare Turnstile) con tiempo máximo corto: no debe dejar colgado el formulario. */
@Configuration
public class CaptchaConfig {

    @Bean("captchaRestTemplate")
    public RestTemplate captchaRestTemplate(RestTemplateBuilder builder,
                                            @Value("${app.captcha.timeout-segundos:5}") long timeoutSegundos) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(timeoutSegundos))
                .setReadTimeout(Duration.ofSeconds(timeoutSegundos))
                .build();
    }
}
