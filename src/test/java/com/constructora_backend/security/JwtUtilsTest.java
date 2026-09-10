package com.constructora_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @InjectMocks
    private JwtUtils jwtUtils;

    private final String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final int expirationMs = 3600000; // 1 hora

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secretKey);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", expirationMs);
    }

    @Test
    @DisplayName("generarToken y obtenerCorreoDelToken - Debe generar token valido y extraer el correo")
    void generarYObtenerCorreo_Exitoso() {
        UserDetails userDetails = new User("admin@constructora.com", "password", Collections.emptyList());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtils.generarToken(authentication);

        assertNotNull(token);
        assertFalse(token.isBlank());

        String correoExtraido = jwtUtils.obtenerCorreoDelToken(token);
        assertEquals("admin@constructora.com", correoExtraido);
    }

    @Test
    @DisplayName("validarToken - Debe retornar true cuando el token es valido")
    void validarToken_Exitoso() {
        UserDetails userDetails = new User("usuario@constructora.com", "password", Collections.emptyList());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtils.generarToken(authentication);

        boolean esValido = jwtUtils.validarToken(token);
        assertTrue(esValido);
    }

    @Test
    @DisplayName("validarToken - Debe retornar false cuando el token esta expirado")
    void validarToken_TokenExpirado() {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1000); // Expirado hace 1 segundo

        UserDetails userDetails = new User("usuario@constructora.com", "password", Collections.emptyList());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtils.generarToken(authentication);

        boolean esValido = jwtUtils.validarToken(token);
        assertFalse(esValido);
    }

    @Test
    @DisplayName("validarToken - Debe retornar false cuando la cadena es invalida o corrupta")
    void validarToken_CadenaInvalida() {
        boolean esValido = jwtUtils.validarToken("token_completamente_invalido_xyz");
        assertFalse(esValido);
    }
}
