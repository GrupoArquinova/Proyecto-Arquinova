package com.constructora_backend.controller;

import com.constructora_backend.config.SecurityConfig;
import com.constructora_backend.exception.ServicioNoDisponibleException;
import com.constructora_backend.security.JwtFilter;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.EmailService;
import com.constructora_backend.service.GoogleWalletService;
import com.constructora_backend.service.LimitadorPasesWallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Envío público de la tarjeta de Google Wallet: seguridad real, validación del correo y límites. */
@WebMvcTest(WalletController.class)
@Import({SecurityConfig.class, JwtFilter.class, LimitadorPasesWallet.class, com.constructora_backend.service.LimitadorVentana.class})
class WalletControllerTest {

    private static final String URL = "/api/wallet/enviar-pase";
    private static final String ENLACE = "https://pay.google.com/gp/v/save/abc.def.ghi";

    @Autowired private MockMvc mockMvc;
    @Autowired private LimitadorPasesWallet limitador;
    @MockBean private GoogleWalletService googleWalletService;
    @MockBean private EmailService emailService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    /** Cada petición sale de una IP distinta: así el límite por IP del filtro no interfiere con lo que se prueba aquí. */
    private static final java.util.concurrent.atomic.AtomicInteger SIGUIENTE_IP = new java.util.concurrent.atomic.AtomicInteger();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        ((java.util.Map<String, ?>) org.springframework.test.util.ReflectionTestUtils.getField(limitador, "ultimoEnvio")).clear();
        when(googleWalletService.disponible()).thenReturn(true);
        when(googleWalletService.generarEnlace(anyString())).thenReturn(ENLACE);
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String cuerpo) throws Exception {
        String ip = "10.9." + (SIGUIENTE_IP.incrementAndGet() / 250) + "." + (SIGUIENTE_IP.get() % 250);
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .with(peticion -> {
                    peticion.setRemoteAddr(ip);
                    return peticion;
                }));
    }

    @Test
    @DisplayName("Público: sin iniciar sesión envía el correo con el enlace de la tarjeta")
    void enviaElCorreo() throws Exception {
        enviar("{\"email\":\"cliente@correo.com\"}").andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").exists());

        verify(emailService).enviarCorreoConBotonWallet("cliente@correo.com", ENLACE);
    }

    @Test
    @DisplayName("Correo vacío, sin formato o demasiado largo: 400 y no se manda nada")
    void validaElCorreo() throws Exception {
        for (String cuerpo : new String[]{"{}", "{\"email\":\"\"}", "{\"email\":\"   \"}", "{\"email\":\"sin-arroba\"}",
                "{\"email\":\"a@b\"}", "{\"email\":\"a b@c.com\"}",
                "{\"email\":\"" + "a".repeat(250) + "@c.com\"}", "no es json"}) {
            enviar(cuerpo).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Sin credenciales de Google la respuesta es 503 y no se gasta el cupo ni se manda correo")
    void sinConfigurar() throws Exception {
        when(googleWalletService.disponible()).thenReturn(false);

        enviar("{\"email\":\"cliente@correo.com\"}").andExpect(status().isServiceUnavailable());

        verifyNoInteractions(emailService);
        when(googleWalletService.disponible()).thenReturn(true);
        enviar("{\"email\":\"cliente@correo.com\"}").andExpect(status().isOk());
    }

    @Test
    @DisplayName("A cada dirección se le manda una vez por hora (sin distinguir mayúsculas): la segunda recibe 429")
    void unaPorHoraYPorDireccion() throws Exception {
        enviar("{\"email\":\"cliente@correo.com\"}").andExpect(status().isOk());
        enviar("{\"email\":\"CLIENTE@correo.com\"}").andExpect(status().isTooManyRequests());
        enviar("{\"email\":\"otro@correo.com\"}").andExpect(status().isOk());

        verify(emailService).enviarCorreoConBotonWallet("cliente@correo.com", ENLACE);
        verify(emailService, never()).enviarCorreoConBotonWallet("CLIENTE@correo.com", ENLACE);
    }

    @Test
    @DisplayName("Si falla al preparar el enlace, la dirección puede volver a intentarlo en seguida")
    void siFallaSeLiberaElCupo() throws Exception {
        when(googleWalletService.generarEnlace(anyString())).thenThrow(new ServicioNoDisponibleException("x"));
        enviar("{\"email\":\"cliente@correo.com\"}").andExpect(status().isServiceUnavailable());

        org.mockito.Mockito.doReturn(ENLACE).when(googleWalletService).generarEnlace(anyString());
        enviar("{\"email\":\"cliente@correo.com\"}").andExpect(status().isOk());
    }

    @Test
    @DisplayName("El error no revela detalles internos")
    void errorSinDetalles() throws Exception {
        doThrow(new ServicioNoDisponibleException("La tarjeta digital no está disponible por ahora."))
                .when(googleWalletService).generarEnlace(anyString());

        enviar("{\"email\":\"cliente@correo.com\"}").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("La tarjeta digital no está disponible por ahora."));
    }
}
