package com.constructora_backend.service;

import com.constructora_backend.dto.request.UsuarioRequestDTO;
import com.constructora_backend.dto.request.UsuarioUpdateDTO;
import com.constructora_backend.dto.response.UsuarioResponseDTO;
import com.constructora_backend.entity.Rol;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.DuplicateResourceException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.UsuarioMapper;
import com.constructora_backend.repository.RolRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Spy
    private UsuarioMapper usuarioMapper = new UsuarioMapper();

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Rol rolAdmin;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        rolAdmin = new Rol();
        rolAdmin.setId(1L);
        rolAdmin.setNombre("ADMINISTRADOR");
        rolAdmin.setActivo(true);

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombreCompleto("Juan Perez");
        usuario.setCorreo("juan@empresa.com");
        usuario.setPasswordHash("hashed_password");
        usuario.setRol(rolAdmin);
        usuario.setActivo(true);
    }

    @Test
    @DisplayName("listarTodos debe retornar lista de UsuarioResponseDTO")
    void listarTodos_exito() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        List<UsuarioResponseDTO> resultado = usuarioService.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Juan Perez", resultado.get(0).getNombreCompleto());
        assertEquals("juan@empresa.com", resultado.get(0).getCorreo());
        assertEquals(1L, resultado.get(0).getRolId());
    }

    @Test
    @DisplayName("obtenerPorId debe retornar Optional con UsuarioResponseDTO si existe")
    void obtenerPorId_exito() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        Optional<UsuarioResponseDTO> resultado = usuarioService.obtenerPorId(10L);

        assertTrue(resultado.isPresent());
        assertEquals("Juan Perez", resultado.get().getNombreCompleto());
    }

    @Test
    @DisplayName("guardar debe encriptar la contraseña y retornar UsuarioResponseDTO")
    void guardar_exito() {
        UsuarioRequestDTO requestDTO = new UsuarioRequestDTO();
        requestDTO.setNombreCompleto("Nuevo Usuario");
        requestDTO.setCorreo("nuevo@empresa.com");
        requestDTO.setPassword("plainPassword123");
        requestDTO.setRolId(1L);

        when(usuarioRepository.existsByCorreo("nuevo@empresa.com")).thenReturn(false);
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin));
        when(passwordEncoder.encode("plainPassword123")).thenReturn("encodedPassword123");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(20L);
            return u;
        });

        UsuarioResponseDTO response = usuarioService.guardar(requestDTO);

        assertNotNull(response);
        assertEquals(20L, response.getId());
        assertEquals("Nuevo Usuario", response.getNombreCompleto());
        verify(passwordEncoder).encode("plainPassword123");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("guardar debe lanzar DuplicateResourceException si el correo ya existe")
    void guardar_correoDuplicado() {
        UsuarioRequestDTO requestDTO = new UsuarioRequestDTO();
        requestDTO.setCorreo("juan@empresa.com");

        when(usuarioRepository.existsByCorreo("juan@empresa.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> usuarioService.guardar(requestDTO));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar debe modificar los campos especificados y encriptar contraseña si se provee")
    void actualizar_exito() {
        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setNombreCompleto("Juan Modificado");
        updateDTO.setPassword("newSecretPass");

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("newSecretPass")).thenReturn("encodedNewPass");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuarioService.actualizar(10L, updateDTO);

        assertNotNull(response);
        assertEquals("Juan Modificado", response.getNombreCompleto());
        verify(passwordEncoder).encode("newSecretPass");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("eliminar debe borrar el usuario si existe")
    void eliminar_exito() {
        when(usuarioRepository.existsById(10L)).thenReturn(true);
        doNothing().when(usuarioRepository).deleteById(10L);

        assertDoesNotThrow(() -> usuarioService.eliminar(10L));
        verify(usuarioRepository).deleteById(10L);
    }

    @Test
    @DisplayName("eliminar debe lanzar ResourceNotFoundException si el usuario no existe")
    void eliminar_noExiste() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.eliminar(99L));
        verify(usuarioRepository, never()).deleteById(any());
    }
}
