package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ForgotPasswordRequest;
import com.constructora_backend.dto.request.ResetPasswordRequest;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("POST /api/auth/forgot-password - Debe devolver HTTP 200 cuando la solicitud es exitosa")
    void forgotPassword_Exitoso() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setCorreo("test@constructora.com");

        doNothing().when(passwordResetService).solicitarRecuperacion(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Se ha enviado un token de recuperación a tu correo"));

        verify(passwordResetService, times(1)).solicitarRecuperacion(any(ForgotPasswordRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - Debe devolver HTTP 500 cuando el servidor SMTP falla")
    void forgotPassword_ErrorSMTP() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setCorreo("test@constructora.com");

        doThrow(new MailSendException("Fallo conexion SMTP"))
                .when(passwordResetService).solicitarRecuperacion(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error al enviar el correo"))
                .andExpect(jsonPath("$.mensaje").value("No se pudo entregar el correo de recuperación. Verifique las credenciales SMTP en el servidor."));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Debe devolver HTTP 200 cuando el token y nueva contraseña son validos")
    void resetPassword_Exitoso() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token-uuid");
        request.setNuevaPassword("NuevaPassword123!");

        doNothing().when(passwordResetService).restablecerPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("La contraseña se ha actualizado correctamente"));

        verify(passwordResetService, times(1)).restablecerPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Debe devolver HTTP 400 cuando el token es invalido o expirado")
    void resetPassword_TokenInvalido() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token-uuid");
        request.setNuevaPassword("NuevaPassword123!");

        doThrow(new RuntimeException("El token ha expirado"))
                .when(passwordResetService).restablecerPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error al restablecer contraseña"))
                .andExpect(jsonPath("$.mensaje").value("El token ha expirado"));
    }
}
