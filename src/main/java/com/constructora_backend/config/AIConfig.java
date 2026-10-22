package com.constructora_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/** Cliente HTTP del asistente de IA, con tiempos máximos para que un Gemini lento no deje colgadas las peticiones del sitio. */
@Configuration
public class AIConfig {

    @Bean("geminiRestTemplate")
    public RestTemplate geminiRestTemplate(RestTemplateBuilder builder,
                                           @Value("${gemini.conexion-segundos:5}") long conexionSegundos,
                                           @Value("${gemini.respuesta-segundos:10}") long respuestaSegundos) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(conexionSegundos))
                .setReadTimeout(Duration.ofSeconds(respuestaSegundos))
                .build();
    }
}
