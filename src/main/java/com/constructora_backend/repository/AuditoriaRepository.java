package com.constructora_backend.repository;

import com.constructora_backend.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findByEntidadAndEntidadId(String entidad, Long entidadId);
    List<Auditoria> findByUsuarioId(Long usuarioId);
}
