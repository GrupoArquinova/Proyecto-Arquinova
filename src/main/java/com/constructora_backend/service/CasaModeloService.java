package com.constructora_backend.service;

import com.constructora_backend.dto.request.CasaModeloRequestDTO;
import com.constructora_backend.dto.response.CasaModeloResponseDTO;
import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.mapper.CasaModeloMapper;
import com.constructora_backend.repository.CasaModeloRepository;
import com.constructora_backend.repository.ProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CasaModeloService {

    @Autowired
    private CasaModeloRepository casaModeloRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private CasaModeloMapper casaModeloMapper;

    @Transactional(readOnly = true)
    public List<CasaModeloResponseDTO> listarPorProyectos(Long proyectoId) {
        return casaModeloRepository.findByProyectoId(proyectoId)
                .stream()
                .map(casaModeloMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CasaModeloResponseDTO> listarPublicadasYActivasPorProyecto(Long proyectoId) {
        return casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(proyectoId)
                .stream()
                .map(casaModeloMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<CasaModeloResponseDTO> obtenerPorId(Long id) {
        return casaModeloRepository.findById(id)
                .map(casaModeloMapper::toDTO);
    }

    @Transactional
    public CasaModeloResponseDTO guardar(CasaModeloRequestDTO dto) {
        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + dto.getProyectoId()));

        if (casaModeloRepository.existsByProyectoIdAndNombre(dto.getProyectoId(), dto.getNombre())) {
            throw new IllegalArgumentException("Ya existe una casa modelo con el nombre '" + dto.getNombre() + "' en este proyecto.");
        }

        CasaModelo casa = casaModeloMapper.toEntity(dto, proyecto);
        CasaModelo guardar = casaModeloRepository.save(casa);
        return casaModeloMapper.toDTO(guardar);
    }

    @Transactional
    public CasaModeloResponseDTO actualizar(Long id, CasaModeloRequestDTO dto) {
        CasaModelo existente = casaModeloRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Casa modelo no encontrada con ID: " + id));

        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + dto.getProyectoId()));

        if (casaModeloRepository.existsByProyectoIdAndNombreAndIdNot(dto.getProyectoId(), dto.getNombre(), id)) {
            throw new IllegalArgumentException("Ya existe otra casa modelo con el nombre '" + dto.getNombre() + "' en este proyecto.");
        }

        casaModeloMapper.updateEntityFromDTO(dto, existente, proyecto);
        CasaModelo actualizada = casaModeloRepository.save(existente);
        return casaModeloMapper.toDTO(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!casaModeloRepository.existsById(id)) {
            throw new RuntimeException("Casa modelo no encontrada con ID: " + id);
        }

        casaModeloRepository.deleteById(id);
    }
}
