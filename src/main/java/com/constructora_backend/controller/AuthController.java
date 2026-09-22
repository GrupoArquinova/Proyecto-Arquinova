package com.constructora_backend.controller;

import com.constructora_backend.dto.auth.AuthResponseDTO;
import com.constructora_backend.dto.auth.LoginRequestDTO;
import com.constructora_backend.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para inicio de sesión y gestión de tokens JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    @Operation(
        summary = "Iniciar sesión de usuario",
        description = "Autentica a un usuario mediante su correo electrónico y contraseña. Retorna un token JWT de tipo Bearer si las credenciales son válidas."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Autenticación exitosa. Retorna el token JWT generado.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales incorrectas (correo o contraseña no coinciden).",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n  \"error\": \"Credenciales incorrectas\",\n  \"detalles\": \"El correo o la contraseña no coinciden\"\n}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor durante el proceso de autenticación.",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n  \"error\": \"Error de autenticación\",\n  \"mensaje\": \"Detalle del error interno\"\n}"
                )
            )
        )
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginDTO) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getCorreo(), loginDTO.getPassword())
            );

            String token = jwtUtils.generarToken(authentication);
            return ResponseEntity.ok(new AuthResponseDTO(token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas", "detalles", "El correo o la contraseña no coinciden"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error de autenticación", "mensaje", "Ocurrió un error interno durante la autenticación. Intente más tarde."));
        }
    }
}
