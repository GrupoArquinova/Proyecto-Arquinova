package com.constructora_backend.security;

import com.constructora_backend.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regla por defecto de SecurityConfig: escribir en /api/** (POST, PUT, PATCH, DELETE) exige ADMINISTRADOR, salvo las
 * excepciones públicas (login y recuperación, formulario de contacto, chat) y el perfil propio. Así un endpoint nuevo
 * sin @PreAuthorize no queda abierto por olvido.
 */
@WebMvcTest(controllers = SeguridadPorDefectoTest.EndpointsSinProteger.class)
@Import({SecurityConfig.class, JwtFilter.class, SeguridadPorDefectoTest.EndpointsSinProteger.class})
class SeguridadPorDefectoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    /** Endpoints de prueba SIN @PreAuthorize: lo único que los protege es la regla por defecto. */
    @RestController
    static class EndpointsSinProteger {
        @PostMapping("/api/cosas") public String crear() { return "ok"; }
        @PutMapping("/api/cosas/1") public String actualizar() { return "ok"; }
        @PatchMapping("/api/cosas/1") public String parchar() { return "ok"; }
        @DeleteMapping("/api/cosas/1") public String borrar() { return "ok"; }
        @GetMapping("/api/proyectos/ping") public String leer() { return "ok"; }

        @PostMapping("/api/auth/login") public String login() { return "ok"; }
        @PostMapping("/api/auth/forgot-password") public String olvido() { return "ok"; }
        @PostMapping("/api/solicitudes-contacto/publico") public String contacto() { return "ok"; }
        @PostMapping("/api/chat") public String chat() { return "ok"; }
        @PutMapping("/api/usuarios/7") public String perfilPropio() { return "ok"; }
        @DeleteMapping("/api/usuarios/7") public String borrarUsuario() { return "ok"; }
    }

    private MockHttpServletRequestBuilder conRol(MockHttpServletRequestBuilder peticion, String token, String rol) {
        when(jwtUtils.validarToken(token)).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken(token)).thenReturn(token + "@arquinova.com");
        when(userDetailsService.loadUserByUsername(token + "@arquinova.com"))
                .thenReturn(new User(token + "@arquinova.com", "x", List.of(new SimpleGrantedAuthority(rol))));
        return peticion.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    private MockHttpServletRequestBuilder sinSesion(MockHttpServletRequestBuilder peticion) {
        return peticion.contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    @Test
    @DisplayName("Sin sesión: escribir en /api/** responde 401 aunque el endpoint no tenga @PreAuthorize")
    void sinSesionNoSePuedeEscribir() throws Exception {
        mockMvc.perform(sinSesion(post("/api/cosas"))).andExpect(status().isUnauthorized());
        mockMvc.perform(sinSesion(put("/api/cosas/1"))).andExpect(status().isUnauthorized());
        mockMvc.perform(sinSesion(patch("/api/cosas/1"))).andExpect(status().isUnauthorized());
        mockMvc.perform(sinSesion(delete("/api/cosas/1"))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con sesión pero sin ser administrador: escribir en /api/** responde 403")
    void usuarioSinRolAdminNoPuedeEscribir() throws Exception {
        mockMvc.perform(conRol(post("/api/cosas"), "u1", "ROLE_USUARIO")).andExpect(status().isForbidden());
        mockMvc.perform(conRol(put("/api/cosas/1"), "u1", "ROLE_USUARIO")).andExpect(status().isForbidden());
        mockMvc.perform(conRol(patch("/api/cosas/1"), "u1", "ROLE_USUARIO")).andExpect(status().isForbidden());
        mockMvc.perform(conRol(delete("/api/cosas/1"), "u1", "ROLE_USUARIO")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El administrador sí puede escribir")
    void administradorPuedeEscribir() throws Exception {
        mockMvc.perform(conRol(post("/api/cosas"), "adm", "ROLE_ADMINISTRADOR")).andExpect(status().isOk());
        mockMvc.perform(conRol(put("/api/cosas/1"), "adm", "ROLE_ADMINISTRADOR")).andExpect(status().isOk());
        mockMvc.perform(conRol(patch("/api/cosas/1"), "adm", "ROLE_ADMINISTRADOR")).andExpect(status().isOk());
        mockMvc.perform(conRol(delete("/api/cosas/1"), "adm", "ROLE_ADMINISTRADOR")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Las excepciones públicas siguen abiertas sin sesión: login, recuperación, contacto y chat")
    void excepcionesPublicasSiguenAbiertas() throws Exception {
        mockMvc.perform(sinSesion(post("/api/auth/login"))).andExpect(status().isOk());
        mockMvc.perform(sinSesion(post("/api/auth/forgot-password"))).andExpect(status().isOk());
        mockMvc.perform(sinSesion(post("/api/solicitudes-contacto/publico"))).andExpect(status().isOk());
        mockMvc.perform(sinSesion(post("/api/chat"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Las lecturas públicas del sitio siguen abiertas")
    void lecturasPublicasSiguenAbiertas() throws Exception {
        mockMvc.perform(get("/api/proyectos/ping")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una persona con sesión (sin ser administradora) puede editar su perfil (PUT /api/usuarios/{id}); sin sesión no")
    void elPerfilPropioSigueFuncionando() throws Exception {
        mockMvc.perform(conRol(put("/api/usuarios/7"), "u1", "ROLE_USUARIO")).andExpect(status().isOk());
        mockMvc.perform(sinSesion(put("/api/usuarios/7"))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Pero eliminar usuarios sigue siendo solo de administradores")
    void eliminarUsuariosSoloAdministradores() throws Exception {
        mockMvc.perform(conRol(delete("/api/usuarios/7"), "u1", "ROLE_USUARIO")).andExpect(status().isForbidden());
        mockMvc.perform(conRol(delete("/api/usuarios/7"), "adm", "ROLE_ADMINISTRADOR")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una cuenta desactivada no entra aunque su token siga vigente")
    void cuentaDesactivadaNoAutentica() throws Exception {
        when(jwtUtils.validarToken("baja")).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken("baja")).thenReturn("baja@arquinova.com");
        when(userDetailsService.loadUserByUsername("baja@arquinova.com"))
                .thenReturn(new User("baja@arquinova.com", "x", false, true, true, true,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));

        mockMvc.perform(post("/api/cosas").header("Authorization", "Bearer baja")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token emitido antes de un cambio de contraseña o de rol (token_valido_desde) deja de valer")
    void tokenAnteriorAlCambioDeCredencialesNoAutentica() throws Exception {
        when(jwtUtils.validarToken("viejo")).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken("viejo")).thenReturn("adm@arquinova.com");
        when(jwtUtils.obtenerEmisionMillis("viejo")).thenReturn(System.currentTimeMillis() - 3_600_000);
        when(userDetailsService.loadUserByUsername("adm@arquinova.com"))
                .thenReturn(new UsuarioPrincipal("adm@arquinova.com", "x", true,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")), java.time.LocalDateTime.now().minusMinutes(10)));

        mockMvc.perform(post("/api/cosas").header("Authorization", "Bearer viejo")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        // Un token emitido después del cambio sí entra
        when(jwtUtils.validarToken("nuevo")).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken("nuevo")).thenReturn("adm@arquinova.com");
        when(jwtUtils.obtenerEmisionMillis("nuevo")).thenReturn(System.currentTimeMillis());

        mockMvc.perform(post("/api/cosas").header("Authorization", "Bearer nuevo")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }
}
