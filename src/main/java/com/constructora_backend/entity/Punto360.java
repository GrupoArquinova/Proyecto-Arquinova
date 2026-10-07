package com.constructora_backend.entity;

import com.constructora_backend.enums.EscenaPunto360;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Botón colocado sobre la imagen 360° del entorno, la vista aérea, el plano de urbanismo o la imagen de zonas
 * destacadas. Apunta a un lote, a una etapa (en el plano) o a una zona común y abre su tarjeta o su imagen 360°.
 */
@Entity
@Table(name = "puntos_360",
        indexes = @Index(name = "idx_puntos_360_proyecto", columnList = "proyecto_id, escena"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Punto360 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Proyecto proyecto;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EscenaPunto360 escena;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Etapa etapa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_comun_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ZonaComun zonaComun;

    @Column(nullable = false, length = 120)
    private String etiqueta;

    /** Ángulos en radianes dentro de la imagen 360° (ENTORNO y AEREA). */
    @Column(precision = 9, scale = 6)
    private BigDecimal yaw;

    @Column(precision = 9, scale = 6)
    private BigDecimal pitch;

    /** Posición en porcentaje (0–100) sobre el plano (URBANISMO) o la imagen de zonas destacadas (ZONAS). */
    @Column(name = "pos_x", precision = 6, scale = 3)
    private BigDecimal posX;

    @Column(name = "pos_y", precision = 6, scale = 3)
    private BigDecimal posY;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;
}
