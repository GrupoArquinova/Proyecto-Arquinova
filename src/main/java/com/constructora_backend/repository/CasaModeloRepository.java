package com.constructora_backend.repository;

import com.constructora_backend.entity.CasaModelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CasaModeloRepository extends JpaRepository<CasaModelo, Long> {
    List<CasaModelo> findByProyectoId(Long proyectoId);
    List<CasaModelo> findByProyectoIdAndPublicadoTrueAndActivoTrue(Long proyectoId);

    /** Lo que ve el sitio público: la casa publicada y activa DENTRO de un proyecto también publicado y activo. */
    @org.springframework.data.jpa.repository.Query("select c from CasaModelo c where c.proyecto.id = :proyectoId "
            + "and c.publicado = true and c.activo = true and c.proyecto.publicado = true and c.proyecto.activo = true")
    List<CasaModelo> findVisiblesAlPublicoPorProyecto(@org.springframework.data.repository.query.Param("proyectoId") Long proyectoId);
    boolean existsByProyectoIdAndNombre(Long proyectoId, String nombre);
    boolean existsByProyectoIdAndNombreAndIdNot(Long proyectoId, String nombre, Long id);
}
