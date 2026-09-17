package com.constructora_backend.service;

import com.constructora_backend.dto.request.UbicacionRequestDTO;
import com.constructora_backend.dto.response.UbicacionResponseDTO;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.exception.DuplicateResourceException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.UbicacionMapper;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.UbicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UbicacionService {

    private final UbicacionRepository ubicacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final UbicacionMapper ubicacionMapper;

    public UbicacionService(UbicacionRepository ubicacionRepository, ProyectoRepository proyectoRepository, UbicacionMapper ubicacionMapper) {
        this.ubicacionRepository = ubicacionRepository;
        this.proyectoRepository = proyectoRepository;
        this.ubicacionMapper = ubicacionMapper;
    }

    public List<UbicacionResponseDTO> listarTodas() {
        return ubicacionRepository.findAll()
                .stream()
                .map(ubicacionMapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<UbicacionResponseDTO> obtenerPorId(Long id) {
        return ubicacionRepository.findById(id)
                .map(ubicacionMapper::toDto);
    }

    public Optional<UbicacionResponseDTO> obtenerPorProyectoId(Long proyectoId) {
        return ubicacionRepository.findByProyectoId(proyectoId)
                .map(ubicacionMapper::toDto);
    }

    public List<UbicacionResponseDTO> listarPorCiudad(String ciudad) {
        return ubicacionRepository.findByCiudadIgnoreCase(ciudad)
                .stream()
                .map(ubicacionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UbicacionResponseDTO guardar(UbicacionRequestDTO dto) {
        if (ubicacionRepository.existsByProyectoId(dto.getProyectoId())) {
            throw new DuplicateResourceException("El proyecto con ID " + dto.getProyectoId() + " ya tiene una ubicación registrada.");
        }

        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + dto.getProyectoId()));

        Ubicacion ubicacion = ubicacionMapper.toEntity(dto, proyecto);
        Ubicacion guardada = ubicacionRepository.save(ubicacion);
        return ubicacionMapper.toDto(guardada);
    }

    @Transactional
    public UbicacionResponseDTO actualizar(Long id, UbicacionRequestDTO dto) {
        Ubicacion existente = ubicacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ubicación no encontrada con ID: " + id));

        if (!existente.getProyecto().getId().equals(dto.getProyectoId()) &&
        ubicacionRepository.existsByProyectoId(dto.getProyectoId())) {
            throw new DuplicateResourceException("El proyecto con ID " + dto.getProyectoId() + " ya tiene una ubicación asignada.");
        }

        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + dto.getProyectoId()));

        ubicacionMapper.updateEntityFromDTO(dto, existente, proyecto);
        Ubicacion actualizada = ubicacionRepository.save(existente);
        return ubicacionMapper.toDto(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!ubicacionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ubicación no encontrada con ID: " + id);
        }
        ubicacionRepository.deleteById(id);
    }
}
