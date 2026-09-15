package com.constructora_backend.repository;

import com.constructora_backend.entity.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {
    Optional<Ubicacion> findByProyectoId(Long proyectoId);
    boolean existsByProyectoId(Long proyectoId);
    List<Ubicacion> findByCiudadIgnoreCase(String ciudad);
    void deleteByProyectoId(Long proyectoId);
}
