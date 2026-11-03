package com.constructora_backend.repository;

import com.constructora_backend.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {
    List<Auditoria> findByEntidadAndEntidadId(String entidad, Long entidadId);
    List<Auditoria> findByUsuarioId(Long usuarioId);

    /** Búsqueda filtrada y paginada del historial; trae el usuario en la misma consulta (sin una consulta por fila). */
    @Override
    @EntityGraph(attributePaths = "usuario")
    Page<Auditoria> findAll(Specification<Auditoria> filtro, Pageable pagina);

    @Query("select distinct a.entidad from Auditoria a order by a.entidad")
    List<String> entidadesRegistradas();

    @Query("select distinct a.accion from Auditoria a order by a.accion")
    List<String> accionesRegistradas();

    /** Borra el historial anterior a la fecha (retención de datos); devuelve cuántos registros eran. */
    @Modifying
    @Query("delete from Auditoria a where a.creadoEn < :limite")
    int eliminarAnterioresA(@Param("limite") LocalDateTime limite);
}
