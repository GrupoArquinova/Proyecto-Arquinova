package com.constructora_backend.repository;

import com.constructora_backend.entity.ContenidoInstitucional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContenidoInstitucionalRepository extends JpaRepository<ContenidoInstitucional, Long> {

    List<ContenidoInstitucional> findByEmpresaId(Long empresaId);

    List<ContenidoInstitucional> findByEmpresaIdAndPublicadoTrue(Long empresaId);

    Optional<ContenidoInstitucional> findByEmpresaIdAndSeccion(Long empresaId, String seccion);

    boolean existsByEmpresaIdAndSeccion(Long empresaId, String seccion);
}
