package com.constructora_backend.security;

import com.constructora_backend.entity.Rol;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSecurityHelperTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserSecurityHelper userSecurityHelper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("isOwnerOrAdmin: Debe permitir acceso si el usuario tiene rol ADMINISTRADOR")
    void isOwnerOrAdmin_AdminTieneAccesoTotal() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@constructora.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        boolean acceso = userSecurityHelper.isOwnerOrAdmin(99L);
        assertTrue(acceso, "El administrador debe tener acceso independientemente del ID");
    }

    @Test
    @DisplayName("isOwnerOrAdmin: Debe permitir acceso si el usuario es el dueño del ID solicitado")
    void isOwnerOrAdmin_UsuarioPropioTieneAcceso() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "cliente@correo.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Usuario u = new Usuario();
        u.setId(10L);
        u.setCorreo("cliente@correo.com");

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(u));

        boolean acceso = userSecurityHelper.isOwnerOrAdmin(10L);
        assertTrue(acceso, "El usuario debe poder acceder a su propio ID");
    }

    @Test
    @DisplayName("isOwnerOrAdmin: Debe denegar acceso si el usuario intenta acceder a un ID ajeno (Row Level Security)")
    void isOwnerOrAdmin_UsuarioAjenoDeniegaAcceso() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "atacante@correo.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Usuario victima = new Usuario();
        victima.setId(10L);
        victima.setCorreo("victima@correo.com");

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(victima));

        boolean acceso = userSecurityHelper.isOwnerOrAdmin(10L);
        assertFalse(acceso, "RLS debe denegar el acceso a un ID de usuario ajeno");
    }
}
