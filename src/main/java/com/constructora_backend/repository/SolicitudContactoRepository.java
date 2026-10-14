package com.constructora_backend.repository;

import com.constructora_backend.entity.SolicitudContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {

    /** Quita la referencia a una tipología que se va a eliminar (la columna no tiene llave foránea). */
    @Modifying
    @Query("update SolicitudContacto s set s.casaModelo = null where s.casaModelo.id = :casaModeloId")
    int quitarCasaModelo(@Param("casaModeloId") Long casaModeloId);

    List<SolicitudContacto> findAllByOrderByCreadoEnDesc();
    List<SolicitudContacto> findByEstadoIdOrderByCreadoEnDesc(Integer estadoId);
    List<SolicitudContacto> findByProyectoIdOrderByCreadoEnDesc(Long proyectoId);
    List<SolicitudContacto> findByLoteIdOrderByCreadoEnDesc(Long loteId);
}
