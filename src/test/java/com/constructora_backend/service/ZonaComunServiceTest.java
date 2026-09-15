package com.constructora_backend.service;

import com.constructora_backend.dto.request.ZonaComunRequestDTO;
import com.constructora_backend.dto.response.ZonaComunResponseDTO;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.mapper.ZonaComunMapper;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.ZonaComunRepository;
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
class ZonaComunServiceTest {

    @Mock
    private ZonaComunRepository zonaComunRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private ZonaComunMapper zonaComunMapper;

    @InjectMocks
    private ZonaComunService zonaComunService;

    private ZonaComunRequestDTO requestDTO;
    private Proyecto proyecto;
    private ZonaComun zonaComun;
    private ZonaComunResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        proyecto = new Proyecto();
        proyecto.setId(1L);

        requestDTO = new ZonaComunRequestDTO();
        requestDTO.setProyectoId(1L);
        requestDTO.setNombre("Gimnasio");
        requestDTO.setDescripcion("Totalmente equipado");

        zonaComun = new ZonaComun();
        zonaComun.setId(10L);
        zonaComun.setProyecto(proyecto);
        zonaComun.setNombre("Gimnasio");

        responseDTO = new ZonaComunResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setProyectoId(1L);
        responseDTO.setNombre("Gimnasio");
    }

    @Test
    void guardarZonaComunExitosamente() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(zonaComunRepository.existsByProyectoIdAndNombre(1L, "Gimnasio")).thenReturn(false);
        when(zonaComunMapper.toEntity(requestDTO, proyecto)).thenReturn(zonaComun);
        when(zonaComunRepository.save(any(ZonaComun.class))).thenReturn(zonaComun);
        when(zonaComunMapper.toDTO(zonaComun)).thenReturn(responseDTO);

        ZonaComunResponseDTO resultado = zonaComunService.guardar(requestDTO);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Gimnasio", resultado.getNombre());
        verify(zonaComunRepository, times(1)).save(any(ZonaComun.class));
    }

    @Test
    void guardarZonaComunConNombreDuplicadoLanzaExcepcion() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(zonaComunRepository.existsByProyectoIdAndNombre(1L, "Gimnasio")).thenReturn(true);

        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> zonaComunService.guardar(requestDTO));

        assertTrue(excepcion.getMessage().contains("Ya existe una zona común"));
        verify(zonaComunRepository, never()).save(any());
    }
}