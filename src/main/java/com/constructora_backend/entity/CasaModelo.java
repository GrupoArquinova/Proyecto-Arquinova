package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "casas_modelo", uniqueConstraints = {
        @UniqueConstraint(name = "uk_casas_proyecto_nombre", columnNames = {"proyecto_id", "nombre"})
})
@Data
public class CasaModelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Proyecto proyecto;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "area_construida_m2", precision = 10, scale = 2)
    private BigDecimal areaConstruidaM2;

    @Column(name = "numero_habitaciones")
    private Byte numeroHabitaciones;

    @Column(name = "numero_banos")
    private Byte numeroBanos;

    @Column(name = "tour_virtual_url", length = 1000)
    private String tourVirtualUrl;

    @Column(name = "plano_url", length = 1000)
    private String planoUrl;

    @Column(nullable = false)
    private Boolean publicado = true;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
