package com.constructora_backend.repository;

import com.constructora_backend.entity.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    List<Proyecto> findByEmpresaId(Long empresaId);
    List<Proyecto> findByPublicadoTrueAndActivoTrue();
    Optional<Proyecto> findByEmpresaIdAndSlug(Long empresaId, String slug);
    boolean existsByEmpresaIdAndSlug(Long empresaId, String slug);
}
