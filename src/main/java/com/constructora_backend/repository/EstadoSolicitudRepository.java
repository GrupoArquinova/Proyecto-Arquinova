package com.constructora_backend.repository;

import com.constructora_backend.entity.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstadoSolicitudRepository extends JpaRepository<EstadoSolicitud, Integer> {

    List<EstadoSolicitud> findAllByOrderByOrdenAsc();
    List<EstadoSolicitud> findByActivoTrueOrderByOrdenAsc();
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Integer id);
}
