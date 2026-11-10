package com.constructora_backend.service;

import com.constructora_backend.dto.request.LoteRequestDTO;
import com.constructora_backend.dto.response.HistorialEstadoLoteDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LoteService {

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private EstadoLoteRepository estadoLoteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LoteMapper loteMapper;

    @Autowired
    private HistorialEstadoLoteRepository historialEstadoLoteRepository;

    private static final Logger log = LoggerFactory.getLogger(LoteService.class);

    // --- NUEVO MÉTODO AGREGADO AQUÍ ---
    @Transactional(readOnly = true)
    public List<LoteResponseDTO> listarTodos() {
        return loteRepository.findAll()
                .stream()
                .map(loteMapper::toDTO)
                .collect(Collectors.toList());
    }
    // ----------------------------------

    @Transactional(readOnly = true)
    public List<LoteResponseDTO> listarPorEtapa(Long etapaId) {
        return loteRepository.findByEtapaId(etapaId)
                .stream()
                .map(loteMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoteResponseDTO> listarPublicadosYActivosPorEtapa(Long etapaId) {
        return loteRepository.findByEtapaIdAndPublicadoTrueAndActivoTrue(etapaId)
                .stream()
                .map(loteMapper::toDTO)
                .map(LoteService::sinDatosInternos)
                .collect(Collectors.toList());
    }

    /** Lotes visibles para el sitio público: solo publicados y activos, sin datos internos. */
    @Transactional(readOnly = true)
    public List<LoteResponseDTO> listarPublicadosYActivos() {
        return loteRepository.findByPublicadoTrueAndActivoTrue()
                .stream()
                .map(loteMapper::toDTO)
                .map(LoteService::sinDatosInternos)
                .collect(Collectors.toList());
    }

    /** Un lote para el sitio público: vacío si no existe, no está publicado o está inactivo. */
    @Transactional(readOnly = true)
    public Optional<LoteResponseDTO> obtenerPublicoPorId(Long id) {
        return loteRepository.findById(id)
                .filter(l -> Boolean.TRUE.equals(l.getPublicado()) && Boolean.TRUE.equals(l.getActivo()))
                .map(loteMapper::toDTO)
                .map(LoteService::sinDatosInternos);
    }

    /** Quita de la respuesta pública quién creó o editó el lote (datos del personal interno). */
    static LoteResponseDTO sinDatosInternos(LoteResponseDTO dto) {
        dto.setCreadoPorId(null);
        dto.setCreadoPorNombre(null);
        dto.setActualizadoPorId(null);
        dto.setActualizadoPorNombre(null);
        return dto;
    }

    @Transactional(readOnly = true)
    public Optional<LoteResponseDTO> obtenerPorId(Long id) {
        return loteRepository.findById(id)
                .map(loteMapper::toDTO);
    }

    @Transactional
    public LoteResponseDTO guardar(LoteRequestDTO dto) {
        Etapa etapa = etapaRepository.findById(dto.getEtapaId())
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Etapa no encontrada con ID: " + dto.getEtapaId()));

        EstadoLote estado = estadoLoteRepository.findById(dto.getEstadoId())
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Estado de lote no encontrado con ID: " + dto.getEstadoId()));

        if (loteRepository.existsByEtapaIdAndCodigo(dto.getEtapaId(), dto.getCodigo())) {
            throw new com.constructora_backend.exception.DuplicateResourceException("Ya existe un lote con el código '" + dto.getCodigo() + "' en esta etapa.");
        }

        Usuario usuarioCreador = null;
        if (dto.getUsuarioId() != null) {
            usuarioCreador = usuarioRepository.findById(dto.getUsuarioId()).orElse(null);
        }

        Lote lote = loteMapper.toEntity(dto, etapa, estado, usuarioCreador);
        Lote guardado = loteRepository.save(lote);
        return loteMapper.toDTO(guardado);
    }

    @Transactional
    public LoteResponseDTO actualizar(Long id, LoteRequestDTO dto) {
        Lote existente = loteRepository.findById(id)
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Lote no encontrado con ID: " + id));

        Etapa etapa = etapaRepository.findById(dto.getEtapaId())
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Etapa no encontrada con ID: " + dto.getEtapaId()));

        EstadoLote estado = estadoLoteRepository.findById(dto.getEstadoId())
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Estado de lote no encontrado con ID: " + dto.getEstadoId()));

        if (loteRepository.existsByEtapaIdAndCodigoAndIdNot(dto.getEtapaId(), dto.getCodigo(), id)) {
            throw new com.constructora_backend.exception.DuplicateResourceException("Ya existe otro lote con el código '" + dto.getCodigo() + "' en esta etapa.");
        }

        Usuario usuarioActualizar = null;
        if (dto.getUsuarioId() != null) {
            usuarioActualizar = usuarioRepository.findById(dto.getUsuarioId()).orElse(null);
        }

        EstadoLote estadoAnterior = existente.getEstado();
        loteMapper.updateEntityFromDTO(dto, existente, etapa, estado, usuarioActualizar);
        Lote actualizado = loteRepository.save(existente);
        if (!mismoEstado(estadoAnterior, estado)) {
            registrarHistorial(actualizado, estadoAnterior, estado, "Cambio de estado al editar el lote");
        }
        return loteMapper.toDTO(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!loteRepository.existsById(id)) {
            throw new com.constructora_backend.exception.ResourceNotFoundException("Lote no encontrado con ID: " + id);
        }
        loteRepository.deleteById(id);
    }

    /**
     * Cambia el estado comercial de un lote y registra el cambio en el historial.
     * Si el lote ya está en ese estado no hace nada (ni guarda ni registra historial).
     */
    @Transactional
    public LoteResponseDTO cambiarEstado(Long id, Integer estadoId, String observaciones) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Lote no encontrado con ID: " + id));
        EstadoLote nuevoEstado = estadoLoteRepository.findById(estadoId)
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Estado no encontrado con ID: " + estadoId));

        EstadoLote estadoAnterior = lote.getEstado();
        if (!mismoEstado(estadoAnterior, nuevoEstado)) {
            lote.setEstado(nuevoEstado);
            loteRepository.save(lote);
            registrarHistorial(lote, estadoAnterior, nuevoEstado, observaciones);
        }
        return loteMapper.toDTO(lote);
    }

    /** Historial de cambios de estado de un lote, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<HistorialEstadoLoteDTO> consultarHistorial(Long loteId) {
        if (!loteRepository.existsById(loteId)) {
            throw new com.constructora_backend.exception.ResourceNotFoundException("Lote no encontrado con ID: " + loteId);
        }
        return historialEstadoLoteRepository.findByLote_IdOrderByCambiadoEnDesc(loteId)
                .stream()
                .map(h -> HistorialEstadoLoteDTO.builder()
                        .id(h.getId())
                        .loteId(h.getLote().getId())
                        .estadoAnterior(h.getEstadoAnterior() != null ? h.getEstadoAnterior().getNombre() : "SIN ESTADO PREVIO")
                        .estadoNuevo(h.getEstadoNuevo().getNombre())
                        .usuarioNombre(h.getUsuario() != null ? h.getUsuario().getNombreCompleto() : "SISTEMA")
                        .observaciones(h.getObservaciones())
                        .cambiadoEn(h.getCambiadoEn())
                        .build())
                .toList();
    }

    private static boolean mismoEstado(EstadoLote a, EstadoLote b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }

    /**
     * Guarda una entrada en historial_estado_lote. La columna cambiado_por es obligatoria,
     * así que si no hay un usuario autenticado se omite (con un aviso en el log) en vez de fallar.
     */
    private void registrarHistorial(Lote lote, EstadoLote estadoAnterior, EstadoLote estadoNuevo, String observaciones) {
        Usuario usuario = usuarioAutenticado();
        if (usuario == null) {
            log.warn("Cambio de estado del lote {} sin usuario autenticado: no se registra en el historial", lote.getId());
            return;
        }
        historialEstadoLoteRepository.save(HistorialEstadoLote.builder()
                .lote(lote)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .usuario(usuario)
                .observaciones(observaciones)
                .build());
    }

    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return usuarioRepository.findByCorreo(auth.getName()).orElse(null);
    }

    @Transactional
    public void toggleActivo(Long id, Boolean activo) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Lote no encontrado con ID: " + id));
        lote.setActivo(activo);
        loteRepository.save(lote);
    }

}
