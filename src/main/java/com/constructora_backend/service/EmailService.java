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

    /** Correo de la empresa que recibe el aviso de cada solicitud nueva (configurable con APP_CONTACTO_CORREO_AVISO). */
    @Value("${app.contacto.correo-aviso:grupoarquinova1@gmail.com}")
    private String correoAviso;

    /** Si es false no se envía la confirmación al cliente (APP_CONTACTO_CONFIRMACION_ACTIVA). */
    @Value("${app.contacto.confirmacion-activa:true}")
    private boolean confirmacionActiva;

    /** WhatsApp de la empresa que se muestra en la confirmación (APP_CONTACTO_WHATSAPP). */
    @Value("${app.contacto.whatsapp:+57 316 865 3715}")
    private String whatsappEmpresa;

    /** Dirección pública del sitio (APP_SITIO_URL), para enlazar la política de datos en la confirmación. Vacía = sin enlace. */
    @Value("${app.sitio-url:}")
    private String sitioUrl;

    private final LimitadorConfirmaciones limitador;

    @Autowired
    public EmailService(JavaMailSender mailSender,
                        @Autowired(required = false) RabbitTemplate rabbitTemplate,
                        @Autowired(required = false) LimitadorConfirmaciones limitador) {
        this.mailSender = mailSender;
        this.rabbitTemplate = rabbitTemplate;
        this.limitador = limitador != null ? limitador : new LimitadorConfirmaciones();
    }

    public EmailService(JavaMailSender mailSender) {
        this(mailSender, null, null);
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
     * Envía un correo de notificación al equipo de ventas cuando ingresa una nueva solicitud web.
     */
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    @CircuitBreaker(name = "emailService", fallbackMethod = "enviarCorreoNotificacionFallback")
    public void enviarCorreoNuevaSolicitud(String nombreCliente, String telefono, String correoCliente, String proyecto,
                                           String servicio, String mensajeCliente, String idioma) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente != null && !remitente.isBlank() ? remitente : "noreply@constructora.com");
        mensaje.setTo(correoAviso);
        // Si el cliente dejó correo, al responder el aviso la respuesta le llega a él
        if (correoCliente != null && !correoCliente.isBlank()) {
            mensaje.setReplyTo(correoCliente);
        }
        mensaje.setSubject("Nueva solicitud de contacto de: " + nombreCliente);
        mensaje.setText("Has recibido una nueva solicitud de información desde la web:\n\n"
                + "Nombre: " + nombreCliente + "\n"
                + "Teléfono: " + (telefono != null && !telefono.isBlank() ? telefono : "No indicó") + "\n"
                + "Correo: " + (correoCliente != null && !correoCliente.isBlank() ? correoCliente : "No indicó") + "\n"
                + "Proyecto de interés: " + (proyecto != null ? proyecto : "No especificado") + "\n"
                + "Servicio de interés: " + (servicio != null && !servicio.isBlank() ? servicio : "No especificado") + "\n"
                + "Idioma del sitio: " + ("en".equals(idioma) ? "Inglés (conviene responderle en inglés)" : "Español") + "\n"
                + "Mensaje del cliente:\n" + (mensajeCliente != null && !mensajeCliente.isBlank() ? mensajeCliente : "(sin mensaje)") + "\n\n"
                + "--- \nEste correo fue generado automáticamente por el sistema de la constructora.");

        try {
            mailSender.send(mensaje);
            log.info("Correo de notificación de solicitud enviado exitosamente a la empresa para el cliente: {}", nombreCliente);
        } catch (MailException e) {
            log.error("Error al enviar el correo de notificación de solicitud: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Confirma al cliente que su solicitud llegó. Solo se envía si dejó correo y como máximo una vez por hora a la
     * misma dirección. El texto es neutro: no repite lo que escribió el cliente ni promete plazos o precios.
     */
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    @CircuitBreaker(name = "emailService", fallbackMethod = "enviarConfirmacionFallback")
    public void enviarConfirmacionSolicitud(String nombreCliente, String correoCliente, String proyecto, String idioma) {
        if (!confirmacionActiva || correoCliente == null || correoCliente.isBlank()) {
            return;
        }
        if (!limitador.permitir(correoCliente)) {
            log.info("Confirmación omitida: ya se envió una a esa dirección en la última hora");
            return;
        }
        mailSender.send(armarConfirmacion(nombreCliente, correoCliente, proyecto, idioma));
        log.info("Confirmación de solicitud enviada al cliente");
    }

    SimpleMailMessage armarConfirmacion(String nombreCliente, String correoCliente, String proyecto, String idioma) {
        boolean ingles = "en".equals(idioma);
        String saludo = ingles
                ? (nombreCliente != null && !nombreCliente.isBlank() ? "Hello " + nombreCliente.trim() + "," : "Hello,")
                : (nombreCliente != null && !nombreCliente.isBlank() ? "Hola " + nombreCliente.trim() + "," : "Hola,");
        String sobre = proyecto != null && !proyecto.isBlank() ? (ingles ? " about " : " sobre ") + proyecto : "";

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente != null && !remitente.isBlank() ? remitente : "noreply@constructora.com");
        mensaje.setTo(correoCliente.trim());
        mensaje.setReplyTo(correoAviso);
        mensaje.setSubject(ingles ? "We received your request - Grupo Arquinova" : "Recibimos tu solicitud - Grupo Arquinova");
        if (ingles) {
            mensaje.setText(saludo + "\n\n"
                    + "We received your request" + sobre + " and our team will get in touch with you.\n\n"
                    + "If you need to talk to us sooner, write to us on WhatsApp at " + whatsappEmpresa + ".\n\n"
                    + enlacePolitica(true)
                    + "If you did not leave this email address on our website, you can ignore this message.\n\n"
                    + "Grupo Arquinova S.A.S.\nNIT 901.397.504-2");
        } else {
            mensaje.setText(saludo + "\n\n"
                    + "Recibimos tu solicitud" + sobre + " y nuestro equipo se pondrá en contacto contigo.\n\n"
                    + "Si necesitas hablar con nosotros antes, escríbenos por WhatsApp al " + whatsappEmpresa + ".\n\n"
                    + enlacePolitica(false)
                    + "Si no fuiste tú quien dejó este correo en nuestro sitio web, puedes ignorar este mensaje.\n\n"
                    + "Grupo Arquinova S.A.S.\nNIT 901.397.504-2");
        }
        return mensaje;
    }

    /** Línea con el enlace a la política de tratamiento de datos; solo si ya se configuró la dirección del sitio. */
    private String enlacePolitica(boolean ingles) {
        if (sitioUrl == null || sitioUrl.isBlank()) return "";
        return (ingles ? "This is how we handle your personal data: " : "Así tratamos tus datos personales: ") + sitioUrl.trim().replaceAll("/+$", "") + "/legal/politica-datos\n\n";
    }

    public void enviarConfirmacionFallback(String nombreCliente, String correoCliente, String proyecto, String idioma, Throwable throwable) {
        log.error("CIRCUIT BREAKER ACTIVADO [emailService]: No fue posible enviar la confirmación al cliente debido a: {}.",
                throwable.getMessage());
    }

    /**
     * Fallback de Circuit Breaker cuando el servidor SMTP se encuentra caído.
     */
    public void enviarCorreoFallback(String destino, String token, Throwable throwable) {
        log.error("CIRCUIT BREAKER ACTIVADO [emailService]: No fue posible enviar correo de recuperación a {} debido a: {}. Notificación preservada para reintento.",
                destino, throwable.getMessage());
    }

    /**
     * Fallback de Circuit Breaker para el envío de notificaciones corporativas.
     */
    public void enviarCorreoNotificacionFallback(String nombreCliente, String telefono, String correoCliente, String proyecto, String servicio, String mensajeCliente, String idioma, Throwable throwable) {
        log.error("CIRCUIT BREAKER ACTIVADO [emailService]: No fue posible enviar la notificación de contacto para {} debido a: {}.",
                nombreCliente, throwable.getMessage());
    }
}