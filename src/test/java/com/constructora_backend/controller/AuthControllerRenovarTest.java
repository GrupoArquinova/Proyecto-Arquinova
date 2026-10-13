package com.constructora_backend.controller;

import com.constructora_backend.dto.auth.AuthResponseDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.TokenBlacklistService;
import com.constructora_backend.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerRenovarTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private UserDetailsServiceImpl userDetailsService;

    private AuthController controller;
    private UserDetails admin;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authenticationManager, jwtUtils, null, tokenBlacklistService, userDetailsService);
        admin = new User("admin@arquinova.com", "x", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
    }

    private MockHttpServletRequest conToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    @DisplayName("renovar - con un token vigente entrega uno nuevo y revoca el anterior")
    void renovarExitoso() {
        when(tokenBlacklistService.estaRevocado("viejo")).thenReturn(false);
        when(jwtUtils.validarToken("viejo")).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken("viejo")).thenReturn("admin@arquinova.com");
        when(userDetailsService.loadUserByUsername("admin@arquinova.com")).thenReturn(admin);
        when(jwtUtils.generarTokenRenovado(admin)).thenReturn("nuevo");
        when(jwtUtils.obtenerExpiracionMillis("viejo")).thenReturn(System.currentTimeMillis() + 600_000);

        ResponseEntity<?> respuesta = controller.renovar(conToken("viejo"));

        assertEquals(200, respuesta.getStatusCode().value());
        AuthResponseDTO cuerpo = assertInstanceOf(AuthResponseDTO.class, respuesta.getBody());
        assertEquals("nuevo", cuerpo.getToken());
        verify(tokenBlacklistService).revocarToken(org.mockito.ArgumentMatchers.eq("viejo"), anyLong());
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
        verify(jwtUtils, never()).generarTokenRenovado(org.mockito.ArgumentMatchers.any());
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
        when(tokenBlacklistService.estaRevocado("viejo")).thenReturn(false);
        when(jwtUtils.validarToken("viejo")).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken("viejo")).thenReturn("admin@arquinova.com");
        when(userDetailsService.loadUserByUsername("admin@arquinova.com")).thenReturn(desactivado);

        assertEquals(401, controller.renovar(conToken("viejo")).getStatusCode().value());
        verify(jwtUtils, never()).generarTokenRenovado(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("logout - revoca el token solo por lo que le falta de vida, no por 24 horas")
    void logoutUsaElVencimientoReal() {
        long vence = System.currentTimeMillis() + 120_000;
        when(jwtUtils.obtenerExpiracionMillis("activo")).thenReturn(vence);

        controller.logout(conToken("activo"));

        verify(tokenBlacklistService).revocarToken("activo", vence);
    }
}
