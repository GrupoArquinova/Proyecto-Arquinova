package com.constructora_backend.service;

import com.constructora_backend.dto.request.EstadoLoteRequestDTO;
import com.constructora_backend.dto.response.EstadoLoteResponseDTO;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.mapper.EstadoLoteMapper;
import com.constructora_backend.repository.EstadoLoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EstadoLoteService {

    @Autowired
    private EstadoLoteRepository estadoLoteRepository;

    @Autowired
    private EstadoLoteMapper estadoLoteMapper;

    @Transactional(readOnly = true)
    public List<EstadoLoteResponseDTO> listarTodos() {
        return estadoLoteRepository.findAllByOrderByOrdenAsc()
                .stream()
                .map(estadoLoteMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EstadoLoteResponseDTO> listarActivos() {
        return estadoLoteRepository.findByActivoTrueOrderByOrdenAsc()
                .stream()
                .map(estadoLoteMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<EstadoLoteResponseDTO> obtenerPorId(Integer id) {
        return estadoLoteRepository.findById(id)
                .map(estadoLoteMapper::toDTO);
    }

    @Transactional
    public EstadoLoteResponseDTO guardar(EstadoLoteRequestDTO dto) {
        if (estadoLoteRepository.existsByNombre(dto.getNombre())) {
            throw new IllegalArgumentException("Ya existe un estado de lote con el nombre '" + dto.getNombre() + "'.");
        }

        EstadoLote estado = estadoLoteMapper.toEntity(dto);
        EstadoLote guardado = estadoLoteRepository.save(estado);
        return estadoLoteMapper.toDTO(guardado);
    }

    @Transactional
    public EstadoLoteResponseDTO actualizar(Integer id, EstadoLoteRequestDTO dto) {
        EstadoLote existente = estadoLoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de lote no encontrado con ID: " + id));

        if (estadoLoteRepository.existsByNombreAndIdNot(dto.getNombre(), id)) {
            throw new IllegalArgumentException("Ya existe otro estado de lote con el nombre '" + dto.getNombre() + "'.");
        }

        estadoLoteMapper.updateEntityFromDTO(dto, existente);
        EstadoLote actualizado = estadoLoteRepository.save(existente);
        return estadoLoteMapper.toDTO(actualizado);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!estadoLoteRepository.existsById(id)) {
            throw new RuntimeException("Estado de lote no encontrado con ID: " + id);
        }
        estadoLoteRepository.deleteById(id);
    }
}
