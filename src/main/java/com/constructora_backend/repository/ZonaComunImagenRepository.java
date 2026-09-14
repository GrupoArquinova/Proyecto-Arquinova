package com.constructora_backend.repository;

import com.constructora_backend.entity.ZonaComunImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZonaComunImagenRepository extends JpaRepository<ZonaComunImagen, Long> {

    List<ZonaComunImagen> findByZonaComunIdOrderByOrdenAsc(Long zonaComunId);

    Optional<ZonaComunImagen> findByZonaComun_IdAndEsPrincipalTrue(Long zonaComunId);

    @Modifying
    @Query("UPDATE ZonaComunImagen z SET z.esPrincipal = false WHERE z.zonaComun.id = :zonasComunId")
    void desmarcarPrincipalesDeZonaComun(@Param("zonasComunId") Long zonaComunId);

    void deleteByZonaComunId(Long zonaComunId);
}
