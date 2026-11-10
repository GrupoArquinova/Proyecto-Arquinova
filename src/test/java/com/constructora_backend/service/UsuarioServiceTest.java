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


    // ───────── actualizar ─────────

    private Rol rolUsuario() {
        Rol r = new Rol();
        r.setId(2L);
        r.setNombre("USUARIO");
        r.setActivo(true);
        return r;
    }

    private void guardarDevuelveLoMismo() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("actualizar (admin) modifica los campos especificados, encripta la contraseña y revoca las sesiones")
    void actualizar_exito() {
        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setNombreCompleto("Juan Modificado");
        updateDTO.setPassword("newSecretPass");

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("newSecretPass")).thenReturn("encodedNewPass");
        guardarDevuelveLoMismo();

        UsuarioResponseDTO response = usuarioService.actualizar(10L, updateDTO, true, "otro.admin@empresa.com");

        assertNotNull(response);
        assertEquals("Juan Modificado", response.getNombreCompleto());
        verify(passwordEncoder).encode("newSecretPass");
        verify(usuarioRepository).save(usuario);
        assertNotNull(usuario.getTokenValidoDesde(), "cambiar la contraseña revoca los tokens anteriores");
    }

    @Test
    @DisplayName("actualizar (admin) cambiar solo el nombre no revoca las sesiones")
    void actualizar_soloNombreNoRevocaSesiones() {
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setNombreCompleto("Solo Nombre");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        guardarDevuelveLoMismo();

        usuarioService.actualizar(10L, dto, true, "otro.admin@empresa.com");

        assertNull(usuario.getTokenValidoDesde());
    }

    @Test
    @DisplayName("actualizar (admin) cambiar el rol o el estado de otra persona revoca sus sesiones")
    void actualizar_cambioDeRolOEstadoRevocaSesiones() {
        usuario.setRol(rolUsuario());
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setRolId(1L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin));
        guardarDevuelveLoMismo();

        usuarioService.actualizar(10L, dto, true, "otro.admin@empresa.com");

        assertEquals(rolAdmin, usuario.getRol());
        assertNotNull(usuario.getTokenValidoDesde());
    }

    @Test
    @DisplayName("actualizar (no admin) solo puede cambiar su nombre; correo, rol y estado distintos se rechazan con 403")
    void actualizar_noAdminNoPuedeCambiarRolEstadoNiCorreo() {
        usuario.setRol(rolUsuario());
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        UsuarioUpdateDTO conRol = new UsuarioUpdateDTO();
        conRol.setRolId(1L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> usuarioService.actualizar(10L, conRol, false, "juan@empresa.com"));

        UsuarioUpdateDTO conEstado = new UsuarioUpdateDTO();
        conEstado.setActivo(false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> usuarioService.actualizar(10L, conEstado, false, "juan@empresa.com"));

        UsuarioUpdateDTO conCorreo = new UsuarioUpdateDTO();
        conCorreo.setCorreo("otro@empresa.com");
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> usuarioService.actualizar(10L, conCorreo, false, "juan@empresa.com"));

        verify(usuarioRepository, never()).save(any());
        assertEquals("USUARIO", usuario.getRol().getNombre());
        assertTrue(usuario.getActivo());
        assertEquals("juan@empresa.com", usuario.getCorreo());
    }

    @Test
    @DisplayName("actualizar (no admin) puede enviar los mismos rol, estado y correo que ya tiene (el panel manda el objeto completo)")
    void actualizar_noAdminConValoresIgualesEsInocuo() {
        usuario.setRol(rolUsuario());
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setNombreCompleto("Nombre Nuevo");
        dto.setRolId(2L);
        dto.setActivo(true);
        dto.setCorreo("JUAN@empresa.com");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        guardarDevuelveLoMismo();

        UsuarioResponseDTO respuesta = usuarioService.actualizar(10L, dto, false, "juan@empresa.com");

        assertEquals("Nombre Nuevo", respuesta.getNombreCompleto());
        assertEquals("juan@empresa.com", usuario.getCorreo(), "el correo no se toca");
        assertNull(usuario.getTokenValidoDesde());
    }

    @Test
    @DisplayName("actualizar (no admin) no puede editar el perfil de otra persona")
    void actualizar_noAdminNoEditaAOtros() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setNombreCompleto("Hackeado");

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> usuarioService.actualizar(10L, dto, false, "intruso@empresa.com"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar (no admin) cambiar la contraseña exige la actual y la valida")
    void actualizar_noAdminCambiaPasswordConLaActual() {
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setPassword("NuevaPass123!");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        // Sin la actual
        assertThrows(com.constructora_backend.exception.BadRequestException.class,
                () -> usuarioService.actualizar(10L, dto, false, "juan@empresa.com"));

        // Con una actual incorrecta
        dto.setPasswordActual("incorrecta");
        when(passwordEncoder.matches("incorrecta", "hashed_password")).thenReturn(false);
        assertThrows(com.constructora_backend.exception.BadRequestException.class,
                () -> usuarioService.actualizar(10L, dto, false, "juan@empresa.com"));
        verify(usuarioRepository, never()).save(any());

        // Con la actual correcta
        dto.setPasswordActual("Correcta1!");
        when(passwordEncoder.matches("Correcta1!", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("NuevaPass123!")).thenReturn("hash_nuevo");
        guardarDevuelveLoMismo();

        usuarioService.actualizar(10L, dto, false, "juan@empresa.com");

        assertEquals("hash_nuevo", usuario.getPasswordHash());
        assertNotNull(usuario.getTokenValidoDesde(), "al cambiar la contraseña se cierran las demás sesiones");
    }

    @Test
    @DisplayName("actualizar rechaza (409) desactivar al último administrador activo")
    void actualizar_noPermiteDesactivarAlUltimoAdmin() {
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setActivo(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.contarAdministradoresActivos()).thenReturn(1L);

        assertThrows(com.constructora_backend.exception.ConflictException.class,
                () -> usuarioService.actualizar(10L, dto, true, "otro.admin@empresa.com"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar rechaza (409) quitarle el rol ADMINISTRADOR al último administrador")
    void actualizar_noPermiteQuitarElRolAlUltimoAdmin() {
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setRolId(2L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(2L)).thenReturn(Optional.of(rolUsuario()));
        when(usuarioRepository.contarAdministradoresActivos()).thenReturn(1L);

        assertThrows(com.constructora_backend.exception.ConflictException.class,
                () -> usuarioService.actualizar(10L, dto, true, "otro.admin@empresa.com"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar permite desactivar a un administrador si queda otro activo")
    void actualizar_permiteDesactivarSiQuedaOtroAdmin() {
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setActivo(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.contarAdministradoresActivos()).thenReturn(2L);
        guardarDevuelveLoMismo();

        usuarioService.actualizar(10L, dto, true, "otro.admin@empresa.com");

        assertFalse(usuario.getActivo());
        assertNotNull(usuario.getTokenValidoDesde(), "desactivar la cuenta revoca sus sesiones");
    }

    @Test
    @DisplayName("actualizar rechaza (409) que un administrador desactive su propia cuenta")
    void actualizar_noPermiteAutoDesactivarse() {
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setActivo(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        assertThrows(com.constructora_backend.exception.ConflictException.class,
                () -> usuarioService.actualizar(10L, dto, true, "juan@empresa.com"));
        verify(usuarioRepository, never()).save(any());
    }

    // ───────── eliminar ─────────

    @Test
    @DisplayName("eliminar debe borrar el usuario si existe")
    void eliminar_exito() {
        usuario.setRol(rolUsuario());
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        doNothing().when(usuarioRepository).deleteById(10L);

        assertDoesNotThrow(() -> usuarioService.eliminar(10L, "admin@empresa.com"));
        verify(usuarioRepository).deleteById(10L);
    }

    @Test
    @DisplayName("eliminar debe lanzar ResourceNotFoundException si el usuario no existe")
    void eliminar_noExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.eliminar(99L, "admin@empresa.com"));
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminar rechaza (409) que alguien elimine su propia cuenta")
    void eliminar_noPermiteAutoEliminarse() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        assertThrows(com.constructora_backend.exception.ConflictException.class,
                () -> usuarioService.eliminar(10L, "JUAN@empresa.com"));
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminar rechaza (409) eliminar al último administrador activo, pero permite hacerlo si queda otro")
    void eliminar_ultimoAdmin() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.contarAdministradoresActivos()).thenReturn(1L, 2L);

        assertThrows(com.constructora_backend.exception.ConflictException.class,
                () -> usuarioService.eliminar(10L, "otro.admin@empresa.com"));
        verify(usuarioRepository, never()).deleteById(any());

        assertDoesNotThrow(() -> usuarioService.eliminar(10L, "otro.admin@empresa.com"));
        verify(usuarioRepository).deleteById(10L);
    }
}
