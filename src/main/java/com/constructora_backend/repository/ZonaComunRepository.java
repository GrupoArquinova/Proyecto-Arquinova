package com.constructora_backend.repository;

import com.constructora_backend.entity.ZonaComun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZonaComunRepository  extends JpaRepository<ZonaComun, Long> {

    List<ZonaComun> findByProyectoId(Long proyectoId);
    List<ZonaComun> findByProyectoIdAndPublicadoTrueAndActivoTrue(Long proyecto);

    /** Lo que ve el sitio público: la zona publicada y activa DENTRO de un proyecto también publicado y activo. */
    @org.springframework.data.jpa.repository.Query("select z from ZonaComun z where z.proyecto.id = :proyectoId "
            + "and z.publicado = true and z.activo = true and z.proyecto.publicado = true and z.proyecto.activo = true")
    List<ZonaComun> findVisiblesAlPublicoPorProyecto(@org.springframework.data.repository.query.Param("proyectoId") Long proyectoId);
    boolean existsByProyectoIdAndNombre(Long proyectoId, String nombre);
    boolean existsByProyectoIdAndNombreAndIdNot(Long proyectoId, String nombre, Long id);
}
