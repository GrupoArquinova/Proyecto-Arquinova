package com.constructora_backend.controller;

import com.constructora_backend.config.SanitizacionJacksonConfig;
import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.SolicitudContactoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** De punta a punta en el controlador: lo que el servicio recibe ya viene limpio, y los campos de una sola línea siguen rechazando saltos de línea. */
@WebMvcTest(SolicitudContactoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SanitizacionJacksonConfig.class)
class SanitizacionEnControladorTest {

    private static final String URL = "/api/solicitudes-contacto/publico";

    @Autowired private MockMvc mockMvc;
    @MockBean private SolicitudContactoService solicitudService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("El servicio recibe nombre, servicio y mensaje sin etiquetas ni scripts")
    void elServicioRecibeTextoLimpio() throws Exception {
        when(solicitudService.crearPublica(any(SolicitudContactoPublicDTO.class), any(), any()))
                .thenReturn(new SolicitudContactoResponseDTO());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"<script>alert('x')</script>Ana <b>Gómez</b>","correo":"ana@correo.com",
                         "servicioInteres":"<img src=x onerror=alert(1)>Diseño",
                         "mensaje":"Hola\\n<iframe src=\\"https://evil.test\\"></iframe>Quiero información",
                         "consentimientoDatos":true}"""))
                .andExpect(status().isCreated());

        ArgumentCaptor<SolicitudContactoPublicDTO> captor = ArgumentCaptor.forClass(SolicitudContactoPublicDTO.class);
        verify(solicitudService).crearPublica(captor.capture(), any(), any());
        assertThat(captor.getValue().getNombre()).isEqualTo("Ana Gómez");
        assertThat(captor.getValue().getServicioInteres()).isEqualTo("Diseño");
        assertThat(captor.getValue().getMensaje()).isEqualTo("Hola\nQuiero información");
    }

    @Test
    @DisplayName("Un nombre con salto de línea sigue rechazándose con 400 (la limpieza no lo deja pasar)")
    void elSaltoDeLineaEnElNombreSigueRechazado() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"nombre\":\"Ana\\nBcc: otro@evil.test\",\"correo\":\"ana@correo.com\",\"consentimientoDatos\":true}"))
                .andExpect(status().isBadRequest());

        verify(solicitudService, never()).crearPublica(any(), any(), any());
    }

    @Test
    @DisplayName("Si todo el nombre era una etiqueta, queda vacío y se rechaza por obligatorio")
    void nombreSoloMarcado() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"nombre\":\"<script>alert(1)</script>\",\"correo\":\"ana@correo.com\",\"consentimientoDatos\":true}"))
                .andExpect(status().isBadRequest());

        verify(solicitudService, never()).crearPublica(any(), any(), any());
    }
}
