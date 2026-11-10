package com.constructora_backend.service;

import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.entity.EstadoSolicitud;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.SolicitudContacto;
import com.constructora_backend.enums.TipoEventoAnalitica;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Al guardar una solicitud el servidor cuenta FORMULARIO_ENVIADO en la analítica; el navegador no lo manda. */
@ExtendWith(MockitoExtension.class)
class SolicitudAnaliticaTest {

    @Mock private SolicitudContactoRepository solicitudRepository;
    @Mock private EstadoSolicitudRepository estadoSolicitudRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private CasaModeloRepository casaModeloRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;
    @Mock private CaptchaService captchaService;
    @Mock private AnaliticaService analiticaService;
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
    }

    private SolicitudContactoPublicDTO pedido(Long proyectoId) {
        SolicitudContactoPublicDTO dto = new SolicitudContactoPublicDTO();
        dto.setNombre("Ana");
        dto.setCorreo("ana@test.com");
        dto.setMensaje("Hola");
        dto.setConsentimientoDatos(true);
        dto.setProyectoId(proyectoId);
        return dto;
    }

    @Test
    @DisplayName("Una solicitud guardada cuenta FORMULARIO_ENVIADO con el proyecto consultado")
    void cuentaElFormulario() {
        Proyecto proyecto = new Proyecto();
        proyecto.setId(6L);
        proyecto.setNombre("El Encanto");
        org.mockito.Mockito.when(proyectoRepository.findById(6L)).thenReturn(Optional.of(proyecto));

        servicio.crearPublica(pedido(6L), "203.0.113.9", "UA");

        verify(analiticaService).registrarDelServidor(TipoEventoAnalitica.FORMULARIO_ENVIADO, 6L);
    }

    @Test
    @DisplayName("Sin proyecto se cuenta igual, sin proyecto")
    void sinProyecto() {
        servicio.crearPublica(pedido(null), "203.0.113.9", "UA");

        verify(analiticaService).registrarDelServidor(TipoEventoAnalitica.FORMULARIO_ENVIADO, null);
    }

    @Test
    @DisplayName("Si la analítica falla, la solicitud se guarda y se responde igual")
    void unFalloNoAfectaLaSolicitud() {
        doThrow(new RuntimeException("BD caída")).when(analiticaService).registrarDelServidor(any(), any());

        var respuesta = servicio.crearPublica(pedido(null), "203.0.113.9", "UA");

        assertEquals(77L, respuesta.getId());
        verify(solicitudRepository).save(any(SolicitudContacto.class));
    }

    @Test
    @DisplayName("Una solicitud descartada por el campo trampa (robot) no cuenta como formulario enviado")
    void elRobotNoCuenta() {
        SolicitudContactoPublicDTO robot = pedido(null);
        robot.setSitioWeb("http://spam.test");

        servicio.crearPublica(robot, "203.0.113.9", "UA");

        verify(analiticaService, never()).registrarDelServidor(any(), any());
    }
}
