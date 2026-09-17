package com.constructora_backend.repository;

import com.constructora_backend.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByEtapaId(Long etapaId);
    List<Lote> findByEtapaIdAndPublicadoTrueAndActivoTrue(Long etapaId);
    List<Lote> findByEstadoId(Integer estadoId);
    boolean existsByEtapaIdAndCodigo(Long etapaId, String codigo);
    boolean existsByEtapaIdAndCodigoAndIdNot(Long etapaId, String codigo, Long id);
}
