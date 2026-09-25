package com.constructora_backend.service;

import com.constructora_backend.dto.request.LoteRequestDTO;
import com.constructora_backend.dto.response.LoteResponseDTO;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.mapper.LoteMapper;
import com.constructora_backend.repository.EstadoLoteRepository;
import com.constructora_backend.repository.EtapaRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
                .collect(Collectors.toList());
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

        loteMapper.updateEntityFromDTO(dto, existente, etapa, estado, usuarioActualizar);
        Lote actualizado = loteRepository.save(existente);
        return loteMapper.toDTO(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!loteRepository.existsById(id)) {
            throw new com.constructora_backend.exception.ResourceNotFoundException("Lote no encontrado con ID: " + id);
        }
        loteRepository.deleteById(id);
    }
}
