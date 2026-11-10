package com.constructora_backend.service;

import com.constructora_backend.service.CorreoConfirmacionSolicitud.Datos;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfirmacionSolicitudTest {

    private JavaMailSender correoQueArmaMensajes() {
        JavaMailSender correo = mock(JavaMailSender.class);
        when(correo.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
        return correo;
    }

    private EmailService servicio(JavaMailSender correo, boolean activa) {
        EmailService s = new EmailService(correo);
        ReflectionTestUtils.setField(s, "confirmacionActiva", activa);
        ReflectionTestUtils.setField(s, "whatsappEmpresa", "+57 300 000 0000");
        ReflectionTestUtils.setField(s, "correoAviso", "empresa@test.com");
        ReflectionTestUtils.setField(s, "remitente", "soporte@test.com");
        return s;
    }

    private Datos datos(String idioma, String proyecto, String interes, String mensaje) {
        return new Datos(idioma, "Ana", proyecto, interes, mensaje, "+57 300 000 0000", "empresa@test.com", "");
    }

    // ───────── Envío ─────────

    @Test
    void enviaLaConfirmacionAlCorreoDelClienteConLoQuePregunto() throws Exception {
        JavaMailSender correo = correoQueArmaMensajes();
        servicio(correo, true).enviarConfirmacionSolicitud("Ana", "ana@test.com", "El Encanto", "Villa Samán", "Quiero saber el precio", "es");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(correo).send(captor.capture());
        MimeMessage m = captor.getValue();

        assertEquals("ana@test.com", m.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Recibimos tu solicitud sobre Villa Samán - Grupo Arquinova", m.getSubject());
        assertEquals("empresa@test.com", m.getReplyTo()[0].toString());
        InternetAddress remitente = (InternetAddress) m.getFrom()[0];
        assertEquals("soporte@test.com", remitente.getAddress());
        assertEquals("Grupo Arquinova", remitente.getPersonal());
    }

    @Test
    void noEnviaSinCorreoOSiEstaApagada() {
        JavaMailSender correo = correoQueArmaMensajes();
        servicio(correo, true).enviarConfirmacionSolicitud("Ana", null, null, null, null, "es");
        servicio(correo, true).enviarConfirmacionSolicitud("Ana", "  ", null, null, null, "es");
        servicio(correo, false).enviarConfirmacionSolicitud("Ana", "ana@test.com", null, null, null, "es");
        verify(correo, never()).send(any(MimeMessage.class));
    }

    @Test
    void soloUnaConfirmacionPorHoraParaLaMismaDireccion() {
        JavaMailSender correo = correoQueArmaMensajes();
        EmailService s = servicio(correo, true);
        s.enviarConfirmacionSolicitud("Ana", "ana@test.com", null, null, null, "es");
        s.enviarConfirmacionSolicitud("Ana", "ANA@test.com", null, null, null, "es");
        verify(correo, times(1)).send(any(MimeMessage.class));
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
    void sinIdiomaOConUnoDesconocidoSaleEnEspanol() throws Exception {
        EmailService s = servicio(correoQueArmaMensajes(), true);
        assertEquals("Recibimos tu solicitud - Grupo Arquinova", s.armarConfirmacion("Ana", "ana@test.com", null, null, null, null).getSubject());
        assertEquals("Recibimos tu solicitud - Grupo Arquinova", s.armarConfirmacion("Ana", "ana@test.com", null, null, null, "fr").getSubject());
    }

    @Test
    void conIdiomaInglesElAsuntoSaleEnIngles() throws Exception {
        EmailService s = servicio(correoQueArmaMensajes(), true);
        assertEquals("We received your request about Lot LT-101 - Grupo Arquinova",
                s.armarConfirmacion("Ana", "ana@test.com", "El Encanto", "Lot LT-101", null, "en").getSubject());
    }

    // ───────── Contenido ─────────

    @Test
    void elCorreoResumeLoQuePreguntoYDejaLosMediosDeContacto() {
        Datos d = datos("es", "El Encanto", "Villa Samán", "Quiero saber el precio");

        String texto = CorreoConfirmacionSolicitud.texto(d);
        String html = CorreoConfirmacionSolicitud.html(d);

        assertTrue(texto.startsWith("Hola Ana,"));
        assertTrue(texto.contains("pronto nos comunicaremos contigo"));
        assertTrue(texto.contains("Proyecto: El Encanto"));
        assertTrue(texto.contains("Preguntaste por: Villa Samán"));
        // El correo sale hacia una dirección escrita por un desconocido: no repite su mensaje
        assertFalse(texto.contains("Quiero saber el precio"));
        assertFalse(texto.contains("Tu mensaje"));
        assertTrue(texto.contains("WhatsApp: +57 300 000 0000"));
        assertTrue(texto.contains("Correo: empresa@test.com"));
        assertTrue(texto.contains("NIT 901.397.504-2"));
        assertTrue(html.contains("Escribir por WhatsApp"));
        assertTrue(html.contains("Villa Samán"));
    }

    @Test
    void elBotonDeWhatsappAbreElChatConElMensajeYaEscrito() {
        String enlace = CorreoConfirmacionSolicitud.enlaceWhatsapp(datos("es", "El Encanto", "Villa Samán", null));

        assertTrue(enlace.startsWith("https://wa.me/573000000000?text="));
        assertTrue(enlace.contains("Villa%20Sam%C3%A1n"));
        assertFalse(enlace.contains(" "));
        assertFalse(enlace.contains("+57"));
    }

    @Test
    void sinInteresElWhatsappUsaElProyectoYSinNadaUnMensajeGeneral() {
        assertTrue(CorreoConfirmacionSolicitud.enlaceWhatsapp(datos("es", "El Encanto", null, null)).contains("El%20Encanto"));
        assertTrue(CorreoConfirmacionSolicitud.enlaceWhatsapp(datos("es", null, null, null)).contains("Hola%2C%20dej"));
    }

    @Test
    void sinNumeroDeWhatsappNoHayBotonNiEnlace() {
        Datos d = new Datos("es", "Ana", null, null, null, "", "empresa@test.com", "");
        assertEquals("", CorreoConfirmacionSolicitud.enlaceWhatsapp(d));
        assertFalse(CorreoConfirmacionSolicitud.html(d).contains("wa.me"));
        assertFalse(CorreoConfirmacionSolicitud.texto(d).contains("WhatsApp"));
    }

    @Test
    void conIdiomaInglesTodoSaleEnIngles() {
        Datos d = new Datos("en", "Ana", "El Encanto", "Villa Samán", "Price please", "+57 300 000 0000", "empresa@test.com", "https://www.ejemplo.com/legal/politica-datos");

        String texto = CorreoConfirmacionSolicitud.texto(d);

        assertTrue(texto.startsWith("Hello Ana,"));
        assertTrue(texto.contains("We received your request and we will get in touch with you soon"));
        assertTrue(texto.contains("You asked about: Villa Samán"));
        assertTrue(texto.contains("This is how we handle your personal data: https://www.ejemplo.com/legal/politica-datos"));
        assertFalse(texto.contains("Recibimos"));
        assertTrue(CorreoConfirmacionSolicitud.html(d).contains("Write on WhatsApp"));
    }

    @Test
    void conDireccionDelSitioElEnlaceALaPoliticaSaleLimpio() {
        EmailService s = servicio(correoQueArmaMensajes(), true);
        ReflectionTestUtils.setField(s, "sitioUrl", "https://www.ejemplo.com/");

        assertEquals("https://www.ejemplo.com/legal/politica-datos", s.urlPolitica());
    }

    @Test
    void sinDireccionDelSitioLaPoliticaNoTieneEnlace() {
        assertEquals("", servicio(correoQueArmaMensajes(), true).urlPolitica());
    }

    @Test
    void sinDireccionDelSitioNoPoneEnlaceALaPolitica() {
        assertFalse(CorreoConfirmacionSolicitud.texto(datos("es", null, null, null)).contains("politica-datos"));
        assertFalse(CorreoConfirmacionSolicitud.html(datos("es", null, null, null)).contains("politica-datos"));
    }

    @Test
    void loQueSeMuestraSeEscapaEnElHtml() {
        Datos d = datos("es", "<script>alert(1)</script> & más", "Villa \"X\"", null);

        String html = CorreoConfirmacionSolicitud.html(d);

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; más"));
    }

    @Test
    void elMensajeDelVisitanteNuncaSeRepiteNiSiEsMuyLargoONiConEnlaces() {
        String largo = "a".repeat(900) + " https://malo.example/phishing";

        String texto = CorreoConfirmacionSolicitud.texto(datos("es", "El Encanto", null, largo));
        String html = CorreoConfirmacionSolicitud.html(datos("es", "El Encanto", null, largo));

        assertFalse(texto.contains("aaaa"));
        assertFalse(html.contains("aaaa"));
        assertFalse(texto.contains("malo.example"));
        assertFalse(html.contains("malo.example"));
    }

    @Test
    void elSaludoSoloLlevaUnNombreSaneadoSinEnlacesNiNumerosNiDirecciones() {
        assertEquals("María José O'Neil-Pérez", CorreoConfirmacionSolicitud.nombreSeguro("  María   José O'Neil-Pérez "));
        // Texto de ataque: enlace, correo, teléfono o saltos de línea no pasan al saludo
        assertEquals("", CorreoConfirmacionSolicitud.nombreSeguro("Visita http://malo.example"));
        assertEquals("", CorreoConfirmacionSolicitud.nombreSeguro("www.malo.example"));
        assertEquals("", CorreoConfirmacionSolicitud.nombreSeguro("pepe@malo.example"));
        assertEquals("Llama ya", CorreoConfirmacionSolicitud.nombreSeguro("Llama ya 3001234567"));
        assertEquals("Ana Bcc evil", CorreoConfirmacionSolicitud.nombreSeguro("Ana\r\nBcc: evil"));
        assertEquals("", CorreoConfirmacionSolicitud.nombreSeguro("<>@$$ 123"));
        assertEquals("", CorreoConfirmacionSolicitud.nombreSeguro(null));
        assertEquals(60, CorreoConfirmacionSolicitud.nombreSeguro("a".repeat(200)).length());
    }

    @Test
    void siElNombreEsUnEnlaceElCorreoSaludaSinNombre() {
        Datos d = new Datos("es", "http://malo.example/x", null, null, null, "+57 300 000 0000", "empresa@test.com", "");

        assertTrue(CorreoConfirmacionSolicitud.texto(d).startsWith("Hola,"));
        assertFalse(CorreoConfirmacionSolicitud.html(d).contains("malo.example"));
    }
}
