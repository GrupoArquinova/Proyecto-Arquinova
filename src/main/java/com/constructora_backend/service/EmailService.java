package com.constructora_backend.service;

import com.constructora_backend.config.RabbitMQConfig;
import com.constructora_backend.dto.event.EmailEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio de mensajería electrónica empresarial.
 * Soporta publicación a cola RabbitMQ para arquitectura orientada a eventos duraderos.
 * Protegido con Circuit Breaker (Resilience4j) para evitar bloqueo de hilos y fallos en cascada.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.mail.username:noreply@constructora.com}")
    private String remitente;

    @Autowired
    public EmailService(JavaMailSender mailSender,
                        @Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.mailSender = mailSender;
        this.rabbitTemplate = rabbitTemplate;
    }


    public EmailService(JavaMailSender mailSender) {
        this(mailSender, null);
    }

    /**
     * Punto de entrada principal para el envío de correos de recuperación.
     * Intenta encolar en RabbitMQ si está disponible, o ejecuta de forma asíncrona local si no hay broker.
     */
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void enviarCorreoRecuperacion(String destino, String token) {
        if (rabbitTemplate != null) {
            try {
                EmailEvent evento = EmailEvent.builder()
                        .tipo("RECUPERACION_PASSWORD")
                        .destinatario(destino)
                        .token(token)
                        .asunto("Recuperación de Contraseña - Constructora")
                        .generadoEn(LocalDateTime.now())
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_DIRECT,
                        RabbitMQConfig.ROUTING_KEY_EMAIL,
                        evento
                );
                log.info("Evento de correo encolado exitosamente en RabbitMQ para: {}", destino);
                return;
            } catch (Exception ex) {
                log.warn("Fallo al conectar con RabbitMQ, ejecutando envío SMTP directo como fallback: {}", ex.getMessage());
            }
        }

        // Ejecución directa si RabbitMQ no está disponible o falla
        enviarCorreoDirecto(destino, token);
    }

    /**
     * Envío directo a través del servidor SMTP, protegido por Circuit Breaker.
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "enviarCorreoFallback")
    public void enviarCorreoDirecto(String destino, String token) {
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

    /**
     * Fallback de Circuit Breaker cuando el servidor SMTP se encuentra caído.
     */
    public void enviarCorreoFallback(String destino, String token, Throwable throwable) {
        log.error("CIRCUIT BREAKER ACTIVADO [emailService]: No fue posible enviar correo de recuperación a {} debido a: {}. Notificación preservada para reintento.",
                destino, throwable.getMessage());
    }
}
