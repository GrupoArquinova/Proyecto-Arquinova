package com.constructora_backend.service;

import com.constructora_backend.dto.request.EstadoLoteRequestDTO;
import com.constructora_backend.dto.response.EstadoLoteResponseDTO;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.mapper.EstadoLoteMapper;
import com.constructora_backend.repository.EstadoLoteRepository;
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
class EstadoLoteServiceTest {

    @Mock
    private EstadoLoteRepository estadoLoteRepository;

    @Mock
    private EstadoLoteMapper estadoLoteMapper;

    @InjectMocks
    private EstadoLoteService estadoLoteService;

    private EstadoLoteRequestDTO requestDTO;
    private EstadoLote estado;
    private EstadoLoteResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new EstadoLoteRequestDTO();
        requestDTO.setNombre("Disponible");
        requestDTO.setDescripcion("Lote listo para la venta");
        requestDTO.setOrden(1);
        requestDTO.setActivo(true);

        estado = new EstadoLote();
        estado.setId(1);
        estado.setNombre("Disponible");
        estado.setDescripcion("Lote listo para la venta");
        estado.setOrden(1);
        estado.setActivo(true);

        responseDTO = new EstadoLoteResponseDTO();
        responseDTO.setId(1);
        responseDTO.setNombre("Disponible");
        responseDTO.setDescripcion("Lote listo para la venta");
        responseDTO.setOrden(1);
        responseDTO.setActivo(true);
    }

    @Test
    void guardarEstadoLoteExitosamente() {
        when(estadoLoteRepository.existsByNombre("Disponible")).thenReturn(false);
        when(estadoLoteMapper.toEntity(requestDTO)).thenReturn(estado);
        when(estadoLoteRepository.save(any(EstadoLote.class))).thenReturn(estado);
        when(estadoLoteMapper.toDTO(estado)).thenReturn(responseDTO);

        EstadoLoteResponseDTO resultado = estadoLoteService.guardar(requestDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("Disponible", resultado.getNombre());
        verify(estadoLoteRepository, times(1)).save(any(EstadoLote.class));
    }

    @Test
    void guardarEstadoLoteConNombreDuplicadoLanzaExcepcion() {
        when(estadoLoteRepository.existsByNombre("Disponible")).thenReturn(true);

        com.constructora_backend.exception.DuplicateResourceException excepcion = assertThrows(com.constructora_backend.exception.DuplicateResourceException.class,
                () -> estadoLoteService.guardar(requestDTO));

        assertTrue(excepcion.getMessage().contains("Ya existe un estado de lote con el nombre"));
        verify(estadoLoteRepository, never()).save(any());
    }

    @Test
    void actualizarEstadoLoteExitosamente() {
        when(estadoLoteRepository.findById(1)).thenReturn(Optional.of(estado));
        when(estadoLoteRepository.existsByNombreAndIdNot("Disponible", 1)).thenReturn(false);
        when(estadoLoteRepository.save(estado)).thenReturn(estado);
        when(estadoLoteMapper.toDTO(estado)).thenReturn(responseDTO);

        EstadoLoteResponseDTO resultado = estadoLoteService.actualizar(1, requestDTO);

        assertNotNull(resultado);
        assertEquals("Disponible", resultado.getNombre());
        verify(estadoLoteRepository, times(1)).save(estado);
    }
}