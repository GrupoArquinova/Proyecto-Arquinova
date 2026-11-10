package com.constructora_backend.security;

import com.constructora_backend.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JwtFilterAndSecurityIntegrationTest.TestSecurityController.class)
@Import({SecurityConfig.class, JwtFilter.class, JwtFilterAndSecurityIntegrationTest.TestSecurityController.class})
class JwtFilterAndSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @RestController
    @RequestMapping("/api")
    static class TestSecurityController {

        @GetMapping("/proyectos/publico")
        public String endpointPublico() {
            return "Contenido publico";
        }

        @GetMapping("/protegido")
        public String endpointProtegido() {
            return "Contenido protegido";
        }

        @GetMapping("/admin/panel")
        public String endpointAdmin() {
            return "Panel admin";
        }
    }

    private UserDetails usuarioNormal;
    private UserDetails usuarioAdmin;

    @BeforeEach
    void setUp() {
        usuarioNormal = new User("user@constructora.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
        usuarioAdmin = new User("admin@constructora.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
    }

    @Test
    @DisplayName("GET /api/proyectos/publico - Debe permitir el acceso sin token (HTTP 200)")
    void accesoEndpointPublico_Exitoso() throws Exception {
        mockMvc.perform(get("/api/proyectos/publico"))
                .andExpect(status().isOk())
                .andExpect(content().string("Contenido publico"));
    }

    @Test
    @DisplayName("GET /api/protegido - Debe rechazar el acceso sin token (HTTP 401)")
    void accesoEndpointProtegido_SinToken_Devuelve401() throws Exception {
        mockMvc.perform(get("/api/protegido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No autorizado"))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @DisplayName("GET /api/protegido - Debe rechazar el acceso con token invalido o corrupto (HTTP 401)")
    void accesoEndpointProtegido_TokenInvalido_Devuelve401() throws Exception {
        when(jwtUtils.validarToken("token_invalido")).thenReturn(false);

        mockMvc.perform(get("/api/protegido")
                        .header("Authorization", "Bearer token_invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No autorizado"));
    }

    @Test
    @DisplayName("GET /api/protegido - Debe permitir el acceso con token valido (HTTP 200)")
    void accesoEndpointProtegido_TokenValido_Devuelve200() throws Exception {
        String tokenValido = "token_valido_123";
        when(jwtUtils.validarToken(tokenValido)).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken(tokenValido)).thenReturn("user@constructora.com");
        when(userDetailsService.loadUserByUsername("user@constructora.com")).thenReturn(usuarioNormal);

        mockMvc.perform(get("/api/protegido")
                        .header("Authorization", "Bearer " + tokenValido))
                .andExpect(status().isOk())
                .andExpect(content().string("Contenido protegido"));
    }

    @Test
    @DisplayName("GET /api/admin/panel - Debe rechazar acceso (HTTP 403) para usuario sin rol ADMINISTRADOR")
    void accesoEndpointAdmin_UsuarioSinRolAdmin_Devuelve403() throws Exception {
        String tokenUsuario = "token_user_123";
        when(jwtUtils.validarToken(tokenUsuario)).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken(tokenUsuario)).thenReturn("user@constructora.com");
        when(userDetailsService.loadUserByUsername("user@constructora.com")).thenReturn(usuarioNormal);

        mockMvc.perform(get("/api/admin/panel")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/panel - Debe permitir acceso (HTTP 200) para usuario con rol ADMINISTRADOR")
    void accesoEndpointAdmin_UsuarioConRolAdmin_Devuelve200() throws Exception {
        String tokenAdmin = "token_admin_123";
        when(jwtUtils.validarToken(tokenAdmin)).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken(tokenAdmin)).thenReturn("admin@constructora.com");
        when(userDetailsService.loadUserByUsername("admin@constructora.com")).thenReturn(usuarioAdmin);

        mockMvc.perform(get("/api/admin/panel")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(content().string("Panel admin"));
    }
}
