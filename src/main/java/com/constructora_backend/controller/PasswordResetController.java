package com.constructora_backend.controller;

import com.constructora_backend.dto.request.ForgotPasswordRequest;
import com.constructora_backend.dto.request.ResetPasswordRequest;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    static final String MENSAJE_FORGOT = "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña. Revisa tu bandeja de entrada y la carpeta de spam.";

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar enlace de recuperación", description = "Si el correo pertenece a una cuenta activa, le envía un enlace para restablecer la contraseña. La respuesta es siempre la misma (200) exista o no la cuenta, y aunque falle el envío del correo.")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.solicitarRecuperacion(request);
        } catch (Exception e) {
            // Un fallo (por ejemplo del correo) solo queda en el registro: contárselo a quien consulta revelaría si la cuenta existe
            log.error("Fallo al procesar la recuperación de contraseña: {}", e.getClass().getSimpleName());
        }
        return ResponseEntity.ok(Map.of("mensaje", MENSAJE_FORGOT));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer contraseña con token", description = "Valida el token recibido y establece la nueva contraseña para la cuenta. Cualquier problema con el token da el mismo mensaje.")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.restablecerPassword(request);
            return ResponseEntity.ok(Map.of("mensaje", "La contraseña se ha actualizado correctamente"));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error al restablecer contraseña", "mensaje", PasswordResetService.MENSAJE_TOKEN_INVALIDO));
        } catch (Exception e) {
            log.error("Fallo inesperado al restablecer la contraseña: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error al restablecer contraseña", "mensaje", PasswordResetService.MENSAJE_TOKEN_INVALIDO));
        }
    }
}
