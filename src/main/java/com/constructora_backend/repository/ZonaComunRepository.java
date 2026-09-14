package com.constructora_backend.repository;

import com.constructora_backend.entity.ZonaComun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZonaComunRepository  extends JpaRepository<ZonaComun, Long> {

    List<ZonaComun> findByProyectoId(Long proyectoId);
    List<ZonaComun> findByProyectoIdAndPublicadoTrueAndActivoTrue(Long proyecto);
    boolean existsByProyectoIdAndNombre(Long proyectoId, String nombre);
    boolean existsByProyectoIdAndNombreAndIdNot(Long proyectoId, String nombre, Long id);
}
