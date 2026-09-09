package com.constructora_backend.security;

import com.constructora_backend.entity.Usuario;
import com.constructora_backend.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        String nombreRol = usuario.getRol().getNombre();
        if (!nombreRol.startsWith("ROLE_")) {
            nombreRol = "ROLE_" + nombreRol;
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(nombreRol);

        return new User(
                usuario.getCorreo(),
                usuario.getPasswordHash(),
                usuario.getActivo(),
                true, true, true,
                Collections.singletonList(authority)
        );
    }
}