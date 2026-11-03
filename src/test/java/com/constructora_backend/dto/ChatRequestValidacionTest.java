package com.constructora_backend.dto;

import com.constructora_backend.dto.request.ChatRequest;
import com.constructora_backend.dto.request.MensajeChat;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Validación del historial del chat: máximo 6 mensajes, texto no vacío de hasta 600 caracteres y rol user/model. */
class ChatRequestValidacionTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private ChatRequest con(List<MensajeChat> historial) {
        ChatRequest r = new ChatRequest();
        r.setPregunta("hola");
        r.setHistorial(historial);
        return r;
    }

    private boolean invalido(ChatRequest r, String fragmentoMensaje) {
        return validador.validate(r).stream().anyMatch(v -> v.getMessage().contains(fragmentoMensaje));
    }

    @Test
    void sinHistorialOConHistorialValidoEsCorrecto() {
        assertTrue(validador.validate(con(null)).isEmpty());
        assertTrue(validador.validate(con(List.of(new MensajeChat("user", "hola"), new MensajeChat("model", "¡Hola!")))).isEmpty());
    }

    @Test
    void masDeSeisMensajesSeRechaza() {
        List<MensajeChat> siete = new ArrayList<>();
        for (int i = 0; i < 7; i++) siete.add(new MensajeChat(i % 2 == 0 ? "user" : "model", "x"));

        assertTrue(invalido(con(siete), "no puede superar los 6 mensajes"));
    }

    @Test
    void exactamenteSeisMensajesSePermite() {
        List<MensajeChat> seis = new ArrayList<>();
        for (int i = 0; i < 6; i++) seis.add(new MensajeChat(i % 2 == 0 ? "user" : "model", "x"));

        assertTrue(validador.validate(con(seis)).isEmpty());
    }

    @Test
    void textoMuyLargoOVacioSeRechaza() {
        assertTrue(invalido(con(List.of(new MensajeChat("user", "a".repeat(601)))), "600 caracteres"));
        assertTrue(validador.validate(con(List.of(new MensajeChat("user", "a".repeat(600))))).isEmpty());
        assertTrue(invalido(con(List.of(new MensajeChat("user", "   "))), "no pueden estar vacíos"));
        assertTrue(invalido(con(List.of(new MensajeChat("user", null))), "no pueden estar vacíos"));
    }

    @Test
    void unRolInvalidoSeRechaza() {
        assertTrue(invalido(con(List.of(new MensajeChat("system", "hola"))), "debe ser user o model"));
        assertTrue(invalido(con(List.of(new MensajeChat(null, "hola"))), "obligatorio"));
    }
}
