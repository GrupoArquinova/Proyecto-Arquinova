package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "historial_estado_lote",
        indexes = {
                @Index(
                        name = "idx_historial_lote_fecha",
                        columnList = "lote_id, cambiado_en"
                ),
                @Index(
                        name = "idx_historial_usuario",
                        columnList = "cambiado_por"
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstadoLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "lote_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_historial_lote")
    )
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "estado_anterior_id",
            foreignKey = @ForeignKey(name = "fk_historial_estado_anterior")
    )
    private EstadoLote estadoAnterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "estado_nuevo_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_historial_estado_nuevo")
    )
    private EstadoLote estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cambiado_por",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_historial_usuario")
    )
    private Usuario usuario;

    @Column(name = "observacion", length = 500)
    private String observaciones;

    @CreationTimestamp
    @Column(
            name = "cambiado_en",
            nullable = false,
            updatable = false
    )
    private LocalDateTime cambiadoEn;
}