package com.constructora_backend.controller;

import com.constructora_backend.dto.auth.AuthResponseDTO;
import com.constructora_backend.dto.auth.LoginRequestDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.LoginAttemptService;
import com.constructora_backend.security.TokenBlacklistService;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerRenovarTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private LoginAttemptService loginAttemptService;

    private AuthController controller;
    private UserDetails admin;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authenticationManager, jwtUtils, loginAttemptService, tokenBlacklistService, userDetailsService);
        admin = new User("admin@arquinova.com", "x", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
    }

    private MockHttpServletRequest conToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private void tokenVigenteDe(String token, UserDetails usuario) {
        when(tokenBlacklistService.estaRevocado(token)).thenReturn(false);
        when(jwtUtils.validarToken(token)).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken(token)).thenReturn("admin@arquinova.com");
        when(userDetailsService.loadUserByUsername("admin@arquinova.com")).thenReturn(usuario);
    }

    // ───────── renovar ─────────

    @Test
    @DisplayName("renovar - con un token vigente entrega uno nuevo conservando el inicio de la sesión y revoca el anterior")
    void renovarExitoso() {
        tokenVigenteDe("viejo", admin);
        long inicio = System.currentTimeMillis() - 3_600_000;
        when(jwtUtils.obtenerSesionInicioMillis("viejo")).thenReturn(inicio);
        when(jwtUtils.generarTokenRenovado(admin, inicio)).thenReturn("nuevo");
        when(jwtUtils.obtenerExpiracionMillis("viejo")).thenReturn(System.currentTimeMillis() + 600_000);

        ResponseEntity<?> respuesta = controller.renovar(conToken("viejo"));

        assertEquals(200, respuesta.getStatusCode().value());
        AuthResponseDTO cuerpo = assertInstanceOf(AuthResponseDTO.class, respuesta.getBody());
        assertEquals("nuevo", cuerpo.getToken());
        verify(tokenBlacklistService).revocarToken(eq("viejo"), anyLong());
    }

    @Test
    @DisplayName("renovar - una sesión que superó su vida máxima absoluta no se renueva")
    void renovarSesionAgotada() {
        when(tokenBlacklistService.estaRevocado("largo")).thenReturn(false);
        when(jwtUtils.validarToken("largo")).thenReturn(true);
        long inicio = System.currentTimeMillis() - 13 * 3_600_000L;
        when(jwtUtils.obtenerSesionInicioMillis("largo")).thenReturn(inicio);
        when(jwtUtils.sesionAgotada(inicio)).thenReturn(true);

        assertEquals(401, controller.renovar(conToken("largo")).getStatusCode().value());
        verify(jwtUtils, never()).generarTokenRenovado(any(), anyLong());
        verify(tokenBlacklistService, never()).revocarToken(any(), anyLong());
    }

    @Test
    @DisplayName("renovar - un token anterior a un cambio de contraseña o de rol (token_valido_desde) no se renueva")
    void renovarTokenAnteriorAUnCambioDeCredenciales() {
        UserDetails conCambio = new UsuarioPrincipal("admin@arquinova.com", "x", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")), LocalDateTime.now());
        tokenVigenteDe("anterior", conCambio);
        when(jwtUtils.obtenerSesionInicioMillis("anterior")).thenReturn(System.currentTimeMillis() - 7_200_000);
        when(jwtUtils.obtenerEmisionMillis("anterior")).thenReturn(System.currentTimeMillis() - 7_200_000);

        assertEquals(401, controller.renovar(conToken("anterior")).getStatusCode().value());
        verify(jwtUtils, never()).generarTokenRenovado(any(), anyLong());
    }

    @Test
    @DisplayName("renovar - sin token responde 401")
    void renovarSinToken() {
        assertEquals(401, controller.renovar(conToken(null)).getStatusCode().value());
    }

    @Test
    @DisplayName("renovar - un token revocado (por ejemplo tras cerrar sesión) no se puede renovar")
    void renovarTokenRevocado() {
        when(tokenBlacklistService.estaRevocado("revocado")).thenReturn(true);

        assertEquals(401, controller.renovar(conToken("revocado")).getStatusCode().value());
        verify(jwtUtils, never()).generarTokenRenovado(any(), anyLong());
    }

    @Test
    @DisplayName("renovar - un token vencido o inválido responde 401")
    void renovarTokenInvalido() {
        when(tokenBlacklistService.estaRevocado("vencido")).thenReturn(false);
        when(jwtUtils.validarToken("vencido")).thenReturn(false);

        assertEquals(401, controller.renovar(conToken("vencido")).getStatusCode().value());
    }

    @Test
    @DisplayName("renovar - si la cuenta fue desactivada no se renueva")
    void renovarUsuarioDesactivado() {
        UserDetails desactivado = new User("admin@arquinova.com", "x", false, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
        tokenVigenteDe("viejo", desactivado);
        when(jwtUtils.obtenerSesionInicioMillis("viejo")).thenReturn(System.currentTimeMillis());

        assertEquals(401, controller.renovar(conToken("viejo")).getStatusCode().value());
        verify(jwtUtils, never()).generarTokenRenovado(any(), anyLong());
    }

    // ───────── logout ─────────

    @Test
    @DisplayName("logout - revoca el token solo por lo que le falta de vida, no por 24 horas")
    void logoutUsaElVencimientoReal() {
        long vence = System.currentTimeMillis() + 120_000;
        when(jwtUtils.validarToken("activo")).thenReturn(true);
        when(jwtUtils.obtenerExpiracionMillis("activo")).thenReturn(vence);

        controller.logout(conToken("activo"));

        verify(tokenBlacklistService).revocarToken("activo", vence);
    }

    @Test
    @DisplayName("logout - un token que no valida (basura o ajeno) no se guarda en la lista de revocados")
    void logoutConTokenInvalidoNoRevoca() {
        when(jwtUtils.validarToken("basura")).thenReturn(false);

        ResponseEntity<?> respuesta = controller.logout(conToken("basura"));

        assertEquals(200, respuesta.getStatusCode().value());
        verify(tokenBlacklistService, never()).revocarToken(any(), anyLong());
    }

    // ───────── login ─────────

    private LoginRequestDTO credenciales(String correo) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCorreo(correo);
        dto.setPassword("123456");
        return dto;
    }

    private MockHttpServletRequest desdeIp(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    @DisplayName("login - contraseña incorrecta y cuenta desactivada dan la misma respuesta 401 y ambas cuentan como intento fallido")
    void loginRespuestaUniforme() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("mala"))
                .thenThrow(new DisabledException("desactivada"));

        ResponseEntity<?> incorrecta = controller.login(credenciales("a@x.com"), desdeIp("203.0.113.7"));
        ResponseEntity<?> desactivada = controller.login(credenciales("b@x.com"), desdeIp("203.0.113.7"));

        assertEquals(401, incorrecta.getStatusCode().value());
        assertEquals(401, desactivada.getStatusCode().value());
        assertEquals(incorrecta.getBody(), desactivada.getBody(), "mismo cuerpo: no se distingue una cuenta desactivada");
        verify(loginAttemptService).registrarFallo("a@x.com", "203.0.113.7");
        verify(loginAttemptService).registrarFallo("b@x.com", "203.0.113.7");
    }

    @Test
    @DisplayName("login - un par correo+IP bloqueado responde 423 sin intentar autenticar")
    void loginBloqueado() {
        when(loginAttemptService.estaBloqueado("a@x.com", "203.0.113.7")).thenReturn(true);

        assertEquals(423, controller.login(credenciales("a@x.com"), desdeIp("203.0.113.7")).getStatusCode().value());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("login - el bloqueo y los fallos usan la IP de la conexión, no un encabezado que envíe el cliente")
    void loginUsaLaIpRealDeLaConexion() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mala"));
        MockHttpServletRequest request = desdeIp("203.0.113.7");
        request.addHeader("X-Forwarded-For", "6.6.6.6");

        controller.login(credenciales("a@x.com"), request);

        verify(loginAttemptService).estaBloqueado("a@x.com", "203.0.113.7");
        verify(loginAttemptService).registrarFallo("a@x.com", "203.0.113.7");
    }
}
