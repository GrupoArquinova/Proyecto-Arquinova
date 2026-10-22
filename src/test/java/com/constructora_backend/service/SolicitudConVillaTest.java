package com.constructora_backend.service;

import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.entity.*;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.SolicitudContactoMapper;
import com.constructora_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Solicitud que llega desde una villa (tipología) o un lote: se guarda con esa referencia y la confirmación al cliente la menciona. */
@ExtendWith(MockitoExtension.class)
class SolicitudConVillaTest {

    @Mock private SolicitudContactoRepository solicitudRepository;
    @Mock private EstadoSolicitudRepository estadoSolicitudRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private CasaModeloRepository casaModeloRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;
    @Spy private SolicitudContactoMapper mapper = new SolicitudContactoMapper();

    @InjectMocks private SolicitudContactoService servicio;

    private Proyecto elEncanto;
    private CasaModelo villaSaman;

    @BeforeEach
    void preparar() {
        EstadoSolicitud nuevo = new EstadoSolicitud();
        nuevo.setId(1);
        nuevo.setNombre("NUEVO");
        lenient().when(estadoSolicitudRepository.findFirstByNombreIgnoreCase("NUEVO")).thenReturn(Optional.of(nuevo));
        lenient().when(solicitudRepository.save(any(SolicitudContacto.class))).thenAnswer(i -> i.getArgument(0));

        elEncanto = new Proyecto();
        elEncanto.setId(6L);
        elEncanto.setNombre("El Encanto");
        villaSaman = new CasaModelo();
        villaSaman.setId(11L);
        villaSaman.setNombre("Villa Samán");
        villaSaman.setProyecto(elEncanto);
    }

    private SolicitudContactoPublicDTO pedido(String idioma) {
        SolicitudContactoPublicDTO dto = new SolicitudContactoPublicDTO();
        dto.setNombre("Ana");
        dto.setCorreo("ana@test.com");
        dto.setMensaje("Quiero el precio");
        dto.setConsentimientoDatos(true);
        dto.setProyectoId(6L);
        dto.setIdioma(idioma);
        return dto;
    }

    @Test
    void laSolicitudGuardaLaVillaYLaConfirmacionLaMenciona() {
        when(proyectoRepository.findById(6L)).thenReturn(Optional.of(elEncanto));
        when(casaModeloRepository.findById(11L)).thenReturn(Optional.of(villaSaman));
        SolicitudContactoPublicDTO dto = pedido("es");
        dto.setCasaModeloId(11L);

        SolicitudContactoResponseDTO respuesta = servicio.crearPublica(dto);

        assertEquals(11L, respuesta.getCasaModeloId());
        assertEquals("Villa Samán", respuesta.getCasaModeloNombre());
        verify(emailService).enviarConfirmacionSolicitud("Ana", "ana@test.com", "El Encanto", "Villa Samán", null, "es");
        // El aviso al equipo también dice por qué villa preguntó (siempre en español)
        verify(emailService).enviarCorreoNuevaSolicitud(eq("Ana"), any(), eq("ana@test.com"), eq("El Encanto"), eq("Villa Samán"), any(), eq("Quiero el precio"), eq("es"));
    }

    @Test
    void laConfirmacionMencionaElLoteEnElIdiomaDelSitio() {
        Lote lote = new Lote();
        lote.setId(5L);
        lote.setCodigo("LT-101");
        when(proyectoRepository.findById(6L)).thenReturn(Optional.of(elEncanto));
        when(loteRepository.findById(5L)).thenReturn(Optional.of(lote));
        SolicitudContactoPublicDTO dto = pedido("en");
        dto.setLoteId(5L);

        servicio.crearPublica(dto);

        verify(emailService).enviarConfirmacionSolicitud("Ana", "ana@test.com", "El Encanto", "Lot LT-101", null, "en");
    }

    @Test
    void sinVillaNiLoteLaConfirmacionSoloLlevaElProyecto() {
        when(proyectoRepository.findById(6L)).thenReturn(Optional.of(elEncanto));

        servicio.crearPublica(pedido("es"));

        verify(emailService).enviarConfirmacionSolicitud(eq("Ana"), eq("ana@test.com"), eq("El Encanto"), isNull(), isNull(), eq("es"));
    }

    @Test
    void unaVillaDeOtroProyectoSeRechaza() {
        Proyecto otro = new Proyecto();
        otro.setId(99L);
        villaSaman.setProyecto(otro);
        when(proyectoRepository.findById(6L)).thenReturn(Optional.of(elEncanto));
        when(casaModeloRepository.findById(11L)).thenReturn(Optional.of(villaSaman));
        SolicitudContactoPublicDTO dto = pedido("es");
        dto.setCasaModeloId(11L);

        assertThrows(BadRequestException.class, () -> servicio.crearPublica(dto));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void unaVillaQueNoExisteSeRechaza() {
        when(proyectoRepository.findById(6L)).thenReturn(Optional.of(elEncanto));
        when(casaModeloRepository.findById(404L)).thenReturn(Optional.empty());
        SolicitudContactoPublicDTO dto = pedido("es");
        dto.setCasaModeloId(404L);

        assertThrows(ResourceNotFoundException.class, () -> servicio.crearPublica(dto));
    }

    @Test
    void describirInteresUneVillaYLote() {
        Lote lote = new Lote();
        lote.setCodigo("LT-7");

        assertEquals("Villa Samán · Lote LT-7", SolicitudContactoService.describirInteres(lote, villaSaman, "es"));
        assertEquals("Villa Samán · Lot LT-7", SolicitudContactoService.describirInteres(lote, villaSaman, "en"));
        assertNull(SolicitudContactoService.describirInteres(null, null, "es"));
    }

    @Test
    void siFallaElCorreoLaSolicitudIgualSeGuarda() {
        when(proyectoRepository.findById(6L)).thenReturn(Optional.of(elEncanto));
        doThrow(new RuntimeException("SMTP caído")).when(emailService)
                .enviarConfirmacionSolicitud(any(), any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> servicio.crearPublica(pedido("es")));
        verify(solicitudRepository).save(any(SolicitudContacto.class));
    }
}
