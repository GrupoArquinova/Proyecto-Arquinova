package com.constructora_backend.service;

import com.constructora_backend.dto.request.ZonaComunImagenRequestDTO;
import com.constructora_backend.dto.response.ZonaComunImagenResponseDTO;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.entity.ZonaComunImagen;
import com.constructora_backend.mapper.ZonaComunImagenMapper;
import com.constructora_backend.repository.ZonaComunImagenRepository;
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
class ZonaComunImagenServiceTest {

    @Mock
    private ZonaComunImagenRepository imagenRepository;

    @Mock
    private ZonaComunRepository zonaComunRepository;

    @Mock
    private ZonaComunImagenMapper imagenMapper;

    @InjectMocks
    private ZonaComunImagenService imagenService;

    private ZonaComunImagenRequestDTO requestDTO;
    private ZonaComun zonaComun;
    private ZonaComunImagen imagen;
    private ZonaComunImagenResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        zonaComun = new ZonaComun();
        zonaComun.setId(10L);
        zonaComun.setNombre("Piscina Climatizada");

        requestDTO = new ZonaComunImagenRequestDTO();
        requestDTO.setZonaComunId(10L);
        requestDTO.setImagenUrl("https://cdn.constructora.com/zonas/piscina1.jpg");
        requestDTO.setTitulo("Vista Panorámica");
        requestDTO.setOrden((short) 1);
        requestDTO.setEsPrincipal(true);

        imagen = new ZonaComunImagen();
        imagen.setId(100L);
        imagen.setZonaComun(zonaComun);
        imagen.setImagenUrl("https://cdn.constructora.com/zonas/piscina1.jpg");
        imagen.setEsPrincipal(true);

        responseDTO = new ZonaComunImagenResponseDTO();
        responseDTO.setId(100L);
        responseDTO.setZonaComunId(10L);
        responseDTO.setImagenUrl("https://cdn.constructora.com/zonas/piscina1.jpg");
        responseDTO.setEsPrincipal(true);
    }

    @Test
    void guardarImagenExitosamente() {
        when(zonaComunRepository.findById(10L)).thenReturn(Optional.of(zonaComun));
        when(imagenMapper.toEntity(requestDTO, zonaComun)).thenReturn(imagen);
        when(imagenRepository.save(any(ZonaComunImagen.class))).thenReturn(imagen);
        when(imagenMapper.toDTO(imagen)).thenReturn(responseDTO);

        ZonaComunImagenResponseDTO resultado = imagenService.guardar(requestDTO);

        assertNotNull(resultado);
        assertEquals(100L, resultado.getId());
        assertTrue(resultado.getEsPrincipal());
        verify(imagenRepository, times(1)).desmarcarPrincipalesDeZonaComun(10L);
        verify(imagenRepository, times(1)).save(any(ZonaComunImagen.class));
    }

    @Test
    void guardarImagenConZonaComunInexistenteLanzaExcepcion() {
        when(zonaComunRepository.findById(10L)).thenReturn(Optional.empty());

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> imagenService.guardar(requestDTO));
        assertTrue(excepcion.getMessage().contains("Zona común no encontrada"));
        verify(imagenRepository, never()).save(any());
    }
}