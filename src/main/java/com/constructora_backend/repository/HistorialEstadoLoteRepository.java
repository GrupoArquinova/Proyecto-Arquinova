package com.constructora_backend.repository;

import com.constructora_backend.entity.HistorialEstadoLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoLoteRepository extends JpaRepository<HistorialEstadoLote, Long> {

    List<HistorialEstadoLote> findByLote_IdOrderByCambiadoEnDesc(Long loteId);
}
