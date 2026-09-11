package com.constructora_backend.service;

import com.constructora_backend.dto.request.ForgotPasswordRequest;
import com.constructora_backend.dto.request.ResetPasswordRequest;
import com.constructora_backend.entity.TokenRecuperacion;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.repository.TokenRecuperacionRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenRecuperacionRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private Usuario usuarioPrueba;

    @BeforeEach
    void setUp() {
        usuarioPrueba = new Usuario();
        usuarioPrueba.setId(1L);
        usuarioPrueba.setCorreo("test@constructora.com");
        usuarioPrueba.setPasswordHash("encoded_old_password");
        usuarioPrueba.setActivo(true);
    }

    @Test
    @DisplayName("solicitarRecuperacion - Debe guardar token en BD y enviar correo cuando el usuario existe")
    void solicitarRecuperacion_Exitoso() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setCorreo("test@constructora.com");

        when(usuarioRepository.findByCorreo("test@constructora.com")).thenReturn(Optional.of(usuarioPrueba));

        passwordResetService.solicitarRecuperacion(request);

        ArgumentCaptor<TokenRecuperacion> tokenCaptor = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository, times(1)).save(tokenCaptor.capture());
        TokenRecuperacion tokenGuardado = tokenCaptor.getValue();

        assertNotNull(tokenGuardado);
        assertNotNull(tokenGuardado.getTokenHash());
        assertEquals(usuarioPrueba, tokenGuardado.getUsuario());
        assertTrue(tokenGuardado.getExpiraEn().isAfter(LocalDateTime.now()));

        verify(emailService, times(1)).enviarCorreoRecuperacion(eq("test@constructora.com"), anyString());
    }

    @Test
    @DisplayName("solicitarRecuperacion - Debe lanzar ResourceNotFoundException cuando el usuario no existe")
    void solicitarRecuperacion_UsuarioNoEncontrado() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setCorreo("inexistente@constructora.com");

        when(usuarioRepository.findByCorreo("inexistente@constructora.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                passwordResetService.solicitarRecuperacion(request)
        );

        assertTrue(exception.getMessage().contains("No se encontró ningún usuario"));
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarCorreoRecuperacion(anyString(), anyString());
    }

    @Test
    @DisplayName("restablecerPassword - Debe actualizar la contraseña y marcar token como usado cuando el token es valido")
    void restablecerPassword_Exitoso() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-valido-123");
        request.setNuevaPassword("NuevaPassword123!");

        TokenRecuperacion tokenEntity = TokenRecuperacion.builder()
                .id(1L)
                .usuario(usuarioPrueba)
                .tokenHash("token-valido-123")
                .expiraEn(LocalDateTime.now().plusMinutes(10))
                .usadoEn(null)
                .build();

        when(tokenRepository.findByTokenHash("token-valido-123")).thenReturn(Optional.of(tokenEntity));
        when(passwordEncoder.encode("NuevaPassword123!")).thenReturn("encoded_new_password");

        passwordResetService.restablecerPassword(request);

        assertEquals("encoded_new_password", usuarioPrueba.getPasswordHash());
        assertNotNull(tokenEntity.getUsadoEn());
        verify(usuarioRepository, times(1)).save(usuarioPrueba);
        verify(tokenRepository, times(1)).save(tokenEntity);
    }

    @Test
    @DisplayName("restablecerPassword - Debe lanzar ResourceNotFoundException si el token no existe")
    void restablecerPassword_TokenInexistente() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-fantasma");
        request.setNuevaPassword("NuevaPassword123!");

        when(tokenRepository.findByTokenHash("token-fantasma")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                passwordResetService.restablecerPassword(request)
        );

        assertTrue(exception.getMessage().contains("inválido o no existe"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("restablecerPassword - Debe lanzar BadRequestException si el token ya fue usado")
    void restablecerPassword_TokenYaUsado() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-usado");
        request.setNuevaPassword("NuevaPassword123!");

        TokenRecuperacion tokenEntity = TokenRecuperacion.builder()
                .id(1L)
                .usuario(usuarioPrueba)
                .tokenHash("token-usado")
                .expiraEn(LocalDateTime.now().plusMinutes(10))
                .usadoEn(LocalDateTime.now().minusMinutes(5))
                .build();

        when(tokenRepository.findByTokenHash("token-usado")).thenReturn(Optional.of(tokenEntity));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                passwordResetService.restablecerPassword(request)
        );

        assertTrue(exception.getMessage().contains("ya ha sido utilizado"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("restablecerPassword - Debe lanzar BadRequestException si el token esta expirado")
    void restablecerPassword_TokenExpirado() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-expirado");
        request.setNuevaPassword("NuevaPassword123!");

        TokenRecuperacion tokenEntity = TokenRecuperacion.builder()
                .id(1L)
                .usuario(usuarioPrueba)
                .tokenHash("token-expirado")
                .expiraEn(LocalDateTime.now().minusMinutes(1))
                .usadoEn(null)
                .build();

        when(tokenRepository.findByTokenHash("token-expirado")).thenReturn(Optional.of(tokenEntity));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                passwordResetService.restablecerPassword(request)
        );

        assertTrue(exception.getMessage().contains("ha expirado"));
        verify(usuarioRepository, never()).save(any());
    }
}
