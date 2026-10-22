package com.constructora_backend.controller;

import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.SolicitudContactoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitudContactoController.class)
@AutoConfigureMockMvc(addFilters = false)
class SolicitudContactoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SolicitudContactoService solicitudService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private static final String CUERPO = "{\"nombre\":\"Ana\",\"correo\":\"ana@test.com\",\"mensaje\":\"Hola\","
            + "\"consentimientoDatos\":true,\"ip\":\"6.6.6.6\",\"userAgent\":\"falso\"}";

    @Test
    @DisplayName("POST /publico pasa al servicio la IP de la conexión y el user-agent del encabezado, no los del cuerpo")
    void ipYUserAgentDeLaPeticion() throws Exception {
        SolicitudContactoResponseDTO respuesta = new SolicitudContactoResponseDTO();
        respuesta.setId(5L);
        when(solicitudService.crearPublica(any(SolicitudContactoPublicDTO.class), anyString(), anyString())).thenReturn(respuesta);

        mockMvc.perform(post("/api/solicitudes-contacto/publico")
                        .with(r -> {
                            r.setRemoteAddr("203.0.113.9");
                            return r;
                        })
                        .header("User-Agent", "Mozilla/5.0 de prueba")
                        .header("X-Forwarded-For", "6.6.6.6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));

        verify(solicitudService).crearPublica(any(SolicitudContactoPublicDTO.class), eq("203.0.113.9"), eq("Mozilla/5.0 de prueba"));
    }

    @Test
    @DisplayName("POST /publico con campos que ya no existen (ip, userAgent) sigue funcionando: se ignoran")
    void camposViejosSeIgnoran() throws Exception {
        when(solicitudService.crearPublica(any(), any(), any())).thenReturn(new SolicitudContactoResponseDTO());

        mockMvc.perform(post("/api/solicitudes-contacto/publico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /publico con saltos de línea en el nombre responde 400")
    void nombreConSaltosDeLineaSeRechaza() throws Exception {
        mockMvc.perform(post("/api/solicitudes-contacto/publico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\\r\\nBcc: x@y.com\",\"correo\":\"ana@test.com\",\"consentimientoDatos\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /publico con un mensaje de más de 2000 caracteres responde 400")
    void mensajeLargoSeRechaza() throws Exception {
        mockMvc.perform(post("/api/solicitudes-contacto/publico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana@test.com\",\"consentimientoDatos\":true,\"mensaje\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /por-correo devuelve cuántas solicitudes se eliminaron")
    void eliminarPorCorreo() throws Exception {
        when(solicitudService.eliminarPorCorreo("ana@test.com")).thenReturn(4);

        mockMvc.perform(delete("/api/solicitudes-contacto/por-correo").param("correo", "ana@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminadas").value(4));
    }

    @Test
    @DisplayName("El controlador ya no abre CORS a cualquier origen (@CrossOrigin): lo decide la configuración global")
    void sinCrossOriginAbierto() throws Exception {
        assertFalse(SolicitudContactoController.class.isAnnotationPresent(org.springframework.web.bind.annotation.CrossOrigin.class));
        for (Method m : SolicitudContactoController.class.getDeclaredMethods()) {
            assertFalse(m.isAnnotationPresent(org.springframework.web.bind.annotation.CrossOrigin.class), m.getName());
        }
    }

    @Test
    @DisplayName("DELETE /por-correo exige ADMINISTRADOR y queda auditado")
    void eliminarPorCorreoProtegidoYAuditado() throws Exception {
        Method m = SolicitudContactoController.class.getDeclaredMethod("eliminarPorCorreo", String.class);

        org.springframework.security.access.prepost.PreAuthorize pre = m.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(pre);
        assertEquals("hasRole('ADMINISTRADOR')", pre.value());
        com.constructora_backend.aspect.Auditable auditable = m.getAnnotation(com.constructora_backend.aspect.Auditable.class);
        assertNotNull(auditable);
        assertEquals("SOLICITUDES_CONTACTO", auditable.entidad());
        assertTrue(auditable.accion().contains("CORREO"));
    }
}
