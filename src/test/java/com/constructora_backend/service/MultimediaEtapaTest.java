package com.constructora_backend.service;

import com.constructora_backend.dto.request.MultimediaRequestDTO;
import com.constructora_backend.dto.response.MultimediaResponseDTO;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Multimedia;
import com.constructora_backend.enums.TipoMultimedia;
import com.constructora_backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultimediaEtapaTest {

    @Mock private MultimediaRepository multimediaRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private ZonaComunRepository zonaComunRepository;
    @Mock private CasaModeloRepository casaModeloRepository;
    @Mock private EtapaRepository etapaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private MultimediaService servicio;

    private MultimediaRequestDTO pedido() {
        MultimediaRequestDTO dto = new MultimediaRequestDTO();
        dto.setTipo(TipoMultimedia.IMAGEN);
        dto.setUrl("https://img.test/etapa.png");
        return dto;
    }

    @Test
    void guardaUnaImagenAsociadaALaEtapa() {
        Etapa etapa = new Etapa();
        etapa.setId(5L);
        etapa.setNombre("Etapa A - Guayacán");
        when(etapaRepository.findById(5L)).thenReturn(Optional.of(etapa));
        when(multimediaRepository.save(any(Multimedia.class))).thenAnswer(i -> i.getArgument(0));

        MultimediaRequestDTO dto = pedido();
        dto.setEtapaId(5L);
        MultimediaResponseDTO respuesta = servicio.guardar(dto);

        assertEquals(5L, respuesta.getEtapaId());
        assertEquals("Etapa A - Guayacán", respuesta.getEtapaNombre());
    }

    @Test
    void noPermiteAsociarlaAUnaEtapaYAOtraEntidadALaVez() {
        MultimediaRequestDTO dto = pedido();
        dto.setEtapaId(5L);
        dto.setProyectoId(1L);
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(dto));
        verify(multimediaRepository, never()).save(any());
    }

    @Test
    void unaEtapaInexistenteDaError() {
        when(etapaRepository.findById(99L)).thenReturn(Optional.empty());
        MultimediaRequestDTO dto = pedido();
        dto.setEtapaId(99L);
        assertThrows(EntityNotFoundException.class, () -> servicio.guardar(dto));
    }

    @Test
    void listaSoloLasPublicadasDeLaEtapa() {
        when(multimediaRepository.findByEtapa_IdAndPublicadoTrueAndActivoTrueOrderByOrdenAsc(5L)).thenReturn(List.of());
        assertTrue(servicio.listarPorEntidad("etapa", 5L, true).isEmpty());
        verify(multimediaRepository).findByEtapa_IdAndPublicadoTrueAndActivoTrueOrderByOrdenAsc(5L);
    }

    /** Hibernate ejecuta esta validación de la entidad al guardar; con repositorios simulados no se ve, por eso se prueba aparte. */
    @Test
    void laEntidadAceptaUnaEtapaComoUnicoPadre() {
        Multimedia conEtapa = Multimedia.builder().etapa(new Etapa()).tipo(TipoMultimedia.IMAGEN).url("https://img.test/a.png").build();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(conEtapa, "validarUnSoloPadre");

        Multimedia sinPadre = Multimedia.builder().tipo(TipoMultimedia.IMAGEN).url("https://img.test/a.png").build();
        assertThrows(IllegalArgumentException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(sinPadre, "validarUnSoloPadre"));
    }
}
