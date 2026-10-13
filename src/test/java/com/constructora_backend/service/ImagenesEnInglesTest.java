package com.constructora_backend.service;

import com.constructora_backend.dto.request.MultimediaRequestDTO;
import com.constructora_backend.dto.request.UbicacionRequestDTO;
import com.constructora_backend.dto.response.MultimediaResponseDTO;
import com.constructora_backend.dto.response.UbicacionResponseDTO;
import com.constructora_backend.entity.Multimedia;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.enums.TipoMultimedia;
import com.constructora_backend.mapper.UbicacionMapper;
import com.constructora_backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Imágenes con texto dentro (diapositivas, mapas, planos): cada una puede tener su versión en inglés. */
@ExtendWith(MockitoExtension.class)
class ImagenesEnInglesTest {

    @Mock private MultimediaRepository multimediaRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private ZonaComunRepository zonaComunRepository;
    @Mock private CasaModeloRepository casaModeloRepository;
    @Mock private EtapaRepository etapaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private MultimediaService servicio;

    private MultimediaRequestDTO pedido(String urlEn) {
        MultimediaRequestDTO dto = new MultimediaRequestDTO();
        dto.setTipo(TipoMultimedia.IMAGEN);
        dto.setProyectoId(1L);
        dto.setUrl("https://img.test/plano-es.jpg");
        dto.setUrlEn(urlEn);
        return dto;
    }

    private Multimedia existente(String urlEn) {
        return Multimedia.builder().id(7L).tipo(TipoMultimedia.IMAGEN).url("https://img.test/plano-es.jpg").urlEn(urlEn).build();
    }

    @Test
    void guardaLaVersionEnInglesLimpiandoLosEspacios() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(new Proyecto()));
        when(multimediaRepository.save(any(Multimedia.class))).thenAnswer(i -> i.getArgument(0));

        MultimediaResponseDTO respuesta = servicio.guardar(pedido("  https://img.test/plano-en.jpg  "));

        assertEquals("https://img.test/plano-en.jpg", respuesta.getUrlEn());
        assertEquals("https://img.test/plano-es.jpg", respuesta.getUrl());
    }

    @Test
    void sinVersionEnInglesQuedaVacia() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(new Proyecto()));
        when(multimediaRepository.save(any(Multimedia.class))).thenAnswer(i -> i.getArgument(0));

        assertNull(servicio.guardar(pedido(null)).getUrlEn());
        assertNull(servicio.guardar(pedido("   ")).getUrlEn());
    }

    @Test
    void alActualizarSinEnviarLaVersionEnInglesSeConserva() {
        Multimedia recurso = existente("https://img.test/plano-en.jpg");
        when(multimediaRepository.findById(7L)).thenReturn(Optional.of(recurso));
        when(multimediaRepository.save(any(Multimedia.class))).thenAnswer(i -> i.getArgument(0));

        MultimediaResponseDTO respuesta = servicio.actualizar(7L, pedido(null));

        assertEquals("https://img.test/plano-en.jpg", respuesta.getUrlEn());
    }

    @Test
    void alActualizarConTextoVacioSeQuitaLaVersionEnIngles() {
        Multimedia recurso = existente("https://img.test/plano-en.jpg");
        when(multimediaRepository.findById(7L)).thenReturn(Optional.of(recurso));
        when(multimediaRepository.save(any(Multimedia.class))).thenAnswer(i -> i.getArgument(0));

        assertNull(servicio.actualizar(7L, pedido("")).getUrlEn());
    }

    @Test
    void alActualizarConUnaUrlNuevaSeReemplaza() {
        Multimedia recurso = existente("https://img.test/vieja-en.jpg");
        when(multimediaRepository.findById(7L)).thenReturn(Optional.of(recurso));
        when(multimediaRepository.save(any(Multimedia.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("https://img.test/nueva-en.jpg", servicio.actualizar(7L, pedido("https://img.test/nueva-en.jpg")).getUrlEn());
    }

    // ───────── Plano de urbanismo (ubicación) ─────────
    private final UbicacionMapper ubicacionMapper = new UbicacionMapper();

    private UbicacionRequestDTO pedidoUbicacion(String urbanismoEn) {
        UbicacionRequestDTO dto = new UbicacionRequestDTO();
        dto.setProyectoId(1L);
        dto.setDireccion("Vía La Paila");
        dto.setCiudad("La Tebaida");
        dto.setDepartamento("Quindío");
        dto.setUrbanismoUrl("https://img.test/urbanismo-es.jpg");
        dto.setUrbanismoUrlEn(urbanismoEn);
        return dto;
    }

    @Test
    void elPlanoDeUrbanismoGuardaSuVersionEnIngles() {
        Ubicacion ubicacion = ubicacionMapper.toEntity(pedidoUbicacion("  https://img.test/urbanismo-en.jpg "), new Proyecto());
        assertEquals("https://img.test/urbanismo-en.jpg", ubicacion.getUrbanismoUrlEn());

        UbicacionResponseDTO respuesta = ubicacionMapper.toDto(ubicacion);
        assertEquals("https://img.test/urbanismo-en.jpg", respuesta.getUrbanismoUrlEn());
        assertEquals("https://img.test/urbanismo-es.jpg", respuesta.getUrbanismoUrl());
    }

    @Test
    void elPlanoDeUrbanismoSinVersionEnInglesQuedaVacio() {
        assertNull(ubicacionMapper.toEntity(pedidoUbicacion("  "), new Proyecto()).getUrbanismoUrlEn());
        assertNull(ubicacionMapper.toEntity(pedidoUbicacion(null), new Proyecto()).getUrbanismoUrlEn());
    }
}
