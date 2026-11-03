package com.constructora_backend.repository;

import com.constructora_backend.entity.EventoAnalitica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventoAnaliticaRepository extends JpaRepository<EventoAnalitica, Long> {

    /** Eventos del periodo [desde, hasta) agrupados por tipo: [tipo, total]. */
    @Query("select e.tipo, count(e) from EventoAnalitica e where e.creadoEn >= :desde and e.creadoEn < :hasta group by e.tipo")
    List<Object[]> contarPorTipo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Vistas de página por día: [fecha, total] (solo los días con datos). */
    @Query("select cast(e.creadoEn as LocalDate), count(e) from EventoAnalitica e "
            + "where e.tipo = com.constructora_backend.enums.TipoEventoAnalitica.VISTA_PAGINA "
            + "and e.creadoEn >= :desde and e.creadoEn < :hasta group by cast(e.creadoEn as LocalDate)")
    List<Object[]> vistasPaginaPorDia(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Vistas de cada proyecto: [proyectoId, nombre, total], de más a menos vistas. Ignora eventos de proyectos que ya no existen. */
    @Query("select p.id, p.nombre, count(e) from EventoAnalitica e, Proyecto p "
            + "where p.id = e.proyectoId and e.tipo = com.constructora_backend.enums.TipoEventoAnalitica.VISTA_PROYECTO "
            + "and e.creadoEn >= :desde and e.creadoEn < :hasta group by p.id, p.nombre order by count(e) desc, p.nombre")
    List<Object[]> vistasPorProyecto(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Borra los eventos anteriores a la fecha (retención de datos); devuelve cuántos eran. */
    @Modifying
    @Query("delete from EventoAnalitica e where e.creadoEn < :limite")
    int eliminarAnterioresA(@Param("limite") LocalDateTime limite);
}
