package com.constructora_backend.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfirmacionSolicitudTest {

    private EmailService servicio(JavaMailSender correo, boolean activa) {
        EmailService s = new EmailService(correo);
        ReflectionTestUtils.setField(s, "confirmacionActiva", activa);
        ReflectionTestUtils.setField(s, "whatsappEmpresa", "+57 300 000 0000");
        ReflectionTestUtils.setField(s, "correoAviso", "empresa@test.com");
        return s;
    }

    @Test
    void enviaLaConfirmacionAlCorreoDelCliente() {
        JavaMailSender correo = mock(JavaMailSender.class);
        servicio(correo, true).enviarConfirmacionSolicitud("Ana", "ana@test.com", "El Encanto", "es");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo).send(captor.capture());
        SimpleMailMessage m = captor.getValue();
        assertArrayEquals(new String[]{"ana@test.com"}, m.getTo());
        assertTrue(m.getText().contains("Hola Ana,"));
        assertTrue(m.getText().contains("sobre El Encanto"));
        assertTrue(m.getText().contains("+57 300 000 0000"));
        assertEquals("empresa@test.com", m.getReplyTo());
    }

    @Test
    void noEnviaSinCorreoOSiEstaApagada() {
        JavaMailSender correo = mock(JavaMailSender.class);
        servicio(correo, true).enviarConfirmacionSolicitud("Ana", null, null, "es");
        servicio(correo, true).enviarConfirmacionSolicitud("Ana", "  ", null, "es");
        servicio(correo, false).enviarConfirmacionSolicitud("Ana", "ana@test.com", null, "es");
        verifyNoInteractions(correo);
    }

    @Test
    void soloUnaConfirmacionPorHoraParaLaMismaDireccion() {
        JavaMailSender correo = mock(JavaMailSender.class);
        EmailService s = servicio(correo, true);
        s.enviarConfirmacionSolicitud("Ana", "ana@test.com", null, "es");
        s.enviarConfirmacionSolicitud("Ana", "ANA@test.com", null, "es");
        verify(correo, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void limitadorVuelvePermitirDespuesDeUnaHora() {
        LimitadorConfirmaciones l = new LimitadorConfirmaciones();
        Instant t = Instant.parse("2026-10-09T10:00:00Z");
        assertTrue(l.permitir("a@test.com", t));
        assertFalse(l.permitir("a@test.com", t.plus(Duration.ofMinutes(59))));
        assertTrue(l.permitir("a@test.com", t.plus(Duration.ofMinutes(61))));
        assertTrue(l.permitir("otra@test.com", t));
    }

    @Test
    void conDireccionDelSitioIncluyeElEnlaceALaPoliticaDeDatos() {
        EmailService s = servicio(mock(JavaMailSender.class), true);
        ReflectionTestUtils.setField(s, "sitioUrl", "https://www.ejemplo.com/");
        String texto = s.armarConfirmacion("Ana", "ana@test.com", null, "es").getText();
        assertTrue(texto.contains("https://www.ejemplo.com/legal/politica-datos"));
        assertFalse(texto.contains("//legal"));
    }

    @Test
    void sinDireccionDelSitioNoPoneEnlaces() {
        EmailService s = servicio(mock(JavaMailSender.class), true);
        String texto = s.armarConfirmacion("Ana", "ana@test.com", null, "es").getText();
        assertFalse(texto.contains("legal/politica-datos"));
    }

    @Test
    void conIdiomaInglesElCorreoSaleEnIngles() {
        EmailService s = servicio(mock(JavaMailSender.class), true);
        ReflectionTestUtils.setField(s, "sitioUrl", "https://www.ejemplo.com");
        SimpleMailMessage m = s.armarConfirmacion("Ana", "ana@test.com", "El Encanto", "en");
        assertEquals("We received your request - Grupo Arquinova", m.getSubject());
        assertTrue(m.getText().startsWith("Hello Ana,"));
        assertTrue(m.getText().contains("about El Encanto"));
        assertTrue(m.getText().contains("This is how we handle your personal data: https://www.ejemplo.com/legal/politica-datos"));
        assertFalse(m.getText().contains("Recibimos"));
    }

    @Test
    void sinIdiomaOConUnoDesconocidoSaleEnEspanol() {
        EmailService s = servicio(mock(JavaMailSender.class), true);
        assertEquals("Recibimos tu solicitud - Grupo Arquinova", s.armarConfirmacion("Ana", "ana@test.com", null, null).getSubject());
        assertEquals("Recibimos tu solicitud - Grupo Arquinova", s.armarConfirmacion("Ana", "ana@test.com", null, "fr").getSubject());
    }
}
