package com.constructora_backend.entity;

import com.constructora_backend.enums.EstadoProyecto;
import com.constructora_backend.enums.TipoProyecto;
import com.constructora_backend.enums.TipoRegistroProyecto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proyectos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_proyectos_empresa_slug", columnNames = {"empresa_id", "slug"})
})
@Data
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "descripcion_en", columnDefinition = "TEXT")
    private String descripcionEn;

    // Texto (VARCHAR) y no ENUM de MySQL: así agregar un valor al enum de Java no exige alterar la tabla
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado_proyecto", nullable = false, length = 30)
    private EstadoProyecto estadoProyecto = EstadoProyecto.EN_DISENO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_registro", nullable = false, length = 20)
    private TipoRegistroProyecto tipoRegistro = TipoRegistroProyecto.OFERTA_COMERCIAL;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_proyecto", length = 20)
    private TipoProyecto tipoProyecto;

    /** Papel de Arquinova en el proyecto (diseño, estudios, licencias, estructuración, construcción, comercialización). */
    @Column(length = 255)
    private String participacion;

    @Column(name = "participacion_en", length = 255)
    private String participacionEn;

    /** Proyecto que se destaca en el inicio. Solo uno por empresa. */
    @Column(nullable = false)
    private Boolean destacado = false;

    @Column(nullable = false)
    private Boolean publicado = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_lanzamiento")
    private LocalDate fechaLanzamiento;

    @Column(name = "imagen_url", length = 500, columnDefinition = "LONGTEXT")
    private String imagenUrl;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private Usuario actualizadoPor;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}