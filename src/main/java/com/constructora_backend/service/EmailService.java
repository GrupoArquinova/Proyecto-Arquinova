package com.constructora_backend.service;

import com.constructora_backend.config.RabbitMQConfig;
import com.constructora_backend.dto.event.EmailEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
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

    /** Correo de la empresa que recibe el aviso de cada solicitud nueva (APP_CONTACTO_CORREO_AVISO); sin valor no se envía el aviso. */
    @Value("${app.contacto.correo-aviso:}")
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

    /** Orígenes permitidos del sitio (CORS_ALLOWED_ORIGINS): si no se configuró APP_SITIO_URL, el enlace de recuperación usa el primero. */
    @Value("${cors.allowed-origins:}")
    private String origenesPermitidos;

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
                log.info("Evento de correo encolado exitosamente en RabbitMQ para: {}", enmascarar(destino));
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
        try {
            mailSender.send(armarCorreoRecuperacion(destino, enlaceRecuperacion(token)));
            log.info("Correo de recuperación enviado exitosamente a {}", enmascarar(destino));
        } catch (MailException e) {
            log.error("Error al enviar el correo de recuperación a {}: {}", enmascarar(destino), e.getMessage());
            throw e;
        }
    }

    /** Correo para los registros: conserva la primera letra y el dominio ("a***@gmail.com") para no guardar direcciones completas. */
    static String enmascarar(String correo) {
        if (correo == null || correo.isBlank()) return "(sin correo)";
        String limpio = correo.trim();
        int arroba = limpio.indexOf('@');
        if (arroba < 1) return "***";
        return limpio.charAt(0) + "***" + limpio.substring(arroba);
    }

    /** Enlace que lleva al formulario de nueva contraseña: dirección pública del sitio (APP_SITIO_URL) o, si falta, el primer origen permitido. */
    String enlaceRecuperacion(String token) {
        String base = sitioUrl != null && !sitioUrl.isBlank() ? sitioUrl : primerOrigenPermitido();
        return base.trim().replaceAll("/+$", "") + "/reset-password?token=" + token;
    }

    private String primerOrigenPermitido() {
        if (origenesPermitidos == null || origenesPermitidos.isBlank()) return "";
        return origenesPermitidos.split(",")[0];
    }

    MimeMessage armarCorreoRecuperacion(String destino, String enlace) {
        MimeMessage mensaje = mailSender.createMimeMessage();
        try {
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, "UTF-8");
            ayudante.setFrom(remitente != null && !remitente.isBlank() ? remitente : "noreply@constructora.com", "Grupo Arquinova");
            ayudante.setTo(destino);
            ayudante.setSubject("Restablece tu contraseña - Grupo Arquinova");
            ayudante.setText(textoRecuperacion(enlace), htmlRecuperacion(enlace));
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("No se pudo preparar el correo de recuperación", e);
        }
        return mensaje;
    }

    String textoRecuperacion(String enlace) {
        return "Hola,\n\n"
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta de administración de Grupo Arquinova.\n\n"
                + "Para elegir una nueva contraseña, abre este enlace:\n"
                + enlace + "\n\n"
                + "El enlace vence en 15 minutos y solo se puede usar una vez.\n\n"
                + "Si no fuiste tú, ignora este mensaje: tu contraseña actual no cambia.\n\n"
                + "Grupo Arquinova S.A.S.\nNIT 901.397.504-2";
    }

    String htmlRecuperacion(String enlace) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;background:#f4f6f6;padding:24px;\">"
                + "<div style=\"max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px;color:#1e293b;\">"
                + "<p style=\"margin:0 0 4px;font-size:13px;letter-spacing:1px;text-transform:uppercase;color:#2c5953;font-weight:700;\">Grupo Arquinova</p>"
                + "<h1 style=\"margin:0 0 20px;font-size:22px;font-weight:600;\">Restablece tu contraseña</h1>"
                + "<p style=\"margin:0 0 16px;font-size:15px;line-height:1.6;\">Hola,</p>"
                + "<p style=\"margin:0 0 24px;font-size:15px;line-height:1.6;\">Recibimos una solicitud para restablecer la contraseña de tu cuenta de administración. Pulsa el botón para elegir una nueva.</p>"
                + "<p style=\"margin:0 0 24px;text-align:center;\"><a href=\"" + enlace + "\" style=\"display:inline-block;background:#2c5953;color:#ffffff;text-decoration:none;font-weight:600;font-size:15px;padding:12px 28px;border-radius:999px;\">Restablecer contraseña</a></p>"
                + "<p style=\"margin:0 0 8px;font-size:13px;line-height:1.5;color:#475569;\">Si el botón no funciona, copia y pega este enlace en tu navegador:</p>"
                + "<p style=\"margin:0 0 24px;font-size:13px;line-height:1.5;word-break:break-all;\"><a href=\"" + enlace + "\" style=\"color:#2c5953;\">" + enlace + "</a></p>"
                + "<p style=\"margin:0 0 8px;font-size:13px;line-height:1.5;color:#475569;\">El enlace vence en 15 minutos y solo se puede usar una vez.</p>"
                + "<p style=\"margin:0 0 24px;font-size:13px;line-height:1.5;color:#475569;\">Si no fuiste tú, ignora este mensaje: tu contraseña actual no cambia.</p>"
                + "<hr style=\"border:none;border-top:1px solid #e2e8f0;margin:0 0 16px;\">"
                + "<p style=\"margin:0;font-size:12px;color:#94a3b8;\">Grupo Arquinova S.A.S. · NIT 901.397.504-2</p>"
                + "</div></div>";
    }

    /**
     * Envía un correo de notificación al equipo de ventas cuando ingresa una nueva solicitud web.
     */
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    @CircuitBreaker(name = "emailService", fallbackMethod = "enviarCorreoNotificacionFallback")
    public void enviarCorreoNuevaSolicitud(String nombreCliente, String telefono, String correoCliente, String proyecto,
                                           String interes, String servicio, String mensajeCliente, String idioma) {
        if (correoAviso == null || correoAviso.isBlank()) {
            log.warn("No se envió el aviso de la solicitud: falta configurar APP_CONTACTO_CORREO_AVISO");
            return;
        }
        try {
            mailSender.send(armarAvisoSolicitud(nombreCliente, telefono, correoCliente, proyecto, interes, servicio, mensajeCliente, idioma));
            log.info("Correo de notificación de solicitud enviado exitosamente a la empresa");
        } catch (MailException e) {
            log.error("Error al enviar el correo de notificación de solicitud: {}", e.getMessage());
            throw e;
        }
    }

    MimeMessage armarAvisoSolicitud(String nombreCliente, String telefono, String correoCliente, String proyecto,
                                    String interes, String servicio, String mensajeCliente, String idioma) {
        CorreoAvisoSolicitud.Datos datos = new CorreoAvisoSolicitud.Datos(
                nombreCliente == null || nombreCliente.isBlank() ? "Sin nombre" : nombreCliente, telefono, correoCliente,
                proyecto, interes, servicio, idioma, mensajeCliente);

        MimeMessage mensaje = mailSender.createMimeMessage();
        try {
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, "UTF-8");
            ayudante.setFrom(remitente != null && !remitente.isBlank() ? remitente : "noreply@constructora.com", "Grupo Arquinova · Sitio web");
            ayudante.setTo(correoAviso);
            // Si el cliente dejó correo, al responder el aviso la respuesta le llega a él
            if (correoCliente != null && !correoCliente.isBlank()) {
                ayudante.setReplyTo(correoCliente.trim());
            }
            ayudante.setSubject(CorreoAvisoSolicitud.asunto(datos));
            ayudante.setText(CorreoAvisoSolicitud.texto(datos), CorreoAvisoSolicitud.html(datos));
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("No se pudo preparar el aviso de la solicitud", e);
        }
        return mensaje;
    }

    /**
     * Confirma al cliente que su solicitud llegó. Solo se envía si dejó correo y como máximo una vez por hora a la
     * misma dirección. El texto es neutro: no repite lo que escribió el cliente ni promete plazos o precios.
     */
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    @CircuitBreaker(name = "emailService", fallbackMethod = "enviarConfirmacionFallback")
    public void enviarConfirmacionSolicitud(String nombreCliente, String correoCliente, String proyecto, String interes, String mensajeCliente, String idioma) {
        if (!confirmacionActiva || correoCliente == null || correoCliente.isBlank()) {
            return;
        }
        if (!limitador.permitir(correoCliente)) {
            log.info("Confirmación omitida: ya se envió una a esa dirección en la última hora");
            return;
        }
        mailSender.send(armarConfirmacion(nombreCliente, correoCliente, proyecto, interes, mensajeCliente, idioma));
        log.info("Confirmación de solicitud enviada al cliente");
    }

    MimeMessage armarConfirmacion(String nombreCliente, String correoCliente, String proyecto, String interes, String mensajeCliente, String idioma) {
        CorreoConfirmacionSolicitud.Datos datos = new CorreoConfirmacionSolicitud.Datos(
                "en".equals(idioma) ? "en" : "es", nombreCliente, proyecto, interes, mensajeCliente,
                whatsappEmpresa, correoAviso, urlPolitica());

        MimeMessage mensaje = mailSender.createMimeMessage();
        try {
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, "UTF-8");
            ayudante.setFrom(remitente != null && !remitente.isBlank() ? remitente : "noreply@constructora.com", "Grupo Arquinova");
            ayudante.setTo(correoCliente.trim());
            if (correoAviso != null && !correoAviso.isBlank()) ayudante.setReplyTo(correoAviso);
            ayudante.setSubject(CorreoConfirmacionSolicitud.asunto(datos));
            ayudante.setText(CorreoConfirmacionSolicitud.texto(datos), CorreoConfirmacionSolicitud.html(datos));
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("No se pudo preparar la confirmación de la solicitud", e);
        }
        return mensaje;
    }

    /** Enlace a la política de tratamiento de datos; vacío mientras no se configure la dirección del sitio. */
    String urlPolitica() {
        if (sitioUrl == null || sitioUrl.isBlank()) return "";
        return sitioUrl.trim().replaceAll("/+$", "") + "/legal/politica-datos";
    }

    public void enviarConfirmacionFallback(String nombreCliente, String correoCliente, String proyecto, String interes, String mensajeCliente, String idioma, Throwable throwable) {
        log.error("CIRCUIT BREAKER ACTIVADO [emailService]: No fue posible enviar la confirmación al cliente debido a: {}.",
                throwable.getMessage());
    }

    /**
     * Fallback de Circuit Breaker cuando el servidor SMTP se encuentra caído.
     */
    public void enviarCorreoFallback(String destino, String token, Throwable throwable) {
        log.error("CIRCUIT BREAKER ACTIVADO [emailService]: No fue posible enviar correo de recuperación a {} debido a: {}. Notificación preservada para reintento.",
                enmascarar(destino), throwable.getMessage());
    }

    /**
     * Fallback de Circuit Breaker para el envío de notificaciones corporativas.
     */
    public void enviarCorreoNotificacionFallback(String nombreCliente, String telefono, String correoCliente, String proyecto, String interes, String servicio, String mensajeCliente, String idioma, Throwable throwable) {
        log.error("CIRCUIT BREAKER ACTIVADO [emailService]: No fue posible enviar la notificación de contacto debido a: {}.",
                throwable.getMessage());
    }
}