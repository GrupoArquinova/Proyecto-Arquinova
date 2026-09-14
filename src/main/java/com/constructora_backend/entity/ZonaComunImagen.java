package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "zona_comun_imagenes")
@Data
public class ZonaComunImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_comun_id", nullable = false)
    private ZonaComun zonaComun;

    @Column(name = "imagen_url", nullable = false, length = 1000)
    private String imagenUrl;

    @Column(length = 150)
    private String titulo;

    @Column(nullable = false)
    private Short orden = 1;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
