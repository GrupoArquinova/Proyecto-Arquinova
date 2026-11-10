package com.constructora_backend.config;

import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.request.ChatRequest;
import com.constructora_backend.dto.request.ResetPasswordRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Lo que llega por JSON se limpia antes de tocar un DTO: lo comprueba el ObjectMapper de Spring Boot, el mismo de los controladores. */
@JsonTest
@Import(SanitizacionJacksonConfig.class)
class SanitizacionDeEntradasTest {

    @Autowired private ObjectMapper mapper;

    @Test
    @DisplayName("El formulario público llega sin etiquetas ni caracteres de control, y el mensaje conserva sus saltos de línea")
    void formularioPublico() throws Exception {
        String json = """
                {"nombre":"Ana <script>alert(1)</script>Gómez","correo":"ana@correo.com","telefono":"+57 316 865 3715",
                 "servicioInteres":"<b>Diseño</b>","mensaje":"Hola\\n<img src=x onerror=alert(1)>Quiero info\\u0000 de <3 villas",
                 "consentimientoDatos":true}""";

        SolicitudContactoPublicDTO dto = mapper.readValue(json, SolicitudContactoPublicDTO.class);

        assertThat(dto.getNombre()).isEqualTo("Ana Gómez");
        assertThat(dto.getServicioInteres()).isEqualTo("Diseño");
        assertThat(dto.getMensaje()).isEqualTo("Hola\nQuiero info de <3 villas");
        assertThat(dto.getCorreo()).isEqualTo("ana@correo.com");
        assertThat(dto.getTelefono()).isEqualTo("+57 316 865 3715");
    }

    @Test
    @DisplayName("Las contraseñas y los tokens no se tocan: si no, la persona no podría iniciar sesión ni usar su enlace")
    void camposSensibles() throws Exception {
        ResetPasswordRequest reset = mapper.readValue(
                "{\"token\":\"abc<def>ghi\",\"nuevaPassword\":\"Cl<ave>Segura1!\"}", ResetPasswordRequest.class);

        assertThat(reset.getToken()).isEqualTo("abc<def>ghi");
        assertThat(reset.getNuevaPassword()).isEqualTo("Cl<ave>Segura1!");
    }

    @Test
    @DisplayName("El chat: la pregunta y el historial también se limpian")
    void chat() throws Exception {
        ChatRequest chat = mapper.readValue(
                "{\"pregunta\":\"<script>x</script>¿Cuánto cuesta?\",\"idioma\":\"es\"}", ChatRequest.class);

        assertThat(chat.getPregunta()).isEqualTo("¿Cuánto cuesta?");
    }

    @Test
    @DisplayName("También dentro de listas y objetos anidados, y los números y booleanos no cambian")
    void anidados() throws Exception {
        Map<?, ?> mapa = mapper.readValue(
                "{\"lista\":[\"<b>uno</b>\",\"dos\"],\"hijo\":{\"texto\":\"<i>tres</i>\"},\"n\":5,\"ok\":true}", Map.class);

        assertThat(mapa.get("lista")).isEqualTo(List.of("uno", "dos"));
        assertThat(((Map<?, ?>) mapa.get("hijo")).get("texto")).isEqualTo("tres");
        assertThat(mapa.get("n")).isEqualTo(5);
        assertThat(mapa.get("ok")).isEqualTo(true);
    }
}
