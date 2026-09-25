package com.constructora_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Test
    @DisplayName("No debe bloquear una cuenta con menos de 5 intentos fallidos")
    void cuentaNoBloqueadaBajoUmbral() {
        String correo = "usuario@empresa.com";

        for (int i = 0; i < 4; i++) {
            loginAttemptService.registrarFallo(correo);
        }

        assertFalse(loginAttemptService.estaBloqueado(correo));
    }

    @Test
    @DisplayName("Debe bloquear la cuenta exactamente al alcanzar 5 intentos fallidos")
    void cuentaBloqueadaAlQuintoIntento() {
        String correo = "victima@empresa.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.registrarFallo(correo);
        }

        assertTrue(loginAttemptService.estaBloqueado(correo));
    }

    @Test
    @DisplayName("Debe restablecer el contador al invocar limpiarIntentos tras un login exitoso")
    void limpiarIntentosRestauraAcceso() {
        String correo = "recuperado@empresa.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.registrarFallo(correo);
        }
        assertTrue(loginAttemptService.estaBloqueado(correo));

        loginAttemptService.limpiarIntentos(correo);
        assertFalse(loginAttemptService.estaBloqueado(correo));
    }
}
