package com.constructora_backend.service;

import com.constructora_backend.entity.*;
import com.constructora_backend.enums.EstadoProyecto;
import com.constructora_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Datos reales y públicos que se le dan al asistente: proyectos publicados, villas, amenidades, ubicación y lotes por estado. */
@ExtendWith(MockitoExtension.class)
class ChatContextoServiceTest {

    @Mock private ProyectoRepository proyectoRepository;
    @Mock private UbicacionRepository ubicacionRepository;
    @Mock private CasaModeloRepository casaModeloRepository;
    @Mock private ZonaComunRepository zonaComunRepository;
    @Mock private LoteRepository loteRepository;
    @InjectMocks private ChatContextoService servicio;

    private Proyecto elEncanto;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(servicio, "whatsappEmpresa", "+57 316 865 3715");
        ReflectionTestUtils.setField(servicio, "sitioUrl", "https://arquinova.vercel.app");

        elEncanto = new Proyecto();
        elEncanto.setId(6L);
        elEncanto.setNombre("El Encanto");
        elEncanto.setDescripcion("Proyecto   inspirado\nen el Quindío.");
        elEncanto.setEstadoProyecto(EstadoProyecto.EN_DISENO);
        elEncanto.setDestacado(true);
    }

    private Lote lote(String estado) {
        Etapa etapa = new Etapa();
        etapa.setProyecto(elEncanto);
        EstadoLote e = new EstadoLote();
        e.setNombre(estado);
        Lote l = new Lote();
        l.setEtapa(etapa);
        l.setEstado(e);
        return l;
    }

    @Test
    void incluyeLosDatosRealesDelProyectoSusVillasAmenidadesYLotes() {
        CasaModelo villa = new CasaModelo();
        villa.setNombre("Villa Samán");
        villa.setAreaConstruidaM2(new BigDecimal("293.00"));
        villa.setNumeroHabitaciones((byte) 3);
        villa.setNumeroBanos((byte) 4);
        ZonaComun jacuzzi = new ZonaComun();
        jacuzzi.setNombre("Jacuzzi");
        Ubicacion u = new Ubicacion();
        u.setCiudad("La Tebaida");
        u.setDepartamento("Quindío");

        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        when(ubicacionRepository.findByProyectoId(6L)).thenReturn(Optional.of(u));
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of(villa));
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of(jacuzzi));
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(lote("Disponible"), lote("Disponible"), lote("Vendido")));

        String texto = servicio.construir();

        assertTrue(texto.contains("Grupo Arquinova S.A.S."));
        assertTrue(texto.contains("WhatsApp del equipo: +57 316 865 3715"));
        assertTrue(texto.contains("== Proyecto: El Encanto (proyecto destacado) =="));
        assertTrue(texto.contains("Etapa del proyecto: en diseño"));
        assertTrue(texto.contains("Descripción: Proyecto inspirado en el Quindío."));
        assertTrue(texto.contains("Ubicación: La Tebaida, Quindío"));
        assertTrue(texto.contains("Tipología/villa Villa Samán: 293 m² construidos, 3 habitaciones, 4 baños"));
        assertTrue(texto.contains("Amenidades: Jacuzzi"));
        assertTrue(texto.contains("Lotes por estado: 2 disponible, 1 vendido"));
    }

    @Test
    void sinProyectosPublicadosLoDiceEnVezDeInventar() {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());

        assertTrue(servicio.construir().contains("no hay proyectos publicados"));
    }

    @Test
    void elTextoSeGuardaDiezMinutosYDespuesSeVuelveAConsultar() {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        Instant t = Instant.parse("2026-10-15T10:00:00Z");

        servicio.obtener(t);
        servicio.obtener(t.plus(Duration.ofMinutes(9)));
        verify(proyectoRepository, times(1)).findByPublicadoTrueAndActivoTrue();

        servicio.obtener(t.plus(Duration.ofMinutes(11)));
        verify(proyectoRepository, times(2)).findByPublicadoTrueAndActivoTrue();
    }

    @Test
    void lasDescripcionesMuyLargasSeRecortan() {
        elEncanto.setDescripcion("x".repeat(2000));
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        when(ubicacionRepository.findByProyectoId(6L)).thenReturn(Optional.empty());
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());

        String texto = servicio.construir();

        assertFalse(texto.contains("x".repeat(500)));
        assertTrue(texto.contains("…"));
    }

    @Test
    void elContextoNuncaTrabajaPrecios() {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        when(ubicacionRepository.findByProyectoId(6L)).thenReturn(Optional.empty());
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());

        assertFalse(servicio.construir().toLowerCase().contains("precio"));
    }
}
