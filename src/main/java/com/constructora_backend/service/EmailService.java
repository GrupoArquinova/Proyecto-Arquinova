package com.constructora_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@constructora.com}")
    private String remitente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void enviarCorreoRecuperacion(String destino, String token) {
        String enlace = "http://localhost:4200/reset-password?token=" + token;

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente != null && !remitente.isBlank() ? remitente : "noreply@constructora.com");
        mensaje.setTo(destino);
        mensaje.setSubject("Recuperación de Contraseña - Constructora");
        mensaje.setText("Hola,\n\nHas solicitado restablecer tu contraseña.\n\n"
                + "🔑 Tu CÓDIGO / TOKEN de recuperación es:\n"
                + token + "\n\n"
                + "Enlace directo para cuando tengas el frontend activo:\n"
                + enlace + "\n\n"
                + "Este token vence en 15 minutos.\nSi no solicitaste este cambio, ignora este mensaje.");

        try {
            mailSender.send(mensaje);
            log.info("Correo de recuperación enviado exitosamente a {}", destino);
        } catch (MailException e) {
            log.error("Error al enviar el correo de recuperación a {}: {}", destino, e.getMessage());
            throw e;
        }
    }
}
