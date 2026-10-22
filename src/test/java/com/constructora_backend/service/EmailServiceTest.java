package com.constructora_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "remitente", "soporte@constructora.com");
    }

    private MimeMessage mimeVacio() {
        return new MimeMessage((Session) null);
    }

    @Test
    @DisplayName("enviarCorreoRecuperacion - Envía un correo con asunto profesional y remitente con el nombre de la empresa")
    void enviarCorreoRecuperacion_Exitoso() throws Exception {
        ReflectionTestUtils.setField(emailService, "sitioUrl", "https://arquinova.vercel.app");
        when(mailSender.createMimeMessage()).thenReturn(mimeVacio());

        emailService.enviarCorreoRecuperacion("cliente@correo.com", "uuid-token-12345");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        MimeMessage enviado = captor.getValue();

        assertEquals("Restablece tu contraseña - Grupo Arquinova", enviado.getSubject());
        assertEquals("cliente@correo.com", enviado.getRecipients(Message.RecipientType.TO)[0].toString());
        InternetAddress remitente = (InternetAddress) enviado.getFrom()[0];
        assertEquals("soporte@constructora.com", remitente.getAddress());
        assertEquals("Grupo Arquinova", remitente.getPersonal());
    }

    @Test
    @DisplayName("enlaceRecuperacion - Usa la dirección pública del sitio (APP_SITIO_URL), sin localhost")
    void enlaceRecuperacion_UsaLaDireccionDelSitio() {
        ReflectionTestUtils.setField(emailService, "sitioUrl", "https://arquinova.vercel.app/");

        String enlace = emailService.enlaceRecuperacion("uuid-token-12345");

        assertEquals("https://arquinova.vercel.app/reset-password?token=uuid-token-12345", enlace);
        assertFalse(enlace.contains("localhost"));
    }

    @Test
    @DisplayName("enlaceRecuperacion - Sin APP_SITIO_URL usa el primer origen permitido (CORS_ALLOWED_ORIGINS)")
    void enlaceRecuperacion_SinSitioUsaElPrimerOrigen() {
        ReflectionTestUtils.setField(emailService, "origenesPermitidos", "https://arquinova.vercel.app,https://otro.com");

        assertEquals("https://arquinova.vercel.app/reset-password?token=abc", emailService.enlaceRecuperacion("abc"));
    }

    @Test
    @DisplayName("El mensaje no habla de token ni de código, trae el enlace y el aviso de vencimiento")
    void mensajeRecuperacion_EsProfesional() {
        String enlace = "https://arquinova.vercel.app/reset-password?token=uuid-token-12345";

        String texto = emailService.textoRecuperacion(enlace);
        String html = emailService.htmlRecuperacion(enlace);

        for (String contenido : new String[]{texto, html}) {
            assertTrue(contenido.contains(enlace));
            assertTrue(contenido.contains("15 minutos"));
            assertTrue(contenido.contains("Grupo Arquinova"));
            String sinEnlace = contenido.replace(enlace, "").toLowerCase();
            assertFalse(sinEnlace.contains("token"));
            assertFalse(sinEnlace.contains("localhost"));
            assertFalse(sinEnlace.contains("frontend"));
        }
        assertTrue(html.contains("Restablecer contraseña"));
    }

    @Test
    @DisplayName("enviarCorreoRecuperacion - Debe propagar MailException si el envio SMTP falla")
    void enviarCorreoRecuperacion_FalloMailSender() {
        when(mailSender.createMimeMessage()).thenReturn(mimeVacio());
        doThrow(new MailSendException("Fallo conexion SMTP"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(MailSendException.class, () ->
                emailService.enviarCorreoRecuperacion("cliente@correo.com", "uuid-token-12345")
        );
    }

    @Test
    @DisplayName("enviarCorreoFallback - Debe ejecutarse limpiamente sin lanzar excepciones")
    void enviarCorreoFallback_EjecutaSinExcepciones() {
        assertDoesNotThrow(() ->
                emailService.enviarCorreoFallback("cliente@correo.com", "uuid-token-12345", new RuntimeException("SMTP Down"))
        );
    }
}

