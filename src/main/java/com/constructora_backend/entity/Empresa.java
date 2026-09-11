package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "empresas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(unique = true, length = 30)
    private String nit;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String trayectoria;

    @Column(columnDefinition = "TEXT")
    private String servicios;

    @Column(name = "correo_comercial", length = 254)
    private String correoComercial;

    @Column(length = 30)
    private String telefono;

    @Column(length = 30)
    private String whatsapp;

    @Column(name = "sitio_web", length = 255)
    private String sitioWeb;

    @Column(length = 255)
    private String direccion;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
