package com.constructora_backend.config;

import com.constructora_backend.util.SanitizadorTexto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Sanitiza TODO texto que llega en el cuerpo JSON de una petición, antes de que llegue a un controlador, a un servicio o a
 * la base de datos: ningún DTO puede olvidarse de hacerlo. Spring Boot registra solo este módulo en su ObjectMapper.
 * Las contraseñas y los tokens se dejan tal cual (ver {@link SanitizadorTexto#esCampoSensible}).
 */
@Configuration
public class SanitizacionJacksonConfig {

    @Bean
    public Module sanitizadorDeEntradas() {
        SimpleModule modulo = new SimpleModule("SanitizadorDeEntradas");
        modulo.addDeserializer(String.class, new TextoSanitizado());
        return modulo;
    }

    static final class TextoSanitizado extends StdScalarDeserializer<String> {

        TextoSanitizado() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser parser, DeserializationContext contexto) throws IOException {
            String valor = StringDeserializer.instance.deserialize(parser, contexto);
            return SanitizadorTexto.esCampoSensible(parser.currentName()) ? valor : SanitizadorTexto.limpiar(valor);
        }
    }
}
