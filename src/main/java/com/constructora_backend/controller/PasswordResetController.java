package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ForgotPasswordRequest;
import com.constructora_backend.dto.request.ResetPasswordRequest;
import com.constructora_backend.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recuperación de Contraseña", description = "Endpoints para la gestión del restablecimiento de contraseñas")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar token de recuperación", description = "Genera un token de recuperación y lo envía al correo del usuario.")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.solicitarRecuperacion(request);
            return ResponseEntity.ok(Map.of("mensaje", "Se ha enviado un token de recuperación a tu correo"));
        } catch (MailException e) {
            log.error("Fallo al enviar correo de recuperación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al enviar el correo", "mensaje", "No se pudo entregar el correo de recuperación. Verifique las credenciales SMTP en el servidor."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error en la solicitud", "mensaje", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer contraseña con token", description = "Valida el token recibido y establece la nueva contraseña para la cuenta.")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.restablecerPassword(request);
            return ResponseEntity.ok(Map.of("mensaje", "La contraseña se ha actualizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error al restablecer contraseña", "mensaje", e.getMessage()));
        }
    }
}
