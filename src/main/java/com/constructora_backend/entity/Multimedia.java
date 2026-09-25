package com.constructora_backend.entity;

import com.constructora_backend.enums.TipoMultimedia;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "multimedia",
        indexes = {
        @Index(name = "idx_multimedia_proyecto", columnList = "proyecto_id, orden"),
                @Index(name = "idx_multimedia_lote", columnList = "lote_id, orden"),
                @Index(name = "idx_multimedia_zona", columnList = "zona_comun_id, orden"),
                @Index(name = "idx_multimedia_casa", columnList = "casa_modelo_id, orden"),
                @Index(name = "idx_multimedia_publicado", columnList = "publicado, activo")

        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Multimedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", foreignKey = @ForeignKey(name = "fk_multimedia_proyecto"))
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", foreignKey = @ForeignKey(name = "fk_multimedia_lote"))
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_comun_id", foreignKey = @ForeignKey(name = "fk_multimedia_zona"))
    private ZonaComun zonaComun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "casa_modelo_id", foreignKey = @ForeignKey(name = "fk_multimedia_casa"))
    private CasaModelo casaModelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMultimedia tipo;

    @Column(length = 200)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 1500)
    private String url;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Builder.Default
    @Column(nullable = false)
    private Integer orden = 1;

    @Builder.Default
    @Column(nullable = false)
    private Boolean portada = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean publicado = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", foreignKey = @ForeignKey(name = "fk_multimedia_usuario"))
    private Usuario creadoPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    private void validarUnSoloPadre() {
        int count = 0;
        if (proyecto != null) count++;
        if (lote != null) count++;
        if (zonaComun != null) count++;
        if (casaModelo != null) count++;

        if (count != 1) {
            throw new IllegalArgumentException("El archivo multimedia debe estar asociado exactamente a una entidad padre (Proyecto, Lote, Zona comun o casa Modelo).");
        }

        if (tamanoBytes != null && tamanoBytes < 0) {
            throw new IllegalArgumentException("El tamaño en bytes no puede ser negativo.");
        }
    }
}
