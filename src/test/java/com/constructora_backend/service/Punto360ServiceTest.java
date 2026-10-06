package com.constructora_backend.service;

import com.constructora_backend.dto.request.Punto360RequestDTO;
import com.constructora_backend.dto.response.Punto360ResponseDTO;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Punto360;
import com.constructora_backend.enums.EscenaPunto360;
import com.constructora_backend.repository.EtapaRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.Punto360Repository;
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
class Punto360ServiceTest {

    @Mock
    private Punto360Repository punto360Repository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private LoteRepository loteRepository;
    @Mock
    private EtapaRepository etapaRepository;

    @InjectMocks
    private Punto360Service service;

    private Proyecto proyecto;
    private Proyecto otroProyecto;

    @BeforeEach
    void setUp() {
        proyecto = new Proyecto();
        proyecto.setId(1L);
        otroProyecto = new Proyecto();
        otroProyecto.setId(2L);
    }

    private Lote loteDe(Proyecto p) {
        Etapa etapa = new Etapa();
        etapa.setId(10L);
        etapa.setProyecto(p);
        Lote lote = new Lote();
        lote.setId(5L);
        lote.setCodigo("C21");
        lote.setAreaM2(new BigDecimal("11448"));
        lote.setEtapa(etapa);
        EstadoLote estado = new EstadoLote();
        estado.setNombre("DISPONIBLE");
        lote.setEstado(estado);
        return lote;
    }

    private Punto360RequestDTO panorama() {
        Punto360RequestDTO dto = new Punto360RequestDTO();
        dto.setProyectoId(1L);
        dto.setEscena(EscenaPunto360.ENTORNO);
        dto.setLoteId(5L);
        dto.setEtiqueta("  C21  ");
        dto.setYaw(new BigDecimal("1.234567"));
        dto.setPitch(new BigDecimal("-0.2"));
        return dto;
    }

    @Test
    void guardaUnPuntoDeUnaImagen360ConLosDatosDelLote() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(loteRepository.findById(5L)).thenReturn(Optional.of(loteDe(proyecto)));
        when(punto360Repository.save(any(Punto360.class))).thenAnswer(inv -> {
            Punto360 p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        Punto360ResponseDTO respuesta = service.guardar(panorama());

        assertEquals(99L, respuesta.getId());
        assertEquals("C21", respuesta.getEtiqueta());
        assertEquals("C21", respuesta.getLoteCodigo());
        assertEquals("DISPONIBLE", respuesta.getLoteEstado());
        assertNull(respuesta.getPosX());
        assertEquals(new BigDecimal("1.234567"), respuesta.getYaw());
    }

    @Test
    void unaImagen360SinAngulosSeRechaza() {
        Punto360RequestDTO dto = panorama();
        dto.setYaw(null);

        assertThrows(IllegalArgumentException.class, () -> service.guardar(dto));
        verify(punto360Repository, never()).save(any());
    }

    @Test
    void elPlanoDeUrbanismoNecesitaPosicionEnPorcentaje() {
        Punto360RequestDTO dto = panorama();
        dto.setEscena(EscenaPunto360.URBANISMO);

        assertThrows(IllegalArgumentException.class, () -> service.guardar(dto));
    }

    @Test
    void unLoteDeOtroProyectoSeRechaza() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(loteRepository.findById(5L)).thenReturn(Optional.of(loteDe(otroProyecto)));

        assertThrows(IllegalArgumentException.class, () -> service.guardar(panorama()));
        verify(punto360Repository, never()).save(any());
    }

    @Test
    void laLecturaOmiteLosPuntosCuyoLoteYaNoExiste() {
        // Sin clave foránea, borrar un lote deja su punto huérfano: no debe romper la lectura del proyecto
        Lote borrado = mock(Lote.class);
        when(borrado.getId()).thenReturn(5L);
        when(borrado.getCodigo()).thenThrow(new jakarta.persistence.EntityNotFoundException("Unable to find Lote"));

        Punto360 huerfano = Punto360.builder().id(1L).proyecto(proyecto).escena(EscenaPunto360.ENTORNO)
                .etiqueta("C21").lote(borrado).yaw(BigDecimal.ONE).pitch(BigDecimal.ZERO).build();
        Punto360 sano = Punto360.builder().id(2L).proyecto(proyecto).escena(EscenaPunto360.ENTORNO)
                .etiqueta("C22").lote(loteDe(proyecto)).yaw(BigDecimal.ONE).pitch(BigDecimal.ZERO).build();
        when(punto360Repository.findByProyectoIdAndActivoTrueOrderByIdAsc(1L)).thenReturn(java.util.List.of(huerfano, sano));

        assertEquals(java.util.List.of(2L), service.listarPorProyecto(1L, null).stream().map(Punto360ResponseDTO::getId).toList());
    }

    @Test
    void enElPlanoSeGuardanSoloLasPosicionesEnPorcentaje() {
        Punto360RequestDTO dto = panorama();
        dto.setEscena(EscenaPunto360.URBANISMO);
        dto.setLoteId(null);
        dto.setPosX(new BigDecimal("42.5"));
        dto.setPosY(new BigDecimal("10"));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(punto360Repository.save(any(Punto360.class))).thenAnswer(inv -> inv.getArgument(0));

        Punto360ResponseDTO respuesta = service.guardar(dto);

        assertEquals(new BigDecimal("42.5"), respuesta.getPosX());
        assertNull(respuesta.getYaw());
        assertNull(respuesta.getPitch());
    }
}
