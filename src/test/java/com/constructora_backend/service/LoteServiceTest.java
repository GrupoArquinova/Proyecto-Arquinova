package com.constructora_backend.service;

import com.constructora_backend.dto.request.LoteRequestDTO;
import com.constructora_backend.dto.response.LoteResponseDTO;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.HistorialEstadoLote;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.mapper.LoteMapper;
import com.constructora_backend.repository.EstadoLoteRepository;
import com.constructora_backend.repository.EtapaRepository;
import com.constructora_backend.repository.HistorialEstadoLoteRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
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

    @Mock
    private HistorialEstadoLoteRepository historialEstadoLoteRepository;

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

        com.constructora_backend.exception.DuplicateResourceException excepcion = assertThrows(
                com.constructora_backend.exception.DuplicateResourceException.class,
                () -> loteService.guardar(requestDTO));

        assertTrue(excepcion.getMessage().contains("Ya existe un lote con el código"));
        verify(loteRepository, never()).save(any());
    }

    @Test
    void listarPublicadosYActivosUsaFiltroYQuitaDatosInternos() {
        responseDTO.setCreadoPorId(7L);
        responseDTO.setCreadoPorNombre("Empleado Interno");
        responseDTO.setActualizadoPorId(8L);
        responseDTO.setActualizadoPorNombre("Otro Empleado");
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(java.util.List.of(lote));
        when(loteMapper.toDTO(lote)).thenReturn(responseDTO);

        java.util.List<LoteResponseDTO> resultado = loteService.listarPublicadosYActivos();

        assertEquals(1, resultado.size());
        assertEquals("LOTE-A12", resultado.get(0).getCodigo());
        assertNull(resultado.get(0).getCreadoPorId());
        assertNull(resultado.get(0).getCreadoPorNombre());
        assertNull(resultado.get(0).getActualizadoPorId());
        assertNull(resultado.get(0).getActualizadoPorNombre());
        verify(loteRepository, never()).findAll();
    }

    @Test
    void obtenerPublicoPorIdDevuelveLotePublicadoYActivo() {
        when(loteRepository.findById(50L)).thenReturn(Optional.of(lote));
        when(loteMapper.toDTO(lote)).thenReturn(responseDTO);

        Optional<LoteResponseDTO> resultado = loteService.obtenerPublicoPorId(50L);

        assertTrue(resultado.isPresent());
        assertEquals(50L, resultado.get().getId());
    }

    @Test
    void obtenerPublicoPorIdNoDevuelveLoteNoPublicado() {
        lote.setPublicado(false);
        when(loteRepository.findById(50L)).thenReturn(Optional.of(lote));

        assertTrue(loteService.obtenerPublicoPorId(50L).isEmpty());
        verifyNoInteractions(loteMapper);
    }

    @Test
    void obtenerPublicoPorIdNoDevuelveLoteInactivo() {
        lote.setActivo(false);
        when(loteRepository.findById(50L)).thenReturn(Optional.of(lote));

        assertTrue(loteService.obtenerPublicoPorId(50L).isEmpty());
        verifyNoInteractions(loteMapper);
    }

    // ───── Cambio de estado + historial ─────

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private Usuario autenticarAdmin() {
        Usuario admin = new Usuario();
        admin.setId(9L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@arquinova.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
        when(usuarioRepository.findByCorreo("admin@arquinova.com")).thenReturn(Optional.of(admin));
        return admin;
    }

    private EstadoLote estadoReservado() {
        EstadoLote reservado = new EstadoLote();
        reservado.setId(2);
        return reservado;
    }

    @Test
    void cambiarEstadoRegistraHistorialConUsuarioAutenticado() {
        Usuario admin = autenticarAdmin();
        EstadoLote reservado = estadoReservado();
        when(loteRepository.findById(50L)).thenReturn(Optional.of(lote));
        when(estadoLoteRepository.findById(2)).thenReturn(Optional.of(reservado));
        when(loteMapper.toDTO(lote)).thenReturn(responseDTO);

        loteService.cambiarEstado(50L, 2, "Reservado tras pago de separación");

        assertSame(reservado, lote.getEstado());
        verify(loteRepository).save(lote);
        verify(historialEstadoLoteRepository).save(argThat((HistorialEstadoLote h) ->
                h.getLote() == lote
                        && h.getEstadoAnterior() == estado
                        && h.getEstadoNuevo() == reservado
                        && h.getUsuario() == admin
                        && "Reservado tras pago de separación".equals(h.getObservaciones())));
    }

    @Test
    void cambiarEstadoAlMismoEstadoNoGuardaNiRegistraHistorial() {
        when(loteRepository.findById(50L)).thenReturn(Optional.of(lote));
        when(estadoLoteRepository.findById(1)).thenReturn(Optional.of(estado));
        when(loteMapper.toDTO(lote)).thenReturn(responseDTO);

        loteService.cambiarEstado(50L, 1, null);

        verify(loteRepository, never()).save(any());
        verifyNoInteractions(historialEstadoLoteRepository);
    }

    @Test
    void cambiarEstadoSinUsuarioAutenticadoCambiaEstadoPeroNoRegistraHistorial() {
        EstadoLote reservado = estadoReservado();
        when(loteRepository.findById(50L)).thenReturn(Optional.of(lote));
        when(estadoLoteRepository.findById(2)).thenReturn(Optional.of(reservado));
        when(loteMapper.toDTO(lote)).thenReturn(responseDTO);

        loteService.cambiarEstado(50L, 2, null);

        assertSame(reservado, lote.getEstado());
        verify(loteRepository).save(lote);
        verifyNoInteractions(historialEstadoLoteRepository);
    }

    @Test
    void actualizarConCambioDeEstadoRegistraHistorial() {
        autenticarAdmin();
        EstadoLote reservado = estadoReservado();
        requestDTO.setEstadoId(2);
        when(loteRepository.findById(50L)).thenReturn(Optional.of(lote));
        when(etapaRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(estadoLoteRepository.findById(2)).thenReturn(Optional.of(reservado));
        when(loteRepository.existsByEtapaIdAndCodigoAndIdNot(2L, "LOTE-A12", 50L)).thenReturn(false);
        when(loteRepository.save(lote)).thenReturn(lote);
        when(loteMapper.toDTO(lote)).thenReturn(responseDTO);

        loteService.actualizar(50L, requestDTO);

        verify(historialEstadoLoteRepository).save(argThat((HistorialEstadoLote h) ->
                h.getEstadoAnterior() == estado && h.getEstadoNuevo() == reservado));
    }

    @Test
    void consultarHistorialDeLoteInexistenteLanzaExcepcion() {
        when(loteRepository.existsById(99L)).thenReturn(false);

        assertThrows(com.constructora_backend.exception.ResourceNotFoundException.class,
                () -> loteService.consultarHistorial(99L));
    }
}
