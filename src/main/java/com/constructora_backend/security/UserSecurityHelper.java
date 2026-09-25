package com.constructora_backend.security;

import com.constructora_backend.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Componente de seguridad para evaluación de Row Level Security (RLS) en SpEL.
 * Permite garantizar que cada usuario acceda únicamente a sus propios recursos
 * a menos que posea privilegios de ADMINISTRADOR.
 */
@Component("userSecurity")
public class UserSecurityHelper {

    private final UsuarioRepository usuarioRepository;

    public UserSecurityHelper(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Verifica si el usuario autenticado es ADMINISTRADOR o si es el propietario
     * del registro de usuario identificado por targetUserId.
     */
    public boolean isOwnerOrAdmin(Long targetUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return false;
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR") || a.getAuthority().equals("ADMINISTRADOR"));
        if (isAdmin) {
            return true;
        }

        if (targetUserId == null) {
            return false;
        }

        String currentEmail = auth.getName();
        return usuarioRepository.findById(targetUserId)
                .map(u -> u.getCorreo() != null && u.getCorreo().equalsIgnoreCase(currentEmail))
                .orElse(false);
    }

    /**
     * Obtiene el correo del usuario actualmente autenticado.
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName()
                : null;
    }
}
