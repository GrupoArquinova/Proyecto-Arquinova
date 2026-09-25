package com.constructora_backend.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Evento inmutable de correo para distribución asíncrona a través de RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent implements Serializable {

    private String tipo;
    private String destinatario;
    private String token;
    private String asunto;
    private LocalDateTime generadoEn;
}
