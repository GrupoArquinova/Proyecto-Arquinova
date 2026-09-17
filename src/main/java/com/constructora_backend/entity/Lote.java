package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lotes_etapa_codigo", columnNames = {"etapa_id", "codigo"})
})
@Data
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Etapa etapa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private EstadoLote estado;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(length = 120)
    private String nombre;

    @Column(name = "area_m2", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaM2;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String caracteristicas;

    @Column(name = "posicion_x", precision = 12, scale = 6)
    private BigDecimal posicionX;

    @Column(name = "posicion_y", precision = 12, scale = 6)
    private BigDecimal posicionY;

    @Column(nullable = false)
    private Boolean publicado = true;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario actualizadoPor;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

}
