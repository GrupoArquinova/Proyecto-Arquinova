package com.constructora_backend.entity;

import com.constructora_backend.enums.TipoEventoAnalitica;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Un evento de la analítica propia del sitio. A propósito NO guarda IP, user-agent, identificador de visitante ni nada
 * que permita reconocer a una persona: solo qué pasó, en qué proyecto (si aplica), en qué ruta y cuándo.
 */
@Entity
@Table(name = "eventos_analitica")
@Getter @Setter
@NoArgsConstructor
public class EventoAnalitica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TipoEventoAnalitica tipo;

    /** Sin llave foránea: los eventos antiguos se depuran por fecha, aunque el proyecto ya no exista. */
    @Column(name = "proyecto_id")
    private Long proyectoId;

    /** Ruta del sitio sin querystring (por ejemplo /proyectos/6/ubicacion), máximo 200 caracteres. */
    @Column(length = 200)
    private String ruta;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
