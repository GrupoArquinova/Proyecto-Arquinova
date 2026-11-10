package com.constructora_backend.service;

import com.constructora_backend.dto.request.ForgotPasswordRequest;
import com.constructora_backend.dto.request.ResetPasswordRequest;
import com.constructora_backend.entity.TokenRecuperacion;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.repository.TokenRecuperacionRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    /** Contador real en memoria: así se prueba de verdad el tope de solicitudes por correo. */
    @Spy
    private LimitadorVentana limitador = new LimitadorVentana();

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

    private ForgotPasswordRequest solicitud(String correo) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setCorreo(correo);
        return request;
    }

    private TokenRecuperacion tokenDe(String texto, LocalDateTime expira, LocalDateTime usadoEn) {
        return TokenRecuperacion.builder()
                .id(1L)
                .usuario(usuarioPrueba)
                .tokenHash(texto)
                .expiraEn(expira)
                .usadoEn(usadoEn)
                .build();
    }

    // ───────── solicitarRecuperacion ─────────

    @Test
    @DisplayName("solicitarRecuperacion - Debe guardar token en BD y enviar correo cuando el usuario existe")
    void solicitarRecuperacion_Exitoso() {
        when(usuarioRepository.findByCorreo("test@constructora.com")).thenReturn(Optional.of(usuarioPrueba));

        passwordResetService.solicitarRecuperacion(solicitud("test@constructora.com"));

        ArgumentCaptor<TokenRecuperacion> tokenCaptor = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository, times(1)).save(tokenCaptor.capture());
        TokenRecuperacion tokenGuardado = tokenCaptor.getValue();

        assertNotNull(tokenGuardado);
        assertNotNull(tokenGuardado.getTokenHash());
        assertEquals(usuarioPrueba, tokenGuardado.getUsuario());
        assertTrue(tokenGuardado.getExpiraEn().isAfter(LocalDateTime.now()));

        ArgumentCaptor<String> tokenEnviado = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).enviarCorreoRecuperacion(eq("test@constructora.com"), tokenEnviado.capture());

        // En la base solo queda el hash: el token real viaja únicamente en el correo
        assertNotEquals(tokenEnviado.getValue(), tokenGuardado.getTokenHash());
        assertEquals(PasswordResetService.hashear(tokenEnviado.getValue()), tokenGuardado.getTokenHash());
        assertEquals(64, tokenGuardado.getTokenHash().length());
    }

    @Test
    @DisplayName("solicitarRecuperacion - Al emitir un token nuevo se invalidan los que seguían pendientes")
    void solicitarRecuperacion_InvalidaLosPendientes() {
        when(usuarioRepository.findByCorreo("test@constructora.com")).thenReturn(Optional.of(usuarioPrueba));

        passwordResetService.solicitarRecuperacion(solicitud("test@constructora.com"));

        var orden = inOrder(tokenRepository);
        orden.verify(tokenRepository).invalidarPendientes(eq(1L), any(LocalDateTime.class));
        orden.verify(tokenRepository).save(any(TokenRecuperacion.class));
    }

    @Test
    @DisplayName("solicitarRecuperacion - Si el correo no existe no lanza error ni envía nada (no revela qué correos hay)")
    void solicitarRecuperacion_UsuarioNoEncontrado() {
        when(usuarioRepository.findByCorreo("inexistente@constructora.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.solicitarRecuperacion(solicitud("inexistente@constructora.com")));

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarCorreoRecuperacion(anyString(), anyString());
    }

    @Test
    @DisplayName("solicitarRecuperacion - Una cuenta inactiva se trata igual que una que no existe")
    void solicitarRecuperacion_CuentaInactiva() {
        usuarioPrueba.setActivo(false);
        when(usuarioRepository.findByCorreo("test@constructora.com")).thenReturn(Optional.of(usuarioPrueba));

        assertDoesNotThrow(() -> passwordResetService.solicitarRecuperacion(solicitud("test@constructora.com")));

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarCorreoRecuperacion(anyString(), anyString());
    }

    @Test
    @DisplayName("solicitarRecuperacion - Máximo 3 solicitudes por hora para el mismo correo (sin distinguir mayúsculas), y sin revelar nada")
    void solicitarRecuperacion_LimitePorCorreo() {
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.of(usuarioPrueba));

        for (int i = 0; i < 6; i++) {
            assertDoesNotThrow(() -> passwordResetService.solicitarRecuperacion(solicitud("Test@Constructora.com")));
        }

        verify(emailService, times(3)).enviarCorreoRecuperacion(anyString(), anyString());
        verify(tokenRepository, times(3)).save(any());
    }

    @Test
    @DisplayName("solicitarRecuperacion - El tope también aplica a correos que no existen (la respuesta no cambia)")
    void solicitarRecuperacion_LimiteAplicaTambienAInexistentes() {
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());

        for (int i = 0; i < 6; i++) {
            passwordResetService.solicitarRecuperacion(solicitud("nadie@constructora.com"));
        }

        // Solo las 3 primeras llegaron a consultar la base: el resto se descartó antes
        verify(usuarioRepository, times(3)).findByCorreo(anyString());
    }

    // ───────── restablecerPassword ─────────

    @Test
    @DisplayName("restablecerPassword - Actualiza la contraseña, marca el token y todos los pendientes como usados y revoca las sesiones")
    void restablecerPassword_Exitoso() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-valido-123");
        request.setNuevaPassword("NuevaPassword123!");
        TokenRecuperacion tokenEntity = tokenDe("token-valido-123", LocalDateTime.now().plusMinutes(10), null);

        when(tokenRepository.findByTokenHash(PasswordResetService.hashear("token-valido-123"))).thenReturn(Optional.of(tokenEntity));
        when(passwordEncoder.encode("NuevaPassword123!")).thenReturn("encoded_new_password");

        passwordResetService.restablecerPassword(request);

        assertEquals("encoded_new_password", usuarioPrueba.getPasswordHash());
        assertNotNull(tokenEntity.getUsadoEn());
        assertNotNull(usuarioPrueba.getTokenValidoDesde(), "los tokens de sesión anteriores dejan de valer");
        verify(usuarioRepository, times(1)).save(usuarioPrueba);
        verify(tokenRepository, times(1)).save(tokenEntity);
        verify(tokenRepository).invalidarPendientes(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("restablecerPassword - Un token que no existe da el mensaje genérico")
    void restablecerPassword_TokenInexistente() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-fantasma");
        request.setNuevaPassword("NuevaPassword123!");

        when(tokenRepository.findByTokenHash(PasswordResetService.hashear("token-fantasma"))).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                passwordResetService.restablecerPassword(request)
        );

        assertEquals(PasswordResetService.MENSAJE_TOKEN_INVALIDO, exception.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("restablecerPassword - Un token ya usado da el mismo mensaje genérico")
    void restablecerPassword_TokenYaUsado() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-usado");
        request.setNuevaPassword("NuevaPassword123!");
        TokenRecuperacion tokenEntity = tokenDe("token-usado", LocalDateTime.now().plusMinutes(10), LocalDateTime.now().minusMinutes(5));

        when(tokenRepository.findByTokenHash(PasswordResetService.hashear("token-usado"))).thenReturn(Optional.of(tokenEntity));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                passwordResetService.restablecerPassword(request)
        );

        assertEquals(PasswordResetService.MENSAJE_TOKEN_INVALIDO, exception.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("restablecerPassword - Un token vencido da el mismo mensaje genérico")
    void restablecerPassword_TokenExpirado() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-expirado");
        request.setNuevaPassword("NuevaPassword123!");
        TokenRecuperacion tokenEntity = tokenDe("token-expirado", LocalDateTime.now().minusMinutes(1), null);

        when(tokenRepository.findByTokenHash(PasswordResetService.hashear("token-expirado"))).thenReturn(Optional.of(tokenEntity));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                passwordResetService.restablecerPassword(request)
        );

        assertEquals(PasswordResetService.MENSAJE_TOKEN_INVALIDO, exception.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("restablecerPassword - Una cuenta desactivada después de pedir el enlace no puede cambiar la contraseña")
    void restablecerPassword_CuentaDesactivada() {
        usuarioPrueba.setActivo(false);
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-valido");
        request.setNuevaPassword("NuevaPassword123!");
        TokenRecuperacion tokenEntity = tokenDe("token-valido", LocalDateTime.now().plusMinutes(10), null);

        when(tokenRepository.findByTokenHash(PasswordResetService.hashear("token-valido"))).thenReturn(Optional.of(tokenEntity));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                passwordResetService.restablecerPassword(request)
        );

        assertEquals(PasswordResetService.MENSAJE_TOKEN_INVALIDO, exception.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
