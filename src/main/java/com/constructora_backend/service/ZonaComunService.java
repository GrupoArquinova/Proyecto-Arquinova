package com.constructora_backend.service;

import com.constructora_backend.dto.request.ZonaComunRequestDTO;
import com.constructora_backend.dto.response.ZonaComunResponseDTO;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.exception.DuplicateResourceException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.ZonaComunMapper;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.ZonaComunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ZonaComunService {

    @Autowired
    private ZonaComunRepository zonaComunRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private ZonaComunMapper zonaComunMapper;

    @Transactional(readOnly = true)
    public List<ZonaComunResponseDTO> listarPorProyecto(Long proyectoId) {
        return zonaComunRepository.findByProyectoId(proyectoId)
                .stream()
                .map(zonaComunMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ZonaComunResponseDTO> listarPublicadasYActivasPorProyectos(Long proyectoId) {
        return zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(proyectoId)
                .stream()
                .map(zonaComunMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ZonaComunResponseDTO> obtenerPorId(Long id) {
        return zonaComunRepository.findById(id)
                .map(zonaComunMapper::toDTO);
    }

    @Transactional
    public ZonaComunResponseDTO guardar(ZonaComunRequestDTO dto) {
        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + dto.getProyectoId()));

        if (zonaComunRepository.existsByProyectoIdAndNombre(dto.getProyectoId(), dto.getNombre())) {
            throw new DuplicateResourceException("Ya existe una zona común con el nombre '" + dto.getNombre() + "' en este proyecto.");
        }

        ZonaComun zona = zonaComunMapper.toEntity(dto, proyecto);
        ZonaComun guardada = zonaComunRepository.save(zona);
        return zonaComunMapper.toDTO(guardada);
    }

    @Transactional
    public ZonaComunResponseDTO actualizar(Long id, ZonaComunRequestDTO dto) {
        ZonaComun existente = zonaComunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zona común no encontrada con ID: " + id));

        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + dto.getProyectoId()));

        if (zonaComunRepository.existsByProyectoIdAndNombreAndIdNot(dto.getProyectoId(), dto.getNombre(), id)) {
            throw new DuplicateResourceException("Ya existe otra zona común con el nombre '" + dto.getNombre() + "' en este proyecto.");
        }

        zonaComunMapper.updateEntityFromDTO(dto, existente, proyecto);
        ZonaComun actualizada = zonaComunRepository.save(existente);
        return zonaComunMapper.toDTO(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!zonaComunRepository.existsById(id)) {
            throw new ResourceNotFoundException("Zona común no encontrada con ID: " + id);
        }
        zonaComunRepository.deleteById(id);
    }
}