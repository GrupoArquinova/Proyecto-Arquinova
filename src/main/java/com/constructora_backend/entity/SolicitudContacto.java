package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_contacto", indexes = {
        @Index(name = "idx_solicitudes_estado_fecha", columnList = "estado_id, creado_en"),
        @Index(name = "idx_solicitudes_proyecto", columnList = "proyecto_id"),
        @Index(name = "idx_solicitudes_lote", columnList = "lote_id"),
        @Index(name = "idx_solicitudes_fecha", columnList = "creado_en")
})
@Data
public class SolicitudContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private EstadoSolicitud estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Lote lote;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 254)
    private String correo;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "consentimiento_datos", nullable = false)
    private Boolean cosentimientoDatos = false;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendida_por")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario atendidaPor;

    @Column(name = "atendida_en")
    private LocalDateTime atendidaEn;

    @Column(name = "observaciones_internas", columnDefinition = "TEXT")
    private String observacionesInternas;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
