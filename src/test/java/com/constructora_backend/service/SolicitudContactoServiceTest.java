package com.constructora_backend.service;

import com.constructora_backend.dto.SolicitudContactoAtencionDTO;
import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.entity.*;
import com.constructora_backend.mapper.SolicitudContactoMapper;
import com.constructora_backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudContactoServiceTest {

    @Mock
    private SolicitudContactoRepository solicitudRepository;

    @Mock
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SolicitudContactoMapper solicitudMapper;

    @InjectMocks
    private SolicitudContactoService solicitudService;

    private SolicitudContactoPublicDTO publicDTO;
    private SolicitudContactoAtencionDTO atencionDTO;
    private EstadoSolicitud estadoNueva;
    private EstadoSolicitud estadoAtendida;
    private SolicitudContacto solicitud;
    private SolicitudContactoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        publicDTO = new SolicitudContactoPublicDTO();
        publicDTO.setNombre("Carlos Pérez");
        publicDTO.setCorreo("carlos@example.com");
        publicDTO.setTelefono("+57 3001234567");
        publicDTO.setMensaje("Interesado en proyecto de prueba");
        publicDTO.setConsentimientoDatos(true);

        atencionDTO = new SolicitudContactoAtencionDTO();
        atencionDTO.setEstadoId(2);
        atencionDTO.setObservacionesInternas("Cliente contactado por teléfono");

        estadoNueva = new EstadoSolicitud();
        estadoNueva.setId(1);
        estadoNueva.setNombre("NUEVA");

        estadoAtendida = new EstadoSolicitud();
        estadoAtendida.setId(2);
        estadoAtendida.setNombre("CONTACTADA");

        solicitud = new SolicitudContacto();
        solicitud.setId(10L);
        solicitud.setNombre("Carlos Pérez");
        solicitud.setCorreo("carlos@example.com");
        solicitud.setEstado(estadoNueva);

        responseDTO = new SolicitudContactoResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setNombre("Carlos Pérez");
        responseDTO.setEstadoId(1);
        responseDTO.setEstadoNombre("NUEVA");
    }

    @Test
    void crearPublicaExitosamente() {
        when(estadoSolicitudRepository.findById(1)).thenReturn(Optional.of(estadoNueva));
        when(solicitudMapper.toEntity(publicDTO, estadoNueva, null, null)).thenReturn(solicitud);
        when(solicitudRepository.save(any(SolicitudContacto.class))).thenReturn(solicitud);
        when(solicitudMapper.toDTO(solicitud)).thenReturn(responseDTO);

        SolicitudContactoResponseDTO resultado = solicitudService.crearPublica(publicDTO);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Carlos Pérez", resultado.getNombre());
        verify(solicitudRepository, times(1)).save(any(SolicitudContacto.class));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void iniciarSesion(String correo) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(correo, null, List.of()));
    }

    private Usuario usuario(Long id, String nombre, String correo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombreCompleto(nombre);
        u.setCorreo(correo);
        return u;
    }

    @Test
    void atenderSolicitudRegistraALaPersonaConLaSesionIniciada() {
        Usuario maria = usuario(7L, "Maria Gomez", "maria@arquinova.com");
        iniciarSesion("maria@arquinova.com");
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(estadoSolicitudRepository.findById(2)).thenReturn(Optional.of(estadoAtendida));
        when(usuarioRepository.findByCorreo("maria@arquinova.com")).thenReturn(Optional.of(maria));
        when(solicitudRepository.save(solicitud)).thenReturn(solicitud);
        when(solicitudMapper.toDTO(solicitud)).thenReturn(responseDTO);

        solicitudService.atenderSolicitud(10L, atencionDTO);

        assertSame(maria, solicitud.getAtendidaPor());
        assertNotNull(solicitud.getAtendidaEn());
    }

    @Test
    void atenderSolicitudIgnoraElUsuarioDelCuerpoCuandoHaySesion() {
        Usuario maria = usuario(7L, "Maria Gomez", "maria@arquinova.com");
        iniciarSesion("maria@arquinova.com");
        atencionDTO.setAtendidaPorId(99L);
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(estadoSolicitudRepository.findById(2)).thenReturn(Optional.of(estadoAtendida));
        when(usuarioRepository.findByCorreo("maria@arquinova.com")).thenReturn(Optional.of(maria));
        when(solicitudRepository.save(solicitud)).thenReturn(solicitud);
        when(solicitudMapper.toDTO(solicitud)).thenReturn(responseDTO);

        solicitudService.atenderSolicitud(10L, atencionDTO);

        assertSame(maria, solicitud.getAtendidaPor());
        verify(usuarioRepository, never()).findById(99L);
    }

    @Test
    void atenderSolicitudCambiaLaPersonaYLaFechaEnCadaAtencion() {
        Usuario antes = usuario(3L, "Pedro Ruiz", "pedro@arquinova.com");
        Usuario ahora = usuario(7L, "Maria Gomez", "maria@arquinova.com");
        solicitud.setAtendidaPor(antes);
        solicitud.setAtendidaEn(java.time.LocalDateTime.of(2026, 10, 2, 15, 21));
        iniciarSesion("maria@arquinova.com");
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(estadoSolicitudRepository.findById(2)).thenReturn(Optional.of(estadoAtendida));
        when(usuarioRepository.findByCorreo("maria@arquinova.com")).thenReturn(Optional.of(ahora));
        when(solicitudRepository.save(solicitud)).thenReturn(solicitud);
        when(solicitudMapper.toDTO(solicitud)).thenReturn(responseDTO);

        solicitudService.atenderSolicitud(10L, atencionDTO);

        assertSame(ahora, solicitud.getAtendidaPor());
        assertTrue(solicitud.getAtendidaEn().isAfter(java.time.LocalDateTime.of(2026, 10, 2, 15, 21)));
    }

    @Test
    void atenderSolicitudSinSesionUsaElIdDelCuerpo() {
        Usuario pedro = usuario(3L, "Pedro Ruiz", "pedro@arquinova.com");
        atencionDTO.setAtendidaPorId(3L);
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(estadoSolicitudRepository.findById(2)).thenReturn(Optional.of(estadoAtendida));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(pedro));
        when(solicitudRepository.save(solicitud)).thenReturn(solicitud);
        when(solicitudMapper.toDTO(solicitud)).thenReturn(responseDTO);

        solicitudService.atenderSolicitud(10L, atencionDTO);

        assertSame(pedro, solicitud.getAtendidaPor());
    }

    @Test
    void atenderSolicitudExitosamente() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(estadoSolicitudRepository.findById(2)).thenReturn(Optional.of(estadoAtendida));
        when(solicitudRepository.save(solicitud)).thenReturn(solicitud);
        when(solicitudMapper.toDTO(solicitud)).thenReturn(responseDTO);

        SolicitudContactoResponseDTO resultado = solicitudService.atenderSolicitud(10L, atencionDTO);

        assertNotNull(resultado);
        verify(solicitudRepository, times(1)).save(solicitud);
    }
}