package com.constructora_backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Con la base de datos real: el contexto del asistente lee relaciones perezosas (lote → etapa → proyecto) y solo funciona dentro de
 * una transacción. Con repositorios simulados ese fallo (LazyInitializationException) no se ve; esta prueba lo atrapa.
 */
@SpringBootTest
class ChatContextoIntegracionTest {

    @Autowired
    private ChatContextoService contextoService;

    @Test
    void armaElContextoConLosDatosRealesSinErroresDeSesion() {
        String contexto = assertDoesNotThrow(() -> contextoService.obtener());

        assertTrue(contexto.contains("Grupo Arquinova S.A.S."));
    }
}
