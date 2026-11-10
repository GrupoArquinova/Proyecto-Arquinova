package com.constructora_backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de RabbitMQ para procesamiento asíncrono y desacoplado de eventos empresariales.
 * Incluye colas duraderas, Exchange directo y Dead Letter Queue (DLQ) para tolerancia a fallos.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_DIRECT = "constructora.exchange";
    public static final String EXCHANGE_DLX = "constructora.dlx";

    public static final String QUEUE_EMAIL = "constructora.email.queue";
    public static final String QUEUE_EMAIL_DLQ = "constructora.email.dlq";

    public static final String ROUTING_KEY_EMAIL = "constructora.email.key";
    public static final String ROUTING_KEY_EMAIL_DLQ = "constructora.email.dlq.key";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_DIRECT, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    public Queue emailQueue() {
        Map<String, Object> args = new HashMap<>();
        // Configuración de Dead Letter Exchange para mensajes fallidos
        args.put("x-dead-letter-exchange", EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_EMAIL_DLQ);
        return new Queue(QUEUE_EMAIL, true, false, false, args);
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return new Queue(QUEUE_EMAIL_DLQ, true, false, false);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(emailQueue).to(directExchange).with(ROUTING_KEY_EMAIL);
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDeadLetterQueue).to(deadLetterExchange).with(ROUTING_KEY_EMAIL_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
