package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contenidos_institucionales")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ContenidoInstitucional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 80)
    private String seccion;

    @Column(length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    @Column(nullable = false)
    @Builder.Default
    private Boolean publicado = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private Usuario actualizadoPor;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
