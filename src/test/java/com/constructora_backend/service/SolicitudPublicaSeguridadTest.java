package com.constructora_backend.service;

import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.entity.EstadoSolicitud;
import com.constructora_backend.entity.SolicitudContacto;
import com.constructora_backend.mapper.SolicitudContactoMapper;
import com.constructora_backend.repository.CasaModeloRepository;
import com.constructora_backend.repository.EstadoSolicitudRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.SolicitudContactoRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Alta pública: IP y user-agent del servidor, política aceptada, campo trampa, respuesta sin datos internos y retención. */
@ExtendWith(MockitoExtension.class)
class SolicitudPublicaSeguridadTest {

    @Mock private SolicitudContactoRepository solicitudRepository;
    @Mock private EstadoSolicitudRepository estadoSolicitudRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private CasaModeloRepository casaModeloRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;
    @Mock private CaptchaService captchaService;
    @Spy private SolicitudContactoMapper mapper = new SolicitudContactoMapper();

    @InjectMocks private SolicitudContactoService servicio;

    @BeforeEach
    void preparar() {
        EstadoSolicitud nuevo = new EstadoSolicitud();
        nuevo.setId(1);
        nuevo.setNombre("NUEVO");
        lenient().when(captchaService.verificar(any(), any())).thenReturn(true);
        lenient().when(estadoSolicitudRepository.findFirstByNombreIgnoreCase("NUEVO")).thenReturn(Optional.of(nuevo));
        lenient().when(solicitudRepository.save(any(SolicitudContacto.class))).thenAnswer(i -> {
            SolicitudContacto s = i.getArgument(0);
            s.setId(77L);
            return s;
        });
        ReflectionTestUtils.setField(servicio, "versionPolitica", "2026-10");
    }

    private SolicitudContactoPublicDTO pedido() {
        SolicitudContactoPublicDTO dto = new SolicitudContactoPublicDTO();
        dto.setNombre("Ana");
        dto.setCorreo("ana@test.com");
        dto.setMensaje("Hola");
        dto.setConsentimientoDatos(true);
        return dto;
    }

    @Test
    @DisplayName("La IP y el user-agent los pone el servidor a partir de la petición")
    void ipYUserAgentDelServidor() {
        servicio.crearPublica(pedido(), "203.0.113.9", "Mozilla/5.0 (X11)");

        ArgumentCaptor<SolicitudContacto> guardada = ArgumentCaptor.forClass(SolicitudContacto.class);
        verify(solicitudRepository).save(guardada.capture());
        assertEquals("203.0.113.9", guardada.getValue().getIp());
        assertEquals("Mozilla/5.0 (X11)", guardada.getValue().getUserAgent());
    }

    @Test
    @DisplayName("El user-agent se recorta a 500 caracteres y se le quitan saltos de línea y caracteres de control")
    void userAgentRecortadoYSaneado() {
        servicio.crearPublica(pedido(), "203.0.113.9", "x".repeat(900) + "\r\nInyeccion");

        ArgumentCaptor<SolicitudContacto> guardada = ArgumentCaptor.forClass(SolicitudContacto.class);
        verify(solicitudRepository).save(guardada.capture());
        assertEquals(500, guardada.getValue().getUserAgent().length());

        assertEquals("a b", SolicitudContactoService.recortar("a\r\nb", 500));
        assertNull(SolicitudContactoService.recortar("   ", 500));
        assertNull(SolicitudContactoService.recortar(null, 500));
        assertEquals("1234", SolicitudContactoService.recortar("123456", 4));
    }

    @Test
    @DisplayName("Se guarda la versión de la política aceptada y la fecha del consentimiento")
    void versionDePoliticaYFechaDeConsentimiento() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        ReflectionTestUtils.setField(servicio, "versionPolitica", "2027-03");

        servicio.crearPublica(pedido(), "203.0.113.9", "ua");

        ArgumentCaptor<SolicitudContacto> guardada = ArgumentCaptor.forClass(SolicitudContacto.class);
        verify(solicitudRepository).save(guardada.capture());
        assertEquals("2027-03", guardada.getValue().getVersionPolitica());
        assertNotNull(guardada.getValue().getFechaConsentimiento());
        assertTrue(guardada.getValue().getFechaConsentimiento().isAfter(antes));
    }

    @Test
    @DisplayName("Campo trampa con texto: responde como si todo saliera bien pero no guarda ni envía ningún correo")
    void honeypotNoGuardaNiEnvia() {
        SolicitudContactoPublicDTO dto = pedido();
        dto.setSitioWeb("http://spam.example");

        SolicitudContactoResponseDTO respuesta = servicio.crearPublica(dto, "203.0.113.9", "bot");

        assertNotNull(respuesta, "responde igual que con una solicitud normal");
        assertEquals("Ana", respuesta.getNombre());
        assertNull(respuesta.getId(), "sin identificador: no se creó nada");
        verify(solicitudRepository, never()).save(any());
        verifyNoInteractions(emailService);
        verifyNoInteractions(estadoSolicitudRepository);
    }

    @Test
    @DisplayName("Captcha no superado: 400 genérico y no se guarda ni se envía nada")
    void captchaRechazadoNoGuardaNiEnvia() {
        org.mockito.Mockito.when(captchaService.verificar("token-malo", "203.0.113.9")).thenReturn(false);
        SolicitudContactoPublicDTO dto = pedido();
        dto.setCaptchaToken("token-malo");

        com.constructora_backend.exception.BadRequestException ex = org.junit.jupiter.api.Assertions.assertThrows(com.constructora_backend.exception.BadRequestException.class,
                () -> servicio.crearPublica(dto, "203.0.113.9", "ua"));

        assertEquals("Verificación de seguridad no superada", ex.getMessage());
        verify(solicitudRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("El honeypot se evalúa antes que el captcha: un robot ni siquiera consulta a Cloudflare")
    void honeypotAntesQueCaptcha() {
        SolicitudContactoPublicDTO dto = pedido();
        dto.setSitioWeb("http://spam.example");

        servicio.crearPublica(dto, "203.0.113.9", "bot");

        verifyNoInteractions(captchaService);
    }

    @Test
    @DisplayName("Captcha superado: el token y la IP del servidor se pasan a la verificación y la solicitud se guarda")
    void captchaValidoGuarda() {
        SolicitudContactoPublicDTO dto = pedido();
        dto.setCaptchaToken("token-bueno");

        servicio.crearPublica(dto, "203.0.113.9", "ua");

        verify(captchaService).verificar("token-bueno", "203.0.113.9");
        verify(solicitudRepository).save(any(SolicitudContacto.class));
    }

    @Test
    @DisplayName("La respuesta pública no incluye IP, user-agent ni datos de gestión interna")
    void respuestaSinDatosInternos() {
        SolicitudContactoResponseDTO respuesta = servicio.crearPublica(pedido(), "203.0.113.9", "Mozilla");

        assertEquals(77L, respuesta.getId());
        assertEquals("Ana", respuesta.getNombre());
        assertNull(respuesta.getIp());
        assertNull(respuesta.getUserAgent());
        assertNull(respuesta.getObservacionesInternas());
        assertNull(respuesta.getAtendidaPorNombre());
        assertNull(respuesta.getVersionPolitica());
    }

    @Test
    @DisplayName("La confirmación al visitante no lleva su mensaje (texto libre)")
    void laConfirmacionNoLlevaElMensaje() {
        SolicitudContactoPublicDTO dto = pedido();
        dto.setMensaje("Visita http://malo.example para ganar dinero");

        servicio.crearPublica(dto, "203.0.113.9", "ua");

        verify(emailService).enviarConfirmacionSolicitud(anyString(), anyString(), any(), any(), org.mockito.ArgumentMatchers.isNull(), anyString());
    }

    // ───────── Retención y atención de titulares ─────────

    @Test
    @DisplayName("eliminarPorCorreo borra las solicitudes de ese correo; sin correo, 400")
    void eliminarPorCorreo() {
        org.mockito.Mockito.when(solicitudRepository.eliminarPorCorreo("ana@test.com")).thenReturn(3);

        assertEquals(3, servicio.eliminarPorCorreo("  ana@test.com "));
        org.junit.jupiter.api.Assertions.assertThrows(com.constructora_backend.exception.BadRequestException.class,
                () -> servicio.eliminarPorCorreo(" "));
    }

    @Test
    @DisplayName("La tarea de retención elimina las solicitudes con más meses que el plazo configurado")
    void retencionEliminaLasAntiguas() {
        RetencionDatosJob job = new RetencionDatosJob(servicio);
        ReflectionTestUtils.setField(job, "mesesSolicitudes", 24);
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 22, 3, 0);
        org.mockito.Mockito.when(solicitudRepository.eliminarAnterioresA(LocalDateTime.of(2024, 10, 22, 3, 0))).thenReturn(5);

        assertEquals(5, job.depurarSolicitudes(ahora));
    }

    @Test
    @DisplayName("Con 0 meses la retención está desactivada y no borra nada")
    void retencionDesactivada() {
        RetencionDatosJob job = new RetencionDatosJob(servicio);
        ReflectionTestUtils.setField(job, "mesesSolicitudes", 0);

        assertEquals(0, job.depurarSolicitudes(LocalDateTime.now()));
        verify(solicitudRepository, never()).eliminarAnterioresA(any());
    }

    @Test
    @DisplayName("Si la depuración falla no revienta la tarea programada")
    void retencionNoPropagaErrores() {
        RetencionDatosJob job = new RetencionDatosJob(servicio);
        ReflectionTestUtils.setField(job, "mesesSolicitudes", 12);
        org.mockito.Mockito.when(solicitudRepository.eliminarAnterioresA(any())).thenThrow(new RuntimeException("BD caída"));

        assertEquals(0, job.depurarSolicitudes(LocalDateTime.now()));
    }
}
