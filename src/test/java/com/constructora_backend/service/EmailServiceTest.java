package com.constructora_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    @DisplayName("enviarCorreoRecuperacion - Debe configurar SimpleMailMessage y llamar mailSender.send exitosamente")
    void enviarCorreoRecuperacion_Exitoso() {
        String destino = "cliente@correo.com";
        String token = "uuid-token-12345";

        emailService.enviarCorreoRecuperacion(destino, token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage mensajeEnviado = messageCaptor.getValue();
        assertNotNull(mensajeEnviado);
        assertEquals("soporte@constructora.com", mensajeEnviado.getFrom());
        assertArrayEquals(new String[]{destino}, mensajeEnviado.getTo());
        assertEquals("Recuperación de Contraseña - Constructora", mensajeEnviado.getSubject());
        assertTrue(mensajeEnviado.getText().contains(token));
        assertTrue(mensajeEnviado.getText().contains("http://localhost:4200/reset-password?token=" + token));
    }

    @Test
    @DisplayName("enviarCorreoRecuperacion - Debe propagar MailException si el envio SMTP falla")
    void enviarCorreoRecuperacion_FalloMailSender() {
        String destino = "cliente@correo.com";
        String token = "uuid-token-12345";

        doThrow(new MailSendException("Fallo conexion SMTP"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(MailSendException.class, () ->
                emailService.enviarCorreoRecuperacion(destino, token)
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

