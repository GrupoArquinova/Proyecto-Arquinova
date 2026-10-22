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


    @Test
    @DisplayName("generarToken - el token lleva los roles del usuario y su vencimiento")
    void tokenLlevaRolesYVencimiento() {
        UserDetails admin = new User("admin@constructora.com", "x",
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(admin);

        String token = jwtUtils.generarToken(authentication);

        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertTrue(payload.contains("\"roles\":[\"ROLE_ADMINISTRADOR\"]"), payload);
        long vence = jwtUtils.obtenerExpiracionMillis(token);
        assertTrue(vence > System.currentTimeMillis() + expirationMs - 60_000);
        assertTrue(vence <= System.currentTimeMillis() + expirationMs + 1_000);
    }

    @Test
    @DisplayName("generarTokenRenovado - emite un token válido para el mismo usuario")
    void tokenRenovado() {
        UserDetails admin = new User("admin@constructora.com", "x",
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));

        String token = jwtUtils.generarTokenRenovado(admin, System.currentTimeMillis());

        assertTrue(jwtUtils.validarToken(token));
        assertEquals("admin@constructora.com", jwtUtils.obtenerCorreoDelToken(token));
    }

    private UserDetails admin() {
        return new User("admin@constructora.com", "x",
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
    }

    @Test
    @DisplayName("El token lleva el instante de emisión (iat) y el inicio de la sesión")
    void tokenLlevaEmisionEInicioDeSesion() {
        long antes = System.currentTimeMillis() - 1_000;
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(admin());

        String token = jwtUtils.generarToken(authentication);

        assertTrue(jwtUtils.obtenerEmisionMillis(token) >= antes);
        assertTrue(jwtUtils.obtenerSesionInicioMillis(token) >= antes);
    }

    @Test
    @DisplayName("Al renovar se conserva el inicio de la sesión original aunque el token sea nuevo")
    void renovarConservaElInicioDeSesion() {
        long inicio = System.currentTimeMillis() - 5 * 3_600_000L;

        String renovado = jwtUtils.generarTokenRenovado(admin(), inicio);

        assertEquals(inicio, jwtUtils.obtenerSesionInicioMillis(renovado));
        assertTrue(jwtUtils.obtenerEmisionMillis(renovado) > inicio);
    }

    @Test
    @DisplayName("Una sesión supera su vida máxima (12 horas por defecto, configurable)")
    void sesionAgotadaSegunLaVidaMaxima() {
        long ahora = System.currentTimeMillis();

        assertFalse(jwtUtils.sesionAgotada(ahora - 11 * 3_600_000L));
        assertTrue(jwtUtils.sesionAgotada(ahora - 13 * 3_600_000L));

        ReflectionTestUtils.setField(jwtUtils, "sesionMaximaHoras", 1L);
        assertTrue(jwtUtils.sesionAgotada(ahora - 2 * 3_600_000L));
    }

    @Test
    @DisplayName("Un token anterior a la fecha token_valido_desde del usuario ya no se acepta; uno posterior sí")
    void tokenValidoDesdeRevocaLosAnteriores() {
        long emision = System.currentTimeMillis() - 60_000;
        UsuarioPrincipal conCambioReciente = new UsuarioPrincipal("a@x.com", "x", true,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_X")),
                java.time.LocalDateTime.now());
        UsuarioPrincipal conCambioViejo = new UsuarioPrincipal("a@x.com", "x", true,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_X")),
                java.time.LocalDateTime.now().minusHours(2));
        UsuarioPrincipal sinCambios = new UsuarioPrincipal("a@x.com", "x", true,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_X")), null);

        assertFalse(UsuarioPrincipal.tokenAceptado(conCambioReciente, emision));
        assertTrue(UsuarioPrincipal.tokenAceptado(conCambioViejo, emision));
        assertTrue(UsuarioPrincipal.tokenAceptado(sinCambios, emision));
        assertTrue(UsuarioPrincipal.tokenAceptado(admin(), emision), "un UserDetails sin ese dato no se restringe");
    }
}
