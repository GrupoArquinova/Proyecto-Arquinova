package com.constructora_backend.service;

import com.constructora_backend.dto.request.EtapaRequestDTO;
import com.constructora_backend.dto.response.EtapaResponseDTO;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.mapper.EtapaMapper;
import com.constructora_backend.repository.EtapaRepository;
import com.constructora_backend.repository.ProyectoRepository;
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
class EtapaServiceTest {

    @Mock
    private EtapaRepository etapaRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private EtapaMapper etapaMapper;

    @InjectMocks
    private EtapaService etapaService;

    private EtapaRequestDTO requestDTO;
    private Proyecto proyecto;
    private Etapa etapa;
    private EtapaResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        proyecto = new Proyecto();
        proyecto.setId(1L);
        proyecto.setNombre("Residencial Palmas");

        requestDTO = new EtapaRequestDTO();
        requestDTO.setProyectoId(1L);
        requestDTO.setNombre("Etapa 1");
        requestDTO.setOrden((short) 1);

        etapa = new Etapa();
        etapa.setId(10L);
        etapa.setProyecto(proyecto);
        etapa.setNombre("Etapa 1");
        etapa.setOrden((short) 1);

        responseDTO = new EtapaResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setProyectoId(1L);
        responseDTO.setNombre("Etapa 1");
        responseDTO.setOrden((short) 1);
    }

    @Test
    void guardarEtapaExitosamente() {
        when(etapaRepository.existsByProyectoIdAndNombre(1L, "Etapa 1")).thenReturn(false);
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(etapaMapper.toEntity(requestDTO, proyecto)).thenReturn(etapa);
        when(etapaRepository.save(any(Etapa.class))).thenReturn(etapa);
        when(etapaMapper.toDTO(etapa)).thenReturn(responseDTO);

        EtapaResponseDTO resultado = etapaService.guardar(requestDTO);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Etapa 1", resultado.getNombre());
        verify(etapaRepository, times(1)).save(any(Etapa.class));
    }

    @Test
    void guardarEtapaConNombreDuplicadoLanzaExcepcion() {
        when(etapaRepository.existsByProyectoIdAndNombre(1L, "Etapa 1")).thenReturn(true);

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> etapaService.guardar(requestDTO));
        assertTrue(excepcion.getMessage().contains("Ya existe una etapa con el nombre"));
        verify(etapaRepository, never()).save(any());
    }
}