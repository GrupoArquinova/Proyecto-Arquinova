package com.constructora_backend.repository;

import com.constructora_backend.entity.SolicitudContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {

    List<SolicitudContacto> findAllByOrderByCreadoEnDesc();
    List<SolicitudContacto> findByEstadoIdOrderByCreadoEnDesc(Integer estadoId);
    List<SolicitudContacto> findByProyectoIdOrderByCreadoEnDesc(Long proyectoId);
    List<SolicitudContacto> findByLoteIdOrderByCreadoEnDesc(Long loteId);
}
