package com.constructora_backend.repository;

import com.constructora_backend.entity.Etapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtapaRepository extends JpaRepository<Etapa, Long> {
    List<Etapa> findByProyectoIdOrderByOrdenAsc(Long proyectoId);
    List<Etapa> findByProyectoIdAndActivoTrueOrderByOrdenAsc(Long proyectoId);
    boolean existsByProyectoIdAndNombre(Long proyectoId, String nombre);
    boolean existsByProyectoIdAndNombreAndIdNot(Long proyectoId, String nombre, Long id);
}
