package com.constructora_backend.service;

import com.constructora_backend.dto.request.CasaModeloRequestDTO;
import com.constructora_backend.dto.response.CasaModeloResponseDTO;
import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.mapper.CasaModeloMapper;
import com.constructora_backend.repository.CasaModeloRepository;
import com.constructora_backend.repository.ProyectoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasaModeloServiceTest {

    @Mock
    private CasaModeloRepository casaModeloRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private CasaModeloMapper casaModeloMapper;

    @InjectMocks
    private CasaModeloService casaModeloService;

    private CasaModeloRequestDTO requestDTO;
    private Proyecto proyecto;
    private CasaModelo casaModelo;
    private CasaModeloResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        proyecto = new Proyecto();
        proyecto.setId(1L);

        requestDTO = new CasaModeloRequestDTO();
        requestDTO.setProyectoId(1L);
        requestDTO.setNombre("Modelo Roble");
        requestDTO.setDescripcion("Casa de 2 pisos con jardín");
        requestDTO.setAreaContruidaM2(new BigDecimal("120.50"));
        requestDTO.setNumeroHabitaciones(3);
        requestDTO.setNumeroBanos(2);

        casaModelo = new CasaModelo();
        casaModelo.setId(5L);
        casaModelo.setProyecto(proyecto);
        casaModelo.setNombre("Modelo Roble");
        casaModelo.setAreaConstruidaM2(new BigDecimal("120.50"));

        responseDTO = new CasaModeloResponseDTO();
        responseDTO.setId(5L);
        responseDTO.setProyectoId(1L);
        responseDTO.setNombre("Modelo Roble");
        responseDTO.setAreaConstruidaM2(new BigDecimal("120.50"));
    }

    @Test
    void guardarCasaModeloExitosamente() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(casaModeloRepository.existsByProyectoIdAndNombre(1L, "Modelo Roble")).thenReturn(false);
        when(casaModeloMapper.toEntity(requestDTO, proyecto)).thenReturn(casaModelo);
        when(casaModeloRepository.save(any(CasaModelo.class))).thenReturn(casaModelo);
        when(casaModeloMapper.toDTO(casaModelo)).thenReturn(responseDTO);

        CasaModeloResponseDTO resultado = casaModeloService.guardar(requestDTO);

        assertNotNull(resultado);
        assertEquals(5L, resultado.getId());
        assertEquals("Modelo Roble", resultado.getNombre());
        verify(casaModeloRepository, times(1)).save(any(CasaModelo.class));
    }

    @Test
    void guardarCasaModeloConNombreDuplicadoLanzaExcepcion() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(casaModeloRepository.existsByProyectoIdAndNombre(1L, "Modelo Roble")).thenReturn(true);

        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> casaModeloService.guardar(requestDTO));

        assertTrue(excepcion.getMessage().contains("Ya existe una casa modelo"));
        verify(casaModeloRepository, never()).save(any());
    }

    @Test
    void eliminarCasaModeloInexistenteLanzaExcepcion() {
        when(casaModeloRepository.existsById(99L)).thenReturn(false);

        RuntimeException excepcion = assertThrows(RuntimeException.class,
                () -> casaModeloService.eliminar(99L));

        assertTrue(excepcion.getMessage().contains("Casa modelo no encontrada"));
        verify(casaModeloRepository, never()).deleteById(any());
    }
}