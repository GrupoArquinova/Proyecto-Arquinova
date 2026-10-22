package com.constructora_backend.service;

import com.constructora_backend.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los reportes (y por tanto los Excel y PDF) tienen un tope de filas con un error claro. */
class ReporteTopeDeFilasTest {

    @Test
    @DisplayName("El tope es de 50.000 filas")
    void elTopeEs50000() {
        assertEquals(50_000, ReporteService.MAX_FILAS);
    }

    @Test
    @DisplayName("Hasta el tope no hay error")
    void enElTopeNoFalla() {
        List<Integer> filas = Collections.nCopies(ReporteService.MAX_FILAS, 1);

        assertDoesNotThrow(() -> ReporteService.verificarTope(filas));
    }

    @Test
    @DisplayName("Una fila de más (las consultas piden tope+1 para saberlo) da un error claro de 400")
    void porEncimaDelTopeFalla() {
        List<Integer> filas = Collections.nCopies(ReporteService.MAX_FILAS + 1, 1);

        BadRequestException error = assertThrows(BadRequestException.class, () -> ReporteService.verificarTope(filas));

        assertTrue(error.getMessage().contains("50000") || error.getMessage().contains("50.000"));
        assertTrue(error.getMessage().contains("filtros"));
    }
}
