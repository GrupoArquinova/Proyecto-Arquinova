package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "estados_lote", uniqueConstraints = {
        @UniqueConstraint(name = "uk_estados_lote_nombre", columnNames = {"nombre"})
})
@Data
public class EstadoLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer id;

    @Column(nullable = false, length = 30)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer orden = 1;

    @Column(nullable = false)
    private Boolean activo = true;
}
