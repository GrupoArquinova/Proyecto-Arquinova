package com.constructora_backend.service;

import com.constructora_backend.dto.request.UbicacionRequestDTO;
import com.constructora_backend.dto.response.UbicacionResponseDTO;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.exception.DuplicateResourceException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.UbicacionMapper;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.UbicacionRepository;
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
class UbicacionServiceTest {

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private UbicacionMapper ubicacionMapper;

    @InjectMocks
    private UbicacionService ubicacionService;

    private UbicacionRequestDTO requestDTO;
    private Proyecto proyecto;
    private Ubicacion ubicacion;
    private UbicacionResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        proyecto = new Proyecto();
        proyecto.setId(1L);
        proyecto.setNombre("Torres del Norte");

        requestDTO = new UbicacionRequestDTO();
        requestDTO.setProyectoId(1L);
        requestDTO.setDireccion("Calle 100 #15-20");
        requestDTO.setCiudad("Bogotá");
        requestDTO.setLatitud(new BigDecimal("4.6097100"));
        requestDTO.setLongitud(new BigDecimal("-74.0817500"));

        ubicacion = new Ubicacion();
        ubicacion.setId(5L);
        ubicacion.setProyecto(proyecto);
        ubicacion.setDireccion("Calle 100 #15-20");
        ubicacion.setCiudad("Bogotá");

        responseDTO = new UbicacionResponseDTO();
        responseDTO.setId(5L);
        responseDTO.setProyectoId(1L);
        responseDTO.setDireccion("Calle 100 #15-20");
        responseDTO.setCiudad("Bogotá");
    }

    @Test
    void guardarUbicacionExitosamente() {
        when(ubicacionRepository.existsByProyectoId(1L)).thenReturn(false);
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(ubicacionMapper.toEntity(requestDTO, proyecto)).thenReturn(ubicacion);
        when(ubicacionRepository.save(any(Ubicacion.class))).thenReturn(ubicacion);
        when(ubicacionMapper.toDto(ubicacion)).thenReturn(responseDTO);

        UbicacionResponseDTO resultado = ubicacionService.guardar(requestDTO);

        assertNotNull(resultado);
        assertEquals(5L, resultado.getId());
        assertEquals("Bogotá", resultado.getCiudad());
        verify(ubicacionRepository, times(1)).save(any(Ubicacion.class));
    }

    @Test
    void guardarUbicacionConProyectoYaExistenteLanzaExcepcion() {
        when(ubicacionRepository.existsByProyectoId(1L)).thenReturn(true);

        DuplicateResourceException excepcion = assertThrows(DuplicateResourceException.class, () -> ubicacionService.guardar(requestDTO));
        assertTrue(excepcion.getMessage().contains("ya tiene una ubicación registrada"));
        verify(ubicacionRepository, never()).save(any());
    }

    @Test
    void guardarUbicacionConProyectoNoExistenteLanzaExcepcion() {
        when(ubicacionRepository.existsByProyectoId(1L)).thenReturn(false);
        when(proyectoRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException excepcion = assertThrows(ResourceNotFoundException.class, () -> ubicacionService.guardar(requestDTO));
        assertTrue(excepcion.getMessage().contains("Proyecto no encontrado"));
        verify(ubicacionRepository, never()).save(any());
    }
}