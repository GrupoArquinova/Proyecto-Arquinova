package com.constructora_backend.repository;

import com.constructora_backend.entity.Punto360;
import com.constructora_backend.enums.EscenaPunto360;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Punto360Repository extends JpaRepository<Punto360, Long> {

    List<Punto360> findByProyectoIdAndActivoTrueOrderByIdAsc(Long proyectoId);

    List<Punto360> findByProyectoIdAndEscenaAndActivoTrueOrderByIdAsc(Long proyectoId, EscenaPunto360 escena);
}
