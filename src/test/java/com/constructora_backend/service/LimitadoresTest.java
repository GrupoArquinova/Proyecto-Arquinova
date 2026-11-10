package com.constructora_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Contador por ventana de tiempo (Redis o memoria) y topes globales de confirmaciones. */
class LimitadoresTest {

    private static final Instant T0 = Instant.parse("2026-10-20T10:00:00Z");

    // ───────── LimitadorVentana ─────────

    @Test
    @DisplayName("Permite hasta el máximo dentro de la ventana y rechaza el resto; al vencer la ventana vuelve a permitir")
    void ventanaConMaximo() {
        LimitadorVentana l = new LimitadorVentana();

        assertTrue(l.intentar("k", 3, Duration.ofHours(1), T0));
        assertTrue(l.intentar("k", 3, Duration.ofHours(1), T0.plusSeconds(10)));
        assertTrue(l.intentar("k", 3, Duration.ofHours(1), T0.plusSeconds(20)));
        assertFalse(l.intentar("k", 3, Duration.ofHours(1), T0.plusSeconds(30)));

        assertTrue(l.intentar("k", 3, Duration.ofHours(1), T0.plus(Duration.ofMinutes(61))), "otra ventana");
    }

    @Test
    @DisplayName("Cada clave cuenta aparte")
    void clavesIndependientes() {
        LimitadorVentana l = new LimitadorVentana();

        assertTrue(l.intentar("a", 1, Duration.ofHours(1), T0));
        assertFalse(l.intentar("a", 1, Duration.ofHours(1), T0));
        assertTrue(l.intentar("b", 1, Duration.ofHours(1), T0));
    }

    @Test
    @DisplayName("Un máximo de 0 o menos siempre rechaza")
    void maximoCero() {
        assertFalse(new LimitadorVentana().intentar("k", 0, Duration.ofHours(1), T0));
    }

    @Test
    @DisplayName("devolver libera un uso de la ventana")
    void devolverLiberaUnUso() {
        LimitadorVentana l = new LimitadorVentana();
        assertTrue(l.intentar("k", 2, Duration.ofHours(1), T0));
        assertTrue(l.intentar("k", 2, Duration.ofHours(1), T0));
        assertFalse(l.intentar("k", 2, Duration.ofHours(1), T0));

        l.devolver("k");

        assertTrue(l.intentar("k", 2, Duration.ofHours(1), T0));
    }

    @Test
    @DisplayName("La memoria local tiene tope de claves y purga las vencidas")
    void memoriaConTopeYPurga() {
        LimitadorVentana l = new LimitadorVentana();
        for (int i = 0; i < LimitadorVentana.MAX_CLAVES_LOCALES; i++) {
            l.intentar("k" + i, 5, Duration.ofMinutes(1), T0);
        }
        assertEquals(LimitadorVentana.MAX_CLAVES_LOCALES, l.tamanoLocal());

        // Con el tope alcanzado y todo vigente, una clave nueva se niega por prudencia
        assertFalse(l.intentar("nueva", 5, Duration.ofMinutes(1), T0.plusSeconds(1)));

        // Pasada la ventana, todo vence: se purga y vuelve a haber sitio
        assertTrue(l.intentar("nueva", 5, Duration.ofMinutes(1), T0.plus(Duration.ofMinutes(5))));
        assertEquals(1, l.tamanoLocal());
    }

    @Test
    @DisplayName("Con Redis cuenta con INCR + EXPIRE en la primera vez")
    @SuppressWarnings("unchecked")
    void conRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment("limite:k")).thenReturn(1L, 2L, 3L);
        LimitadorVentana l = new LimitadorVentana(redis);

        assertTrue(l.intentar("k", 2, Duration.ofHours(1)));
        assertTrue(l.intentar("k", 2, Duration.ofHours(1)));
        assertFalse(l.intentar("k", 2, Duration.ofHours(1)));

        verify(redis).expire("limite:k", Duration.ofHours(1)); // solo en el primero
    }

    @Test
    @DisplayName("Si Redis falla, sigue funcionando con la memoria local")
    void redisCaidoUsaMemoria() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new RuntimeException("sin conexión"));
        LimitadorVentana l = new LimitadorVentana(redis);

        assertTrue(l.intentar("k", 1, Duration.ofHours(1)));
        assertFalse(l.intentar("k", 1, Duration.ofHours(1)));
    }

    // ───────── LimitadorConfirmaciones: topes globales ─────────

    @Test
    @DisplayName("Tope global por hora: después de N confirmaciones a direcciones distintas ya no se envían más")
    void topeGlobalPorHora() {
        LimitadorConfirmaciones l = new LimitadorConfirmaciones(new LimitadorVentana(), 3, 100);

        assertTrue(l.permitir("a@x.com", T0));
        assertTrue(l.permitir("b@x.com", T0));
        assertTrue(l.permitir("c@x.com", T0));
        assertFalse(l.permitir("d@x.com", T0.plusSeconds(5)), "se alcanzó el tope global de la hora");

        // Pasada la hora, el tope se renueva
        assertTrue(l.permitir("d@x.com", T0.plus(Duration.ofMinutes(61))));
    }

    @Test
    @DisplayName("Tope global por día aunque cada hora tenga cupo")
    void topeGlobalPorDia() {
        LimitadorConfirmaciones l = new LimitadorConfirmaciones(new LimitadorVentana(), 100, 2);

        assertTrue(l.permitir("a@x.com", T0));
        assertTrue(l.permitir("b@x.com", T0.plus(Duration.ofHours(2))));
        assertFalse(l.permitir("c@x.com", T0.plus(Duration.ofHours(4))));
        assertTrue(l.permitir("c@x.com", T0.plus(Duration.ofHours(25))));
    }

    @Test
    @DisplayName("Una dirección que no recibió su confirmación por el tope global puede recibirla cuando haya cupo")
    void laDireccionNoQuedaMarcadaSiNoSeEnvio() {
        LimitadorConfirmaciones l = new LimitadorConfirmaciones(new LimitadorVentana(), 1, 100);
        assertTrue(l.permitir("a@x.com", T0));

        assertFalse(l.permitir("b@x.com", T0.plusSeconds(1)));
        assertTrue(l.permitir("b@x.com", T0.plus(Duration.ofMinutes(61))), "no quedó bloqueada por la hora");
    }

    @Test
    @DisplayName("Sigue valiendo 1 por hora por dirección, sin gastar cupo global en los repetidos")
    void unaPorHoraPorDireccionNoGastaCupoGlobal() {
        LimitadorConfirmaciones l = new LimitadorConfirmaciones(new LimitadorVentana(), 2, 100);

        assertTrue(l.permitir("a@x.com", T0));
        assertFalse(l.permitir("A@x.com", T0.plusSeconds(1)));
        assertFalse(l.permitir("a@x.com", T0.plusSeconds(2)));

        assertTrue(l.permitir("b@x.com", T0.plusSeconds(3)), "los repetidos no consumieron el cupo global");
    }

    @Test
    @DisplayName("Sin correo no se envía")
    void sinCorreo() {
        LimitadorConfirmaciones l = new LimitadorConfirmaciones();
        assertFalse(l.permitir(null));
        assertFalse(l.permitir("  "));
    }

    // ───────── EmailService: correos enmascarados en logs ─────────

    @Test
    @DisplayName("enmascarar deja la primera letra y el dominio")
    void enmascararCorreo() {
        assertEquals("a***@gmail.com", EmailService.enmascarar("ana.perez@gmail.com"));
        assertEquals("***", EmailService.enmascarar("sin-arroba"));
        assertEquals("(sin correo)", EmailService.enmascarar(null));
        assertEquals("(sin correo)", EmailService.enmascarar("  "));
    }
}
