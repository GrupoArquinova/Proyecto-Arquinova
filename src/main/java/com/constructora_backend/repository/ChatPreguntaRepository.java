package com.constructora_backend.repository;

import com.constructora_backend.entity.ChatPregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatPreguntaRepository extends JpaRepository<ChatPregunta, Long> {

    /** Preguntas del periodo [desde, hasta) por tema: [tema, total]. */
    @Query("select c.tema, count(c) from ChatPregunta c where c.creadoEn >= :desde and c.creadoEn < :hasta group by c.tema")
    List<Object[]> contarPorTema(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    long countByCreadoEnGreaterThanEqualAndCreadoEnLessThan(LocalDateTime desde, LocalDateTime hasta);

    /** Las 30 preguntas más recientes del periodo. */
    List<ChatPregunta> findTop30ByCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnDescIdDesc(LocalDateTime desde, LocalDateTime hasta);

    /** Borra las preguntas anteriores a la fecha (retención de datos); devuelve cuántas eran. */
    @Modifying
    @Query("delete from ChatPregunta c where c.creadoEn < :limite")
    int eliminarAnterioresA(@Param("limite") LocalDateTime limite);
}
