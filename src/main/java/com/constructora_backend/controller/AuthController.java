package com.constructora_backend.controller;

import com.constructora_backend.dto.auth.AuthResponseDTO;
import com.constructora_backend.dto.auth.LoginRequestDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.LoginAttemptService;
import com.constructora_backend.security.TokenBlacklistService;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.security.UsuarioPrincipal;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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
            description = "Autentica a un usuario mediante su correo y contraseña. Bloquea temporalmente el par correo+IP tras 5 fallos consecutivos (anti-fuerza bruta)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticación exitosa. Retorna el token JWT generado.",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales incorrectas o cuenta desactivada (la respuesta es la misma en ambos casos).",
                    content = @Content(examples = @ExampleObject(value = "{\"error\": \"Credenciales incorrectas\"}"))
            ),
            @ApiResponse(
                    responseCode = "423",
                    description = "Demasiados intentos fallidos: bloqueo temporal (Anti-Brute-Force).",
                    content = @Content(examples = @ExampleObject(value = "{\"error\": \"Cuenta bloqueada temporalmente\"}"))
            ),
            @ApiResponse(responseCode = "429", description = "Demasiadas peticiones desde la misma IP")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginDTO, HttpServletRequest request) {
        String correo = loginDTO.getCorreo();
        String ip = request.getRemoteAddr();

        // 1. Verificación de bloqueo por intentos fallidos (por correo + IP, y por correo en general con un umbral mucho más alto)
        if (loginAttemptService != null && loginAttemptService.estaBloqueado(correo, ip)) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of(
                            "error", "Cuenta bloqueada temporalmente",
                            "mensaje", "Has excedido el número máximo de intentos fallidos. El acceso está bloqueado temporalmente durante 15 minutos por seguridad."
                    ));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(correo, loginDTO.getPassword())
            );

            // Limpiar intentos al autenticar con éxito
            if (loginAttemptService != null) {
                loginAttemptService.limpiarIntentos(correo, ip);
            }

            String token = jwtUtils.generarToken(authentication);
            return ResponseEntity.ok(new AuthResponseDTO(token));

        } catch (AuthenticationException e) {
            // Contraseña incorrecta, cuenta desactivada, usuario inexistente...: la misma respuesta y todo cuenta como intento fallido,
            // para que nadie pueda averiguar qué correos existen o están desactivados
            if (loginAttemptService != null) {
                loginAttemptService.registrarFallo(correo, ip);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas", "detalles", "El correo o la contraseña no coinciden"));
        } catch (Exception e) {
            log.error("Error interno durante la autenticación: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error de autenticación", "mensaje", "Ocurrió un error interno durante la autenticación. Intente más tarde."));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión de usuario", description = "Invalida el token JWT en la lista negra (Blacklist) para revocar la sesión activa. Solo se revoca un token válido.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión cerrada y token revocado con éxito"),
            @ApiResponse(responseCode = "400", description = "Header de autorización no proporcionado")
    })
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ") && tokenBlacklistService != null) {
            String token = authHeader.substring(7);
            // Solo se revoca un token que de verdad es nuestro y vigente: así nadie llena la lista con basura
            if (jwtUtils.validarToken(token)) {
                tokenBlacklistService.revocarToken(token, vencimientoParaRevocar(token));
            }
        }

        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada exitosamente"));
    }

    @PostMapping("/renovar")
    @Operation(summary = "Renovar la sesión",
            description = "Cambia un token vigente por uno nuevo (con los roles actuales del usuario) y revoca el anterior. "
                    + "Así la sesión puede ser corta sin obligar a iniciar sesión de nuevo mientras la persona está trabajando. "
                    + "La sesión tiene una vida máxima absoluta (12 horas por defecto): pasado ese tiempo hay que iniciar sesión de nuevo.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token nuevo",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, vencido, revocado, de un usuario desactivado o de una sesión que superó su vida máxima")
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
            // La sesión no se puede estirar renovando sin fin: tiene un límite absoluto desde el inicio de sesión
            long inicioSesion = jwtUtils.obtenerSesionInicioMillis(token);
            if (jwtUtils.sesionAgotada(inicioSesion)) {
                return noAutorizado();
            }

            // Se vuelve a leer al usuario: si lo desactivaron o le cambiaron el rol o la contraseña, lo nota aquí
            UserDetails usuario = userDetailsService.loadUserByUsername(jwtUtils.obtenerCorreoDelToken(token));
            if (!usuario.isEnabled() || !UsuarioPrincipal.tokenAceptado(usuario, jwtUtils.obtenerEmisionMillis(token))) {
                return noAutorizado();
            }

            String nuevo = jwtUtils.generarTokenRenovado(usuario, inicioSesion);
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
