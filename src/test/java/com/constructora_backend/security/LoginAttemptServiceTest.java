package com.constructora_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private static final String IP = "203.0.113.7";

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
            loginAttemptService.registrarFallo(correo, IP);
        }

        assertFalse(loginAttemptService.estaBloqueado(correo, IP));
    }

    @Test
    @DisplayName("Debe bloquear el par correo+IP exactamente al alcanzar 5 intentos fallidos")
    void cuentaBloqueadaAlQuintoIntento() {
        String correo = "victima@empresa.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.registrarFallo(correo, IP);
        }

        assertTrue(loginAttemptService.estaBloqueado(correo, IP));
    }

    @Test
    @DisplayName("Quien falla desde una IP no deja fuera a la persona real que entra desde otra IP")
    void elBloqueoPorParNoPermiteDenegarElServicioDesdeOtraIp() {
        String correo = "victima@empresa.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.registrarFallo(correo, "198.51.100.9"); // el atacante
        }

        assertTrue(loginAttemptService.estaBloqueado(correo, "198.51.100.9"));
        assertFalse(loginAttemptService.estaBloqueado(correo, IP), "la persona real, desde su IP, sigue pudiendo entrar");
    }

    @Test
    @DisplayName("Un ataque repartido entre muchas IP termina bloqueando el correo en general (umbral mucho más alto)")
    void elUmbralGlobalFrenaAtaquesDistribuidos() {
        String correo = "victima@empresa.com";

        for (int i = 0; i < LoginAttemptService.MAX_INTENTOS_GLOBAL - 1; i++) {
            loginAttemptService.registrarFallo(correo, "10.0.0." + i);
        }
        assertFalse(loginAttemptService.estaBloqueado(correo, IP));

        loginAttemptService.registrarFallo(correo, "10.0.1.1");
        assertTrue(loginAttemptService.estaBloqueado(correo, IP));
    }

    @Test
    @DisplayName("Debe restablecer el contador al invocar limpiarIntentos tras un login exitoso")
    void limpiarIntentosRestauraAcceso() {
        String correo = "recuperado@empresa.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.registrarFallo(correo, IP);
        }
        assertTrue(loginAttemptService.estaBloqueado(correo, IP));

        loginAttemptService.limpiarIntentos(correo, IP);
        assertFalse(loginAttemptService.estaBloqueado(correo, IP));
    }

    @Test
    @DisplayName("El correo se compara sin distinguir mayúsculas")
    void elCorreoNoDistingueMayusculas() {
        for (int i = 0; i < 5; i++) {
            loginAttemptService.registrarFallo("Ana@Empresa.com", IP);
        }

        assertTrue(loginAttemptService.estaBloqueado("ana@empresa.com", IP));
    }

    @Test
    @DisplayName("La memoria local tiene tope: una avalancha de correos distintos no la llena")
    void laMemoriaLocalTieneTope() {
        for (int i = 0; i < LoginAttemptService.MAX_CLAVES_LOCALES; i++) {
            loginAttemptService.registrarFallo("u" + i + "@x.com", IP);
        }
        int antes = loginAttemptService.tamanoLocal();

        loginAttemptService.registrarFallo("nuevo@x.com", IP);

        assertEquals(antes, loginAttemptService.tamanoLocal(), "con el tope alcanzado no se agregan claves nuevas");
    }
}
