package com.constructora_backend.service;

import com.constructora_backend.dto.request.ForgotPasswordRequest;
import com.constructora_backend.dto.request.ResetPasswordRequest;
import com.constructora_backend.entity.TokenRecuperacion;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.exception.ResourceNotFoundException;
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
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Genera el enlace de recuperación y lo envía por correo. Si el correo no está registrado (o la cuenta está inactiva)
     * no hace nada y no lo dice: quien consulta recibe la misma respuesta, para que no se pueda averiguar qué correos existen.
     * El token viaja solo en el correo; en la base se guarda su hash.
     */
    @Transactional
    public void solicitarRecuperacion(ForgotPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo()).orElse(null);
        if (usuario == null || Boolean.FALSE.equals(usuario.getActivo())) {
            log.info("Recuperación de contraseña solicitada para un correo sin cuenta activa; no se envía nada");
            return;
        }

        String token = UUID.randomUUID().toString();

        TokenRecuperacion tokenEntity = TokenRecuperacion.builder()
                .usuario(usuario)
                .tokenHash(hashear(token))
                .expiraEn(LocalDateTime.now().plusMinutes(15))
                .build();

        tokenRepository.save(tokenEntity);

        emailService.enviarCorreoRecuperacion(usuario.getCorreo(), token);
    }

    @Transactional
    public void restablecerPassword(ResetPasswordRequest request) {
        TokenRecuperacion tokenEntity = tokenRepository.findByTokenHash(hashear(request.getToken()))
                .orElseThrow(() -> new ResourceNotFoundException("El token de recuperación proporcionado es inválido o no existe"));

        if (tokenEntity.getUsadoEn() != null) {
            throw new BadRequestException("Este token ya ha sido utilizado previamente");
        }

        if (tokenEntity.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("El token de recuperación ha expirado");
        }

        Usuario usuario = tokenEntity.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);

        tokenEntity.setUsadoEn(LocalDateTime.now());
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
