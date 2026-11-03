package com.constructora_backend.entity;

import com.constructora_backend.enums.TemaChat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Una pregunta hecha al asistente de IA. Se guarda SOLO la pregunta (con correos y teléfonos ya reemplazados por
 * [dato omitido]), su idioma y su tema: nunca la respuesta, el historial de la conversación, la IP ni identificadores.
 */
@Entity
@Table(name = "chat_preguntas")
@Getter @Setter
@NoArgsConstructor
public class ChatPregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String pregunta;

    /** "es" o "en"; vacío si el sitio no lo informó. */
    @Column(length = 5)
    private String idioma;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TemaChat tema;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
