package com.constructora_backend.service;

import com.constructora_backend.config.RabbitMQConfig;
import com.constructora_backend.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consumidor desacoplado de eventos de mensajería desde RabbitMQ.
 * Procesa en segundo plano los envíos de correo sin bloquear las peticiones HTTP del backend.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailQueueConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL)
    public void procesarEventoCorreo(EmailEvent evento) {
        if (evento == null || evento.getDestinatario() == null) {
            log.warn("Mensaje descartado de RabbitMQ: evento o destinatario nulo");
            return;
        }

        log.info("Mensaje recibido desde RabbitMQ para destinatario: {}", evento.getDestinatario());
        try {
            emailService.enviarCorreoDirecto(evento.getDestinatario(), evento.getToken());
        } catch (Exception ex) {
            log.error("Fallo al procesar evento de correo para {}. RabbitMQ enviará a DLQ si excede reintentos: {}",
                    evento.getDestinatario(), ex.getMessage());
            throw ex; // Permite que RabbitMQ gestione el reintento o desvío a DLQ
        }
    }
}
