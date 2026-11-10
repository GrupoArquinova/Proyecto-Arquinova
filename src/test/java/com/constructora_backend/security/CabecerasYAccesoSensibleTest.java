package com.constructora_backend.security;

import com.constructora_backend.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cabeceras de seguridad de la API y acceso a lo sensible: Swagger (cerrado por defecto) y las métricas de Prometheus
 * (solo con el token de métricas).
 */
@WebMvcTest(controllers = CabecerasYAccesoSensibleTest.PruebaController.class)
@Import({SecurityConfig.class, JwtFilter.class, MetricasTokenFilter.class, CabecerasYAccesoSensibleTest.PruebaController.class})
@TestPropertySource(properties = "app.seguridad.metricas-token=token-de-metricas-de-prueba")
class CabecerasYAccesoSensibleTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @RestController
    static class PruebaController {
        @GetMapping("/api/proyectos/ping")
        public String ping() {
            return "ok";
        }

        @GetMapping("/actuator/prometheus")
        public String metricas() {
            return "# métricas";
        }
    }

    @Test
    @DisplayName("La API envía HSTS por HTTPS, CSP cerrada y las demás cabeceras de seguridad")
    void cabecerasDeSeguridad() throws Exception {
        mockMvc.perform(get("/api/proyectos/ping").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Referrer-Policy"));
    }

    @Test
    @DisplayName("Swagger está cerrado por defecto: sin sesión no se ve (HTTP 401)")
    void swaggerCerradoSinSesion() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Swagger cerrado también para un usuario sin rol de administrador")
    void swaggerCerradoParaUsuarioSinRol() throws Exception {
        when(jwtUtils.validarToken("token-usuario")).thenReturn(true);
        when(jwtUtils.obtenerCorreoDelToken("token-usuario")).thenReturn("usuario@arquinova.com");
        when(userDetailsService.loadUserByUsername("usuario@arquinova.com"))
                .thenReturn(new User("usuario@arquinova.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));

        mockMvc.perform(get("/v3/api-docs").header("Authorization", "Bearer token-usuario"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Las métricas rechazan a quien no trae el token de métricas")
    void metricasSinToken() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer otro-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Las métricas se entregan con el token de métricas")
    void metricasConToken() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer token-de-metricas-de-prueba"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El token de métricas no abre ninguna otra ruta protegida")
    void tokenDeMetricasSoloSirveParaMetricas() throws Exception {
        mockMvc.perform(get("/api/protegido").header("Authorization", "Bearer token-de-metricas-de-prueba"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").header("Authorization", "Bearer token-de-metricas-de-prueba"))
                .andExpect(status().isUnauthorized());
    }
}
