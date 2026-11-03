package com.constructora_backend.security;

import com.constructora_backend.dto.request.UsuarioUpdateDTO;
import com.constructora_backend.entity.Rol;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.exception.ConflictException;
import com.constructora_backend.mapper.UsuarioMapper;
import com.constructora_backend.repository.RolRepository;
import com.constructora_backend.repository.UsuarioRepository;
import com.constructora_backend.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Roles COMERCIAL y EDITOR: cómo se guardan, cómo se autentican y que no se puede escalar ni dejar el sistema sin administrador. */
class RolesNuevosTest {

    private static Rol rol(long id, String nombre, boolean activo) {
        Rol r = new Rol();
        r.setId(id);
        r.setNombre(nombre);
        r.setActivo(activo);
        return r;
    }

    private static Usuario usuario(long id, String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setCorreo(correo);
        u.setNombreCompleto("Persona " + id);
        u.setPasswordHash("hash");
        u.setActivo(true);
        u.setRol(rol);
        return u;
    }

    // ------------------------------------------------------------------ autenticación

    @Test
    @DisplayName("El rol se guarda con o sin prefijo ROLE_ y siempre se autentica como ROLE_<nombre>")
    void elPrefijoSeAgregaSoloUnaVez() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        UserDetailsServiceImpl servicio = new UserDetailsServiceImpl(repo);
        when(repo.findByCorreo("c@a.co")).thenReturn(Optional.of(usuario(1, "c@a.co", rol(2, "COMERCIAL", true))));
        when(repo.findByCorreo("e@a.co")).thenReturn(Optional.of(usuario(2, "e@a.co", rol(3, "ROLE_EDITOR", true))));

        UserDetails comercial = servicio.loadUserByUsername("c@a.co");
        UserDetails editor = servicio.loadUserByUsername("e@a.co");

        assertEquals("ROLE_COMERCIAL", comercial.getAuthorities().iterator().next().getAuthority());
        assertEquals("ROLE_EDITOR", editor.getAuthorities().iterator().next().getAuthority());
    }

    // ------------------------------------------------------------------ helpers de rol

    @Test
    @DisplayName("Permisos: quién edita contenido y quién puede leer todo")
    void helpersDePermisos() {
        var admin = new TestingAuthenticationToken("a", "x", "ROLE_ADMINISTRADOR");
        var editor = new TestingAuthenticationToken("e", "x", "ROLE_EDITOR");
        var comercial = new TestingAuthenticationToken("c", "x", "ROLE_COMERCIAL");
        var otro = new TestingAuthenticationToken("o", "x", "ROLE_USUARIO");

        assertTrue(Permisos.puedeEditarContenido(admin));
        assertTrue(Permisos.puedeEditarContenido(editor));
        assertFalse(Permisos.puedeEditarContenido(comercial));
        assertFalse(Permisos.puedeEditarContenido(otro));
        assertFalse(Permisos.puedeEditarContenido(null));

        assertTrue(Permisos.puedeLeerTodo(admin));
        assertTrue(Permisos.puedeLeerTodo(editor));
        assertTrue(Permisos.puedeLeerTodo(comercial));
        assertFalse(Permisos.puedeLeerTodo(otro));
        assertFalse(Permisos.puedeLeerTodo(null));

        // Sin prefijo no cuenta: así coincide con hasRole(), que siempre exige ROLE_
        assertFalse(Permisos.tieneAlgunRol(new TestingAuthenticationToken("x", "x", "EDITOR"), Permisos.EDITOR));
    }

    // ------------------------------------------------------------------ usuarios y roles

    private UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private RolRepository roles = mock(RolRepository.class);
    private PasswordEncoder encoder = mock(PasswordEncoder.class);
    private UsuarioService servicio = new UsuarioService(usuarios, roles, new UsuarioMapper(), encoder);

    @Test
    @DisplayName("Un administrador puede asignar COMERCIAL o EDITOR a otra persona")
    void administradorAsignaRolesNuevos() {
        Rol admin = rol(1, "ADMINISTRADOR", true);
        Rol comercial = rol(2, "COMERCIAL", true);
        Usuario persona = usuario(7, "p@a.co", rol(3, "EDITOR", true));
        when(usuarios.findById(7L)).thenReturn(Optional.of(persona));
        when(roles.findById(2L)).thenReturn(Optional.of(comercial));
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));

        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setRolId(2L);
        var respuesta = servicio.actualizar(7L, dto, true, "admin@a.co");

        assertEquals(2L, respuesta.getRolId());
        assertEquals("COMERCIAL", respuesta.getRolNombre());
        assertTrue(persona.getTokenValidoDesde() != null, "cambiar el rol revoca las sesiones abiertas");
        assertTrue(admin.getActivo());
    }

    @Test
    @DisplayName("No se puede asignar un rol desactivado")
    void rolInactivoSeRechaza() {
        Usuario persona = usuario(7, "p@a.co", rol(3, "EDITOR", true));
        when(usuarios.findById(7L)).thenReturn(Optional.of(persona));
        when(roles.findById(9L)).thenReturn(Optional.of(rol(9, "COMERCIAL", false)));

        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setRolId(9L);

        assertThrows(BadRequestException.class, () -> servicio.actualizar(7L, dto, true, "admin@a.co"));
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Regresión: un COMERCIAL o EDITOR no puede subirse a ADMINISTRADOR editando su propio perfil")
    void nadieSeAscendiAsiMismo() {
        for (String nombreRol : new String[]{"COMERCIAL", "EDITOR", "ROLE_COMERCIAL"}) {
            Usuario yo = usuario(5, "yo@a.co", rol(2, nombreRol, true));
            when(usuarios.findById(5L)).thenReturn(Optional.of(yo));

            UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
            dto.setRolId(1L);

            assertThrows(AccessDeniedException.class, () -> servicio.actualizar(5L, dto, false, "yo@a.co"),
                    nombreRol + " no debe poder cambiarse el rol");
        }
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Regresión: un COMERCIAL o EDITOR no puede editar a otra persona")
    void nadieEditaAOtros() {
        Usuario otra = usuario(8, "otra@a.co", rol(1, "ADMINISTRADOR", true));
        when(usuarios.findById(8L)).thenReturn(Optional.of(otra));

        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setNombreCompleto("Cambiado");

        assertThrows(AccessDeniedException.class, () -> servicio.actualizar(8L, dto, false, "editor@a.co"));
    }

    @Test
    @DisplayName("No se deja el sistema sin administrador activo: pasar al último administrador a COMERCIAL o EDITOR da 409")
    void ultimoAdministradorNoPuedeBajarDeRol() {
        Usuario unicoAdmin = usuario(1, "admin@a.co", rol(1, "ROLE_ADMINISTRADOR", true));
        when(usuarios.findById(1L)).thenReturn(Optional.of(unicoAdmin));
        when(usuarios.contarAdministradoresActivos()).thenReturn(1L);
        for (long idRol : new long[]{2, 3}) {
            when(roles.findById(idRol)).thenReturn(Optional.of(rol(idRol, idRol == 2 ? "COMERCIAL" : "EDITOR", true)));
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
            dto.setRolId(idRol);
            assertThrows(ConflictException.class, () -> servicio.actualizar(1L, dto, true, "otro-admin@a.co"));
        }
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Con otro administrador activo sí se puede pasar a un administrador a COMERCIAL")
    void conOtroAdministradorSePuedeBajarDeRol() {
        Usuario admin = usuario(1, "admin@a.co", rol(1, "ADMINISTRADOR", true));
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarios.contarAdministradoresActivos()).thenReturn(2L);
        when(roles.findById(2L)).thenReturn(Optional.of(rol(2, "COMERCIAL", true)));
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));

        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setRolId(2L);

        assertEquals("COMERCIAL", servicio.actualizar(1L, dto, true, "otro-admin@a.co").getRolNombre());
    }
}
