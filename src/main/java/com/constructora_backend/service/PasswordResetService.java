package com.constructora_backend.service;

import com.constructora_backend.dto.request.ForgotPasswordRequest;
import com.constructora_backend.dto.request.ResetPasswordRequest;
import com.constructora_backend.entity.TokenRecuperacion;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.repository.TokenRecuperacionRepository;
import com.constructora_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    /** Mensaje único para cualquier token que no sirva (no existe, ya usado, vencido o de una cuenta desactivada). */
    public static final String MENSAJE_TOKEN_INVALIDO = "El enlace de recuperación no es válido o ya venció. Solicita uno nuevo.";

    /** Máximo de solicitudes de recuperación por correo en una hora (exista o no la cuenta, para no revelarlo). */
    static final int MAX_SOLICITUDES_POR_HORA = 3;

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final LimitadorVentana limitador;

    /**
     * Genera el enlace de recuperación y lo envía por correo. Si el correo no está registrado (o la cuenta está inactiva)
     * no hace nada y no lo dice: quien consulta recibe la misma respuesta, para que no se pueda averiguar qué correos existen.
     * El token viaja solo en el correo; en la base se guarda su hash. Cada correo admite 3 solicitudes por hora, y al emitir
     * un token nuevo los anteriores que seguían pendientes dejan de valer.
     */
    @Transactional
    public void solicitarRecuperacion(ForgotPasswordRequest request) {
        String correo = request.getCorreo() == null ? "" : request.getCorreo().trim().toLowerCase(Locale.ROOT);
        if (!limitador.intentar("recuperacion:" + correo, MAX_SOLICITUDES_POR_HORA, Duration.ofHours(1))) {
            log.info("Recuperación de contraseña omitida: se superó el límite por hora para ese correo");
            return;
        }

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo()).orElse(null);
        if (usuario == null || Boolean.FALSE.equals(usuario.getActivo())) {
            log.info("Recuperación de contraseña solicitada para un correo sin cuenta activa; no se envía nada");
            return;
        }

        tokenRepository.invalidarPendientes(usuario.getId(), LocalDateTime.now());

        String token = UUID.randomUUID().toString();
        TokenRecuperacion tokenEntity = TokenRecuperacion.builder()
                .usuario(usuario)
                .tokenHash(hashear(token))
                .expiraEn(LocalDateTime.now().plusMinutes(15))
                .build();
        tokenRepository.save(tokenEntity);

        emailService.enviarCorreoRecuperacion(usuario.getCorreo(), token);
    }

    /**
     * Cambia la contraseña con un token válido. Cualquier problema con el token da el mismo mensaje. Al terminar, todos los
     * tokens pendientes del usuario quedan usados y se revocan las sesiones abiertas (los JWT anteriores dejan de valer).
     */
    @Transactional
    public void restablecerPassword(ResetPasswordRequest request) {
        TokenRecuperacion tokenEntity = tokenRepository.findByTokenHash(hashear(request.getToken()))
                .orElseThrow(() -> new BadRequestException(MENSAJE_TOKEN_INVALIDO));

        if (tokenEntity.getUsadoEn() != null || tokenEntity.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(MENSAJE_TOKEN_INVALIDO);
        }

        Usuario usuario = tokenEntity.getUsuario();
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadRequestException(MENSAJE_TOKEN_INVALIDO);
        }

        LocalDateTime ahora = LocalDateTime.now();
        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuario.setTokenValidoDesde(ahora);
        usuarioRepository.save(usuario);

        tokenRepository.invalidarPendientes(usuario.getId(), ahora);
        tokenEntity.setUsadoEn(ahora);
        tokenRepository.save(tokenEntity);
    }

    /** SHA-256 en hexadecimal (64 caracteres, el largo de la columna token_hash). */
    static String hashear(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
