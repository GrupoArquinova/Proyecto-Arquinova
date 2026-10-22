package com.constructora_backend.service;

import com.constructora_backend.dto.request.UsuarioRequestDTO;
import com.constructora_backend.dto.request.UsuarioUpdateDTO;
import com.constructora_backend.dto.response.UsuarioResponseDTO;
import com.constructora_backend.entity.Rol;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.exception.ConflictException;
import com.constructora_backend.exception.DuplicateResourceException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.UsuarioMapper;
import com.constructora_backend.repository.RolRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          UsuarioMapper usuarioMapper,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<UsuarioResponseDTO> obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(usuarioMapper::toDTO);
    }

    public UsuarioResponseDTO guardar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new DuplicateResourceException("El correo ya se encuentra registrado: " + dto.getCorreo());
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getRolId()));

        Usuario usuario = usuarioMapper.toEntity(dto, rol);
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return usuarioMapper.toDTO(usuarioGuardado);
    }

    /**
     * Actualiza un usuario.
     * <ul>
     *   <li>Un administrador puede cambiar nombre, correo, contraseña, rol y estado de cualquiera.</li>
     *   <li>Cualquier otra persona solo puede tocar SU propio perfil, y solo el nombre y la contraseña: si en la petición
     *       vienen un rol, un estado o un correo distintos de los actuales se rechaza con 403, y para cambiar la
     *       contraseña debe enviar la actual (passwordActual).</li>
     *   <li>Nadie puede desactivar su propia cuenta, ni quitar o desactivar al último administrador activo.</li>
     * </ul>
     * Cambiar la contraseña, el rol o el estado revoca las sesiones abiertas de ese usuario (token_valido_desde).
     *
     * @param llamadorEsAdmin si quien hace la petición tiene el rol ADMINISTRADOR
     * @param correoLlamador  correo de quien hace la petición (para saber si edita su propio perfil)
     */
    public UsuarioResponseDTO actualizar(Long id, UsuarioUpdateDTO dto, boolean llamadorEsAdmin, String correoLlamador) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        boolean esPropio = correoLlamador != null && correoLlamador.equalsIgnoreCase(usuario.getCorreo());
        boolean cambiaPassword = dto.getPassword() != null && !dto.getPassword().isBlank();
        boolean revocarSesiones = cambiaPassword;

        if (!llamadorEsAdmin) {
            // Solo el propio perfil, y sin tocar lo que da poder o identidad
            if (!esPropio) {
                throw new AccessDeniedException("Solo puedes modificar tu propio perfil");
            }
            if (dto.getRolId() != null && !dto.getRolId().equals(usuario.getRol().getId())) {
                throw new AccessDeniedException("No puedes cambiar tu rol");
            }
            if (dto.getActivo() != null && !dto.getActivo().equals(usuario.getActivo())) {
                throw new AccessDeniedException("No puedes cambiar el estado de tu cuenta");
            }
            if (dto.getCorreo() != null && !dto.getCorreo().isBlank() && !dto.getCorreo().equalsIgnoreCase(usuario.getCorreo())) {
                throw new AccessDeniedException("No puedes cambiar tu correo");
            }
            if (cambiaPassword) {
                if (dto.getPasswordActual() == null || dto.getPasswordActual().isBlank()
                        || !passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordHash())) {
                    throw new BadRequestException("La contraseña actual no es correcta");
                }
            }
            usuarioMapper.actualizarNombre(dto, usuario);
        } else {
            if (dto.getCorreo() != null && !dto.getCorreo().equalsIgnoreCase(usuario.getCorreo())
                    && usuarioRepository.existsByCorreo(dto.getCorreo())) {
                throw new DuplicateResourceException("El correo ya se encuentra en uso por otro usuario: " + dto.getCorreo());
            }

            Rol rol = null;
            if (dto.getRolId() != null) {
                rol = rolRepository.findById(dto.getRolId())
                        .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getRolId()));
            }

            boolean eraAdminActivo = esAdminActivo(usuario);
            boolean quedaraAdminActivo = (rol != null ? esRolAdmin(rol) : esRolAdmin(usuario.getRol()))
                    && (dto.getActivo() != null ? dto.getActivo() : Boolean.TRUE.equals(usuario.getActivo()));

            if (esPropio && Boolean.FALSE.equals(dto.getActivo())) {
                throw new ConflictException("No puedes desactivar tu propia cuenta");
            }
            if (eraAdminActivo && !quedaraAdminActivo && usuarioRepository.contarAdministradoresActivos() <= 1) {
                throw new ConflictException("Debe quedar al menos un administrador activo");
            }

            boolean cambiaRol = rol != null && !rol.getId().equals(usuario.getRol().getId());
            boolean cambiaEstado = dto.getActivo() != null && !dto.getActivo().equals(usuario.getActivo());
            revocarSesiones = cambiaPassword || cambiaRol || cambiaEstado;

            usuarioMapper.updateEntityFromDTO(dto, usuario, rol);
        }

        if (cambiaPassword) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        if (revocarSesiones) {
            // Los tokens emitidos antes de este instante dejan de valer
            usuario.setTokenValidoDesde(LocalDateTime.now());
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return usuarioMapper.toDTO(usuarioActualizado);
    }

    /**
     * Elimina un usuario. No se puede eliminar la propia cuenta ni al último administrador activo.
     *
     * @param correoLlamador correo de quien hace la petición
     */
    public void eliminar(Long id, String correoLlamador) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        if (correoLlamador != null && correoLlamador.equalsIgnoreCase(usuario.getCorreo())) {
            throw new ConflictException("No puedes eliminar tu propia cuenta");
        }
        if (esAdminActivo(usuario) && usuarioRepository.contarAdministradoresActivos() <= 1) {
            throw new ConflictException("Debe quedar al menos un administrador activo");
        }
        usuarioRepository.deleteById(id);
    }

    private static boolean esAdminActivo(Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getActivo()) && esRolAdmin(usuario.getRol());
    }

    private static boolean esRolAdmin(Rol rol) {
        return rol != null && rol.getNombre() != null
                && ("ADMINISTRADOR".equals(rol.getNombre()) || "ROLE_ADMINISTRADOR".equals(rol.getNombre()));
    }
}
