package com.constructora_backend.controller;

import com.constructora_backend.dto.auth.AuthResponseDTO;
import com.constructora_backend.dto.auth.LoginRequestDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.LoginAttemptService;
import com.constructora_backend.security.TokenBlacklistService;
import com.constructora_backend.security.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para inicio de sesión, mitigación anti-fuerza bruta y revocación de sesiones JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          @Autowired(required = false) LoginAttemptService loginAttemptService,
                          @Autowired(required = false) TokenBlacklistService tokenBlacklistService,
                          UserDetailsServiceImpl userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.loginAttemptService = loginAttemptService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión de usuario",
            description = "Autentica a un usuario mediante su correo y contraseña. Bloquea temporalmente tras 5 fallos consecutivos (anti-fuerza bruta)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticación exitosa. Retorna el token JWT generado.",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales incorrectas (correo o contraseña no coinciden).",
                    content = @Content(examples = @ExampleObject(value = "{\"error\": \"Credenciales incorrectas\"}"))
            ),
            @ApiResponse(
                    responseCode = "423",
                    description = "Cuenta bloqueada temporalmente por exceso de intentos fallidos (Anti-Brute-Force).",
                    content = @Content(examples = @ExampleObject(value = "{\"error\": \"Cuenta bloqueada temporalmente\"}"))
            )
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginDTO) {
        String correo = loginDTO.getCorreo();

        // 1. Verificación de bloqueo por intentos fallidos
        if (loginAttemptService != null && loginAttemptService.estaBloqueado(correo)) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of(
                            "error", "Cuenta bloqueada temporalmente",
                            "mensaje", "Has excedido el número máximo de intentos fallidos. Tu cuenta está bloqueada temporalmente durante 15 minutos por seguridad."
                    ));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(correo, loginDTO.getPassword())
            );

            // Limpiar intentos al autenticar con éxito
            if (loginAttemptService != null) {
                loginAttemptService.limpiarIntentos(correo);
            }

            String token = jwtUtils.generarToken(authentication);
            return ResponseEntity.ok(new AuthResponseDTO(token));
        } catch (BadCredentialsException e) {
            if (loginAttemptService != null) {
                loginAttemptService.registrarFallo(correo);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas", "detalles", "El correo o la contraseña no coinciden"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error de autenticación", "mensaje", "Ocurrió un error interno durante la autenticación. Intente más tarde."));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión de usuario", description = "Invalida el token JWT en la lista negra (Blacklist) para revocar la sesión activa.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión cerrada y token revocado con éxito"),
            @ApiResponse(responseCode = "400", description = "Header de autorización no proporcionado")
    })
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ") && tokenBlacklistService != null) {
            String token = authHeader.substring(7);
            tokenBlacklistService.revocarToken(token, vencimientoParaRevocar(token));
        }
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada exitosamente"));
    }

    @PostMapping("/renovar")
    @Operation(summary = "Renovar la sesión",
            description = "Cambia un token vigente por uno nuevo (con los roles actuales del usuario) y revoca el anterior. "
                    + "Así la sesión puede ser corta sin obligar a iniciar sesión de nuevo mientras la persona está trabajando.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token nuevo",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, vencido, revocado o de un usuario desactivado")
    })
    public ResponseEntity<?> renovar(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return noAutorizado();
        }
        String token = authHeader.substring(7);

        if ((tokenBlacklistService != null && tokenBlacklistService.estaRevocado(token)) || !jwtUtils.validarToken(token)) {
            return noAutorizado();
        }

        try {
            // Se vuelve a leer al usuario: si lo desactivaron o le cambiaron el rol, lo nota aquí
            UserDetails usuario = userDetailsService.loadUserByUsername(jwtUtils.obtenerCorreoDelToken(token));
            if (!usuario.isEnabled()) {
                return noAutorizado();
            }
            String nuevo = jwtUtils.generarTokenRenovado(usuario);
            if (tokenBlacklistService != null) {
                tokenBlacklistService.revocarToken(token, vencimientoParaRevocar(token));
            }
            return ResponseEntity.ok(new AuthResponseDTO(nuevo));
        } catch (Exception e) {
            return noAutorizado();
        }
    }

    private ResponseEntity<?> noAutorizado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "No autorizado", "mensaje", "Token JWT ausente, vencido o revocado"));
    }

    /** La revocación dura lo que le falta al token por vencer; si no se puede leer, el máximo de una sesión larga. */
    private long vencimientoParaRevocar(String token) {
        try {
            return jwtUtils.obtenerExpiracionMillis(token);
        } catch (Exception e) {
            return System.currentTimeMillis() + 86400000L;
        }
    }
}
