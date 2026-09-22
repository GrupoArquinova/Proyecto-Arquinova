package com.constructora_backend.service;

import com.constructora_backend.dto.request.EstadoSolicitudRequestDTO;
import com.constructora_backend.dto.response.EstadoSolicitudResponseDTO;
import com.constructora_backend.entity.EstadoSolicitud;
import com.constructora_backend.mapper.EstadoSolicitudMapper;
import com.constructora_backend.repository.EstadoSolicitudRepository;
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
class EstadoSolicitudServiceTest {

    @Mock
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @Mock
    private EstadoSolicitudMapper estadoSolicitudMapper;

    @InjectMocks
    private EstadoSolicitudService estadoSolicitudService;

    private EstadoSolicitudRequestDTO requestDTO;
    private EstadoSolicitud estado;
    private EstadoSolicitudResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new EstadoSolicitudRequestDTO();
        requestDTO.setNombre("NUEVA");
        requestDTO.setDescripcion("Solicitud recién recibida");
        requestDTO.setOrden(1);
        requestDTO.setActivo(true);

        estado = new EstadoSolicitud();
        estado.setId(1);
        estado.setNombre("NUEVA");
        estado.setDescripcion("Solicitud recién recibida");
        estado.setOrden(1);
        estado.setActivo(true);

        responseDTO = new EstadoSolicitudResponseDTO();
        responseDTO.setId(1);
        responseDTO.setNombre("NUEVA");
        responseDTO.setDescripcion("Solicitud recién recibida");
        responseDTO.setOrden(1);
        responseDTO.setActivo(true);
    }

    @Test
    void guardarEstadoSolicitudExitosamente() {
        when(estadoSolicitudRepository.existsByNombre("NUEVA")).thenReturn(false);
        when(estadoSolicitudMapper.toEntity(requestDTO)).thenReturn(estado);
        when(estadoSolicitudRepository.save(any(EstadoSolicitud.class))).thenReturn(estado);
        when(estadoSolicitudMapper.toDTO(estado)).thenReturn(responseDTO);

        EstadoSolicitudResponseDTO resultado = estadoSolicitudService.guardar(requestDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("NUEVA", resultado.getNombre());
        verify(estadoSolicitudRepository, times(1)).save(any(EstadoSolicitud.class));
    }

    @Test
    void guardarEstadoSolicitudConNombreDuplicadoLanzaExcepcion() {
        when(estadoSolicitudRepository.existsByNombre("NUEVA")).thenReturn(true);

        com.constructora_backend.exception.DuplicateResourceException excepcion = assertThrows(com.constructora_backend.exception.DuplicateResourceException.class,
                () -> estadoSolicitudService.guardar(requestDTO));

        assertTrue(excepcion.getMessage().contains("Ya existe un estado de solicitud con el nombre"));
        verify(estadoSolicitudRepository, never()).save(any());
    }

    @Test
    void actualizarEstadoSolicitudExitosamente() {
        when(estadoSolicitudRepository.findById(1)).thenReturn(Optional.of(estado));
        when(estadoSolicitudRepository.existsByNombreAndIdNot("NUEVA", 1)).thenReturn(false);
        when(estadoSolicitudRepository.save(estado)).thenReturn(estado);
        when(estadoSolicitudMapper.toDTO(estado)).thenReturn(responseDTO);

        EstadoSolicitudResponseDTO resultado = estadoSolicitudService.actualizar(1, requestDTO);

        assertNotNull(resultado);
        assertEquals("NUEVA", resultado.getNombre());
        verify(estadoSolicitudRepository, times(1)).save(estado);
    }
}