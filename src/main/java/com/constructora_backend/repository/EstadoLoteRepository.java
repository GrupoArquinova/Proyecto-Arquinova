package com.constructora_backend.repository;

import com.constructora_backend.entity.EstadoLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoLoteRepository  extends JpaRepository<EstadoLote, Integer> {

    List<EstadoLote> findAllByOrderByOrdenAsc();
    List<EstadoLote> findByActivoTrueOrderByOrdenAsc();
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Integer id);
}
