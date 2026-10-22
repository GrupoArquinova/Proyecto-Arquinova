package com.constructora_backend.service;

import com.constructora_backend.controller.CasaModeloController;
import com.constructora_backend.controller.UbicacionController;
import com.constructora_backend.controller.ZonaComunController;
import com.constructora_backend.controller.ZonaComunImagenController;
import com.constructora_backend.dto.response.CasaModeloResponseDTO;
import com.constructora_backend.dto.response.UbicacionResponseDTO;
import com.constructora_backend.dto.response.ZonaComunImagenResponseDTO;
import com.constructora_backend.dto.response.ZonaComunResponseDTO;
import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.entity.ZonaComunImagen;
import com.constructora_backend.mapper.CasaModeloMapper;
import com.constructora_backend.mapper.UbicacionMapper;
import com.constructora_backend.mapper.ZonaComunImagenMapper;
import com.constructora_backend.mapper.ZonaComunMapper;
import com.constructora_backend.repository.CasaModeloRepository;
import com.constructora_backend.repository.UbicacionRepository;
import com.constructora_backend.repository.ZonaComunImagenRepository;
import com.constructora_backend.repository.ZonaComunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Casas modelo, zonas comunes, imágenes de zonas comunes y ubicaciones: el público solo ve lo publicado y activo
 * (y dentro de un proyecto también publicado y activo); el administrador ve todo. Mismo patrón que Lote y Proyecto.
 */
@ExtendWith(MockitoExtension.class)
class VisibilidadPublicaTest {

    private static final Authentication ADMIN = new TestingAuthenticationToken("admin", "x", "ROLE_ADMINISTRADOR");
    private static final Authentication USUARIO = new TestingAuthenticationToken("ana", "x", "ROLE_USUARIO");
    private static final Authentication ANONIMO = new AnonymousAuthenticationToken("k", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    @Mock private CasaModeloRepository casaRepository;
    @Mock private CasaModeloMapper casaMapper;
    @Mock private ZonaComunRepository zonaRepository;
    @Mock private ZonaComunMapper zonaMapper;
    @Mock private ZonaComunImagenRepository imagenRepository;
    @Mock private ZonaComunImagenMapper imagenMapper;
    @Mock private UbicacionRepository ubicacionRepository;
    @Mock private UbicacionMapper ubicacionMapper;

    @InjectMocks private CasaModeloService casaService;
    @InjectMocks private ZonaComunService zonaService;
    @InjectMocks private ZonaComunImagenService imagenService;
    @InjectMocks private UbicacionService ubicacionService;

    private Proyecto visible;
    private Proyecto oculto;
    private Proyecto sinPublicar;

    private static Proyecto proyecto(long id, boolean publicado, boolean activo) {
        Proyecto p = new Proyecto();
        p.setId(id);
        p.setPublicado(publicado);
        p.setActivo(activo);
        return p;
    }

    @BeforeEach
    void preparar() {
        visible = proyecto(1, true, true);
        oculto = proyecto(2, true, false);       // inactivo
        sinPublicar = proyecto(3, false, true);  // sin publicar
        lenient().when(casaMapper.toDTO(any())).thenAnswer(i -> new CasaModeloResponseDTO());
        lenient().when(zonaMapper.toDTO(any())).thenAnswer(i -> new ZonaComunResponseDTO());
        lenient().when(imagenMapper.toDTO(any())).thenAnswer(i -> new ZonaComunImagenResponseDTO());
        lenient().when(ubicacionMapper.toDto(any())).thenAnswer(i -> new UbicacionResponseDTO());
    }

    private CasaModelo casa(Proyecto p, boolean publicada, boolean activa) {
        CasaModelo c = new CasaModelo();
        c.setId(10L);
        c.setProyecto(p);
        c.setPublicado(publicada);
        c.setActivo(activa);
        return c;
    }

    private ZonaComun zona(Proyecto p, boolean publicada, boolean activa) {
        ZonaComun z = new ZonaComun();
        z.setId(20L);
        z.setProyecto(p);
        z.setPublicado(publicada);
        z.setActivo(activa);
        return z;
    }

    private Ubicacion ubicacion(long id, Proyecto p) {
        Ubicacion u = new Ubicacion();
        u.setId(id);
        u.setProyecto(p);
        return u;
    }

    // ───────── Casas modelo ─────────

    @Test
    @DisplayName("Casa modelo por ID (público): solo si está publicada, activa y su proyecto también")
    void casaPorIdPublica() {
        when(casaRepository.findById(10L)).thenReturn(Optional.of(casa(visible, true, true)));
        assertTrue(casaService.obtenerPublicaPorId(10L).isPresent());

        for (CasaModelo c : List.of(casa(visible, false, true), casa(visible, true, false), casa(oculto, true, true), casa(sinPublicar, true, true))) {
            when(casaRepository.findById(10L)).thenReturn(Optional.of(c));
            assertTrue(casaService.obtenerPublicaPorId(10L).isEmpty());
        }
    }

    @Test
    @DisplayName("Casas modelo públicas de un proyecto: usa la consulta que exige proyecto publicado y activo")
    void casasPublicasDeUnProyecto() {
        when(casaRepository.findVisiblesAlPublicoPorProyecto(1L)).thenReturn(List.of(casa(visible, true, true)));

        assertEquals(1, casaService.listarPublicadasYActivasPorProyecto(1L).size());
        verify(casaRepository).findVisiblesAlPublicoPorProyecto(1L);
    }

    @Test
    @DisplayName("Casa modelo por ID (controlador): el administrador ve todo; los demás, solo lo público o 404")
    void casaControlador() {
        CasaModeloService servicio = org.mockito.Mockito.mock(CasaModeloService.class);
        CasaModeloController controlador = new CasaModeloController();
        org.springframework.test.util.ReflectionTestUtils.setField(controlador, "casaModeloService", servicio);
        when(servicio.obtenerPorId(10L)).thenReturn(Optional.of(new CasaModeloResponseDTO()));
        when(servicio.obtenerPublicaPorId(10L)).thenReturn(Optional.empty());

        assertEquals(200, controlador.obtenerPorId(10L, ADMIN).getStatusCode().value());
        assertEquals(404, controlador.obtenerPorId(10L, null).getStatusCode().value());
        assertEquals(404, controlador.obtenerPorId(10L, ANONIMO).getStatusCode().value());
        assertEquals(404, controlador.obtenerPorId(10L, USUARIO).getStatusCode().value());
    }

    // ───────── Zonas comunes ─────────

    @Test
    @DisplayName("Zona común por ID (público): solo si está publicada, activa y su proyecto también")
    void zonaPorIdPublica() {
        when(zonaRepository.findById(20L)).thenReturn(Optional.of(zona(visible, true, true)));
        assertTrue(zonaService.obtenerPublicaPorId(20L).isPresent());

        for (ZonaComun z : List.of(zona(visible, false, true), zona(visible, true, false), zona(oculto, true, true), zona(sinPublicar, true, true))) {
            when(zonaRepository.findById(20L)).thenReturn(Optional.of(z));
            assertTrue(zonaService.obtenerPublicaPorId(20L).isEmpty());
        }
    }

    @Test
    @DisplayName("Zonas comunes públicas de un proyecto: exige proyecto publicado y activo")
    void zonasPublicasDeUnProyecto() {
        when(zonaRepository.findVisiblesAlPublicoPorProyecto(1L)).thenReturn(List.of(zona(visible, true, true)));

        assertEquals(1, zonaService.listarPublicadasYActivasPorProyectos(1L).size());
        verify(zonaRepository).findVisiblesAlPublicoPorProyecto(1L);
    }

    @Test
    @DisplayName("Zona común por ID (controlador): admin ve todo; los demás, solo lo público o 404")
    void zonaControlador() {
        ZonaComunService servicio = org.mockito.Mockito.mock(ZonaComunService.class);
        ZonaComunController controlador = new ZonaComunController();
        org.springframework.test.util.ReflectionTestUtils.setField(controlador, "zonaComunService", servicio);
        when(servicio.obtenerPorId(20L)).thenReturn(Optional.of(new ZonaComunResponseDTO()));
        when(servicio.obtenerPublicaPorId(20L)).thenReturn(Optional.empty());

        assertEquals(200, controlador.obtenerPorId(20L, ADMIN).getStatusCode().value());
        assertEquals(404, controlador.obtenerPorId(20L, null).getStatusCode().value());
        assertEquals(404, controlador.obtenerPorId(20L, USUARIO).getStatusCode().value());
    }

    // ───────── Imágenes de zonas comunes ─────────

    private ZonaComunImagen imagen(ZonaComun z) {
        ZonaComunImagen i = new ZonaComunImagen();
        i.setId(30L);
        i.setZonaComun(z);
        return i;
    }

    @Test
    @DisplayName("Imágenes de una zona (público): solo si la zona es visible; si no, lista vacía")
    void imagenesPublicasPorZona() {
        when(zonaRepository.findById(20L)).thenReturn(Optional.of(zona(visible, true, true)));
        when(imagenRepository.findByZonaComunIdOrderByOrdenAsc(20L)).thenReturn(List.of(imagen(zona(visible, true, true))));
        assertEquals(1, imagenService.listarPublicasPorZonaComun(20L).size());

        for (ZonaComun z : List.of(zona(visible, false, true), zona(visible, true, false), zona(oculto, true, true), zona(sinPublicar, true, true))) {
            when(zonaRepository.findById(20L)).thenReturn(Optional.of(z));
            assertTrue(imagenService.listarPublicasPorZonaComun(20L).isEmpty());
        }
        when(zonaRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(imagenService.listarPublicasPorZonaComun(99L).isEmpty());
    }

    @Test
    @DisplayName("Imagen por ID (público): solo si su zona es visible")
    void imagenPorIdPublica() {
        when(imagenRepository.findById(30L)).thenReturn(Optional.of(imagen(zona(visible, true, true))));
        assertTrue(imagenService.obtenerPublicaPorId(30L).isPresent());

        when(imagenRepository.findById(30L)).thenReturn(Optional.of(imagen(zona(sinPublicar, true, true))));
        assertTrue(imagenService.obtenerPublicaPorId(30L).isEmpty());

        when(imagenRepository.findById(30L)).thenReturn(Optional.of(imagen(zona(visible, false, true))));
        assertTrue(imagenService.obtenerPublicaPorId(30L).isEmpty());
    }

    @Test
    @DisplayName("Imágenes (controlador): admin ve todo; los demás pasan por las lecturas públicas")
    void imagenControlador() {
        ZonaComunImagenService servicio = org.mockito.Mockito.mock(ZonaComunImagenService.class);
        ZonaComunImagenController controlador = new ZonaComunImagenController();
        org.springframework.test.util.ReflectionTestUtils.setField(controlador, "imagenService", servicio);
        when(servicio.listarPorZonaComun(20L)).thenReturn(List.of(new ZonaComunImagenResponseDTO(), new ZonaComunImagenResponseDTO()));
        when(servicio.listarPublicasPorZonaComun(20L)).thenReturn(List.of());
        when(servicio.obtenerPorId(30L)).thenReturn(Optional.of(new ZonaComunImagenResponseDTO()));
        when(servicio.obtenerPublicaPorId(30L)).thenReturn(Optional.empty());

        assertEquals(2, controlador.listarPorZonaComun(20L, ADMIN).getBody().size());
        assertEquals(0, controlador.listarPorZonaComun(20L, null).getBody().size());
        assertEquals(200, controlador.obtenerPorId(30L, ADMIN).getStatusCode().value());
        assertEquals(404, controlador.obtenerPorId(30L, null).getStatusCode().value());
    }

    // ───────── Ubicaciones ─────────

    @Test
    @DisplayName("Ubicaciones (público): el listado solo trae las de proyectos publicados y activos")
    void ubicacionesPublicasFiltraPorProyecto() {
        when(ubicacionRepository.findAll()).thenReturn(List.of(ubicacion(1, visible), ubicacion(2, oculto), ubicacion(3, sinPublicar)));

        assertEquals(1, ubicacionService.listarPublicas().size());
    }

    @Test
    @DisplayName("Ubicación por ID, por proyecto y por ciudad (público): solo si el proyecto es visible")
    void ubicacionPublicaPorIdProyectoYCiudad() {
        when(ubicacionRepository.findById(1L)).thenReturn(Optional.of(ubicacion(1, visible)));
        when(ubicacionRepository.findById(2L)).thenReturn(Optional.of(ubicacion(2, sinPublicar)));
        assertTrue(ubicacionService.obtenerPublicaPorId(1L).isPresent());
        assertTrue(ubicacionService.obtenerPublicaPorId(2L).isEmpty());

        when(ubicacionRepository.findByProyectoId(1L)).thenReturn(Optional.of(ubicacion(1, visible)));
        when(ubicacionRepository.findByProyectoId(2L)).thenReturn(Optional.of(ubicacion(2, oculto)));
        assertTrue(ubicacionService.obtenerPublicaPorProyectoId(1L).isPresent());
        assertTrue(ubicacionService.obtenerPublicaPorProyectoId(2L).isEmpty());

        when(ubicacionRepository.findByCiudadIgnoreCase("Armenia")).thenReturn(List.of(ubicacion(1, visible), ubicacion(2, oculto)));
        assertEquals(1, ubicacionService.listarPublicasPorCiudad("Armenia").size());
    }

    @Test
    @DisplayName("Ubicaciones (controlador): admin ve todo; los demás, solo lo público")
    void ubicacionControlador() {
        UbicacionService servicio = org.mockito.Mockito.mock(UbicacionService.class);
        UbicacionController controlador = new UbicacionController(servicio);
        when(servicio.listarTodas()).thenReturn(List.of(new UbicacionResponseDTO(), new UbicacionResponseDTO()));
        when(servicio.listarPublicas()).thenReturn(List.of(new UbicacionResponseDTO()));
        when(servicio.obtenerPorId(1L)).thenReturn(Optional.of(new UbicacionResponseDTO()));
        when(servicio.obtenerPublicaPorId(1L)).thenReturn(Optional.empty());
        when(servicio.obtenerPorProyectoId(1L)).thenReturn(Optional.of(new UbicacionResponseDTO()));
        when(servicio.obtenerPublicaPorProyectoId(1L)).thenReturn(Optional.empty());
        when(servicio.listarPorCiudad("Armenia")).thenReturn(List.of(new UbicacionResponseDTO(), new UbicacionResponseDTO()));
        when(servicio.listarPublicasPorCiudad("Armenia")).thenReturn(List.of());

        assertEquals(2, controlador.listarTodas(ADMIN).getBody().size());
        assertEquals(1, controlador.listarTodas(null).getBody().size());
        assertEquals(200, controlador.obtenerPorId(1L, ADMIN).getStatusCode().value());
        assertEquals(404, controlador.obtenerPorId(1L, null).getStatusCode().value());
        assertEquals(200, controlador.obtenerPorProyectoId(1L, ADMIN).getStatusCode().value());
        assertEquals(404, controlador.obtenerPorProyectoId(1L, USUARIO).getStatusCode().value());
        assertEquals(2, controlador.listarPorCiudad("Armenia", ADMIN).getBody().size());
        assertEquals(0, controlador.listarPorCiudad("Armenia", null).getBody().size());
    }
}
