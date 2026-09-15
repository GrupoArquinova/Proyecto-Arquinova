package com.constructora_backend.repository;

import com.constructora_backend.entity.CasaModelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CasaModeloRepository extends JpaRepository<CasaModelo, Long> {
    List<CasaModelo> findByProyectoId(Long proyectoId);
    List<CasaModelo> findByProyectoIdAndPublicadoTrueAndActivoTrue(Long proyectoId);
    boolean existsByProyectoIdAndNombre(Long proyectoId, String nombre);
    boolean existsByProyectoIdAndNombreAndIdNot(Long proyectoId, String nombre, Long id);
}
