package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "estados_solicitud", uniqueConstraints = {
        @UniqueConstraint(name = "uk_estados_solicitud_nombre", columnNames = {"nombre"})
}, indexes = {
        @Index(name = "idx_estados_solicitud_activo", columnList = "activo")
})
@Data
public class EstadoSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer id;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer orden = 1;

    @Column(nullable = false)
    private Boolean activo = true;
}
