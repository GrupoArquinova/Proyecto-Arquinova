package com.constructora_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "zonas_comunes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_zonas_proyecto_nombre", columnNames = {"proyecto_id", "nombre"})
})
@Data
public class ZonaComun {

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

    @Column(nullable = false)
    private Boolean publicado = true;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "zonaComun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ZonaComunImagen> imagenes = new ArrayList<>();

    public void agregarImagen(ZonaComunImagen imagen) {
        imagenes.add(imagen);
        imagen.setZonaComun(this);
    }

    public void removerImagen(ZonaComunImagen imagen) {
        imagenes.remove(imagen);
        imagen.setZonaComun(null);
    }
}
