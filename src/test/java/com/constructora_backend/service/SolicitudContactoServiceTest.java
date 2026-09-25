package com.constructora_backend.service;

import com.constructora_backend.dto.SolicitudContactoAtencionDTO;
import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.entity.*;
import com.constructora_backend.mapper.SolicitudContactoMapper;
import com.constructora_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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