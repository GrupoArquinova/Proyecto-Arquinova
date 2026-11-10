package com.constructora_backend.service;

import com.constructora_backend.service.CorreoAvisoSolicitud.Datos;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Aviso interno que recibe el equipo con cada solicitud: datos de la persona, lo que preguntó y botones para responder. */
class AvisoSolicitudTest {

    private Datos datos(String telefono, String correo, String interes, String mensaje) {
        return new Datos("Ana Gómez", telefono, correo, "El Encanto", interes, "Diseño", "es", mensaje);
    }

    private EmailService servicio(JavaMailSender correo) {
        EmailService s = new EmailService(correo);
        ReflectionTestUtils.setField(s, "correoAviso", "equipo@test.com");
        ReflectionTestUtils.setField(s, "remitente", "soporte@test.com");
        return s;
    }

    // ───────── Envío ─────────

    @Test
    void elAvisoVaAlCorreoDeLaEmpresaYLaRespuestaLlegaAlCliente() throws Exception {
        JavaMailSender correo = mock(JavaMailSender.class);
        when(correo.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));

        servicio(correo).enviarCorreoNuevaSolicitud("Ana Gómez", "3001234567", "ana@test.com", "El Encanto", "Villa Samán", null, "Hola", "es");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(correo).send(captor.capture());
        MimeMessage m = captor.getValue();
        assertEquals("equipo@test.com", m.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("ana@test.com", m.getReplyTo()[0].toString());
        assertEquals("Nueva solicitud de contacto de: Ana Gómez (Villa Samán)", m.getSubject());
    }

    @Test
    void elAsuntoNoAdmiteSaltosDeLineaNiCaracteresDeControl() {
        Datos atacante = new Datos("Ana\r\nBcc: victima@x.com\u0000", "300", "a@x.com", null, "Villa\nX", null, "es", "m");

        String asunto = CorreoAvisoSolicitud.asunto(atacante);

        assertEquals("Nueva solicitud de contacto de: Ana Bcc: victima@x.com (Villa X)", asunto);
        assertFalse(asunto.contains("\r"));
        assertFalse(asunto.contains("\n"));
        assertEquals("a b c", CorreoAvisoSolicitud.sinControl("a\r\n\t b \u0007 c"));
    }

    @Test
    void elMensajeArmadoNoTieneCabecerasInyectadas() throws Exception {
        JavaMailSender correo = mock(JavaMailSender.class);
        when(correo.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));

        MimeMessage m = servicio(correo).armarAvisoSolicitud("Ana\r\nBcc: victima@x.com", "300", "a@x.com", null, null, null, "hola", "es");

        assertNull(m.getHeader("Bcc"));
        assertFalse(m.getSubject().contains("\n"));
    }

    @Test
    void sinCorreoDeAvisoConfiguradoNoSeEnviaNadaYNoFalla() {
        JavaMailSender correo = mock(JavaMailSender.class);
        EmailService s = servicio(correo);
        ReflectionTestUtils.setField(s, "correoAviso", "");

        assertDoesNotThrow(() -> s.enviarCorreoNuevaSolicitud("Ana", "300", null, null, null, null, null, "es"));

        verify(correo, never()).send(any(MimeMessage.class));
    }

    @Test
    void siFallaElEnvioElErrorSePropaga() {
        JavaMailSender correo = mock(JavaMailSender.class);
        when(correo.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP caído")).when(correo).send(any(MimeMessage.class));

        assertThrows(MailSendException.class, () ->
                servicio(correo).enviarCorreoNuevaSolicitud("Ana", "3001234567", null, null, null, null, null, "es"));
    }

    // ───────── Contenido ─────────

    @Test
    void elAvisoMuestraTodosLosDatosYLosBotonesDeRespuesta() {
        Datos d = datos("300 123 4567", "ana@test.com", "Villa Samán", "Quiero el precio");

        String html = CorreoAvisoSolicitud.html(d);
        String texto = CorreoAvisoSolicitud.texto(d);

        assertTrue(html.contains("Nueva solicitud de contacto"));
        assertTrue(html.contains("Ana Gómez"));
        assertTrue(html.contains("Villa Samán"));
        assertTrue(html.contains("Quiero el precio"));
        assertTrue(html.contains("href=\"tel:3001234567\""));
        assertTrue(html.contains("href=\"mailto:ana@test.com\""));
        assertTrue(html.contains("Escribirle por WhatsApp"));
        assertTrue(html.contains("Responder por correo"));
        assertTrue(texto.contains("Preguntó por: Villa Samán"));
        assertTrue(texto.contains("Mensaje del cliente:\nQuiero el precio"));
    }

    @Test
    void elWhatsappDelClienteCompletaElIndicativoDeUnCelularColombiano() {
        String enlace = CorreoAvisoSolicitud.enlaceWhatsappCliente(datos("300 123 4567", null, "Villa Samán", null));

        assertTrue(enlace.startsWith("https://wa.me/573001234567?text="));
        assertTrue(enlace.contains("Villa%20Sam%C3%A1n"));
        assertFalse(enlace.contains(" "));
    }

    @Test
    void conIndicativoPropioSeRespeta() {
        assertTrue(CorreoAvisoSolicitud.enlaceWhatsappCliente(datos("+1 305 555 0100", null, null, null)).startsWith("https://wa.me/13055550100?"));
        assertTrue(CorreoAvisoSolicitud.enlaceWhatsappCliente(datos("573001234567", null, null, null)).startsWith("https://wa.me/573001234567?"));
    }

    @Test
    void unNumeroDudosoNoGeneraEnlaceDeWhatsapp() {
        assertEquals("", CorreoAvisoSolicitud.enlaceWhatsappCliente(datos("604 123 45", null, null, null)));
        assertEquals("", CorreoAvisoSolicitud.enlaceWhatsappCliente(datos(null, "ana@test.com", null, null)));
    }

    @Test
    void sinTelefonoNoHayBotonesDeLlamadaNiWhatsappPeroSiDeCorreo() {
        String html = CorreoAvisoSolicitud.html(datos(null, "ana@test.com", null, "Hola"));

        assertFalse(html.contains("tel:"));
        assertFalse(html.contains("wa.me"));
        assertTrue(html.contains("Responder por correo"));
    }

    @Test
    void sinCorreoNiMensajeSeIndicaSinInventarDatos() {
        Datos d = datos("3001234567", null, null, null);

        assertFalse(CorreoAvisoSolicitud.html(d).contains("mailto:"));
        assertTrue(CorreoAvisoSolicitud.html(d).contains("(sin mensaje)"));
        assertTrue(CorreoAvisoSolicitud.texto(d).contains("Correo: No indicó"));
        assertTrue(CorreoAvisoSolicitud.texto(d).contains("(sin mensaje)"));
    }

    @Test
    void siElSitioEstabaEnInglesSeAvisaAlEquipo() {
        Datos d = new Datos("Ana", "3001234567", "ana@test.com", null, null, null, "en", "Hello");

        assertTrue(CorreoAvisoSolicitud.html(d).contains("Sitio en inglés"));
        assertTrue(CorreoAvisoSolicitud.texto(d).contains("Inglés (conviene responderle en inglés)"));
        assertTrue(CorreoAvisoSolicitud.enlaceWhatsappCliente(d).contains("Hello%20Ana"));
    }

    @Test
    void loQueEscribeLaPersonaSeEscapaEnElHtml() {
        Datos d = new Datos("<b>Ana</b>", "3001234567", "a\"b@test.com", "<i>X</i>", "Villa <script>", null, "es", "<script>alert(1)</script>");

        String html = CorreoAvisoSolicitud.html(d);

        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<b>Ana</b>"));
        assertFalse(html.contains("<i>X</i>"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(html.contains("mailto:a\"b"));
    }
}
