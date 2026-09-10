package com.constructora_backend.service;

import com.constructora_backend.dto.request.ForgotPasswordRequest;
import com.constructora_backend.dto.request.ResetPasswordRequest;
import com.constructora_backend.entity.TokenRecuperacion;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.repository.TokenRecuperacionRepository;
import com.constructora_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void solicitarRecuperacion(ForgotPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new RuntimeException("No se encontro ningun usuario con ese correo"));

        String token = UUID.randomUUID().toString();

        TokenRecuperacion tokenEntity = TokenRecuperacion.builder()
                .usuario(usuario)
                .tokenHash(token)
                .expiraEn(LocalDateTime.now().plusMinutes(15))
                .build();

        tokenRepository.save(tokenEntity);

        emailService.enviarCorreoRecuperacion(usuario.getCorreo(), token);
    }

    @Transactional
    public void restablecerPassword(ResetPasswordRequest request) {
        TokenRecuperacion tokenEntity = tokenRepository.findByTokenHash(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token invalido o inxistente"));

        if (tokenEntity.getUsadoEn() != null) {
            throw new RuntimeException("Este token ya ha sido utilizado");
        }

        if (tokenEntity.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El token ha expirado");
        }

        Usuario usuario = tokenEntity.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);

        tokenEntity.setUsadoEn(LocalDateTime.now());
        tokenRepository.save(tokenEntity);
    }
}
