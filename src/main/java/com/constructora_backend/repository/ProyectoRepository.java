package com.constructora_backend.repository;

import com.constructora_backend.entity.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    List<Proyecto> findByEmpresaId(Long empresaId);
    List<Proyecto> findByPublicadoTrueAndActivoTrue();
    Optional<Proyecto> findByEmpresaIdAndSlug(Long empresaId, String slug);
    boolean existsByEmpresaIdAndSlug(Long empresaId, String slug);

    /** Solo un proyecto por empresa puede ser el destacado del inicio: se lo quita a los demás. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Proyecto p set p.destacado = false where p.empresa.id = :empresaId and p.id <> :id and p.destacado = true")
    int quitarDestacadoDeOtros(@Param("empresaId") Long empresaId, @Param("id") Long id);
}
