package com.constructora_backend.repository;

import com.constructora_backend.entity.Multimedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultimediaRepository extends JpaRepository<Multimedia, Long> {

    List<Multimedia> findByProyectoIdAndActivoTrueOrderByOrdenAsc(Long proyectoId);
    List<Multimedia> findByLoteIdAndActivoTrueOrderByOrdenAsc(Long loteId);
    List<Multimedia> findByZonaComunIdAndActivoTrueOrderByOrdenAsc(Long zonaComunId);
    List<Multimedia> findByCasaModelo_IdAndActivoTrueOrderByOrdenAsc(Long casaModeloId);

    List<Multimedia> findByProyectoIdAndPublicadoTrueAndActivoTrueOrderByOrdenAsc(Long proyectoId);
    List<Multimedia> findByLoteIdAndPublicadoTrueAndActivoTrueOrderByOrdenAsc(Long loteId);
}