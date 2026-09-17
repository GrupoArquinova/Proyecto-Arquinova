package com.constructora_backend.service;

import com.constructora_backend.dto.request.LoteRequestDTO;
import com.constructora_backend.dto.response.LoteResponseDTO;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.mapper.LoteMapper;
import com.constructora_backend.repository.EstadoLoteRepository;
import com.constructora_backend.repository.EtapaRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.UsuarioRepository;
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
class LoteServiceTest {

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private EtapaRepository etapaRepository;

    @Mock
    private EstadoLoteRepository estadoLoteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LoteMapper loteMapper;

    @InjectMocks
    private LoteService loteService;

    private LoteRequestDTO requestDTO;
    private Etapa etapa;
    private EstadoLote estado;
    private Lote lote;
    private LoteResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        etapa = new Etapa();
        etapa.setId(2L);

        estado = new EstadoLote();
        estado.setId(1);

        requestDTO = new LoteRequestDTO();
        requestDTO.setEtapaId(2L);
        requestDTO.setEstadoId(1);
        requestDTO.setCodigo("LOTE-A12");
        requestDTO.setNombre("Lote Esquinero A12");
        requestDTO.setAreaM2(new BigDecimal("150.00"));

        lote = new Lote();
        lote.setId(50L);
        lote.setEtapa(etapa);
        lote.setEstado(estado);
        lote.setCodigo("LOTE-A12");
        lote.setAreaM2(new BigDecimal("150.00"));

        responseDTO = new LoteResponseDTO();
        responseDTO.setId(50L);
        responseDTO.setEtapaId(2L);
        responseDTO.setEstadoId(1);
        responseDTO.setCodigo("LOTE-A12");
        responseDTO.setAreaM2(new BigDecimal("150.00"));
        responseDTO.setPrecio(new BigDecimal("250000000.00"));
    }

    @Test
    void guardarLoteExitosamente() {
        when(etapaRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(estadoLoteRepository.findById(1)).thenReturn(Optional.of(estado));
        when(loteRepository.existsByEtapaIdAndCodigo(2L, "LOTE-A12")).thenReturn(false);
        when(loteMapper.toEntity(eq(requestDTO), eq(etapa), eq(estado), any())).thenReturn(lote);
        when(loteRepository.save(any(Lote.class))).thenReturn(lote);
        when(loteMapper.toDTO(lote)).thenReturn(responseDTO);

        LoteResponseDTO resultado = loteService.guardar(requestDTO);

        assertNotNull(resultado);
        assertEquals(50L, resultado.getId());
        assertEquals("LOTE-A12", resultado.getCodigo());
        assertEquals(new BigDecimal("250000000.00"), resultado.getPrecio());
        verify(loteRepository, times(1)).save(any(Lote.class));
    }

    @Test
    void guardarLoteConCodigoDuplicadoEnEtapaLanzaExcepcion() {
        when(etapaRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(estadoLoteRepository.findById(1)).thenReturn(Optional.of(estado));
        when(loteRepository.existsByEtapaIdAndCodigo(2L, "LOTE-A12")).thenReturn(true);

        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> loteService.guardar(requestDTO));

        assertTrue(excepcion.getMessage().contains("Ya existe un lote con el código"));
        verify(loteRepository, never()).save(any());
    }
}