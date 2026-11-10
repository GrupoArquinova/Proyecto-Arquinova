package com.constructora_backend.service;

import com.constructora_backend.dto.request.EmpresaRequestDTO;
import com.constructora_backend.dto.response.EmpresaResponseDTO;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.exception.DuplicateResourceException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.EmpresaMapper;
import com.constructora_backend.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaMapper empresaMapper;

    @Autowired
    public EmpresaService(EmpresaRepository empresaRepository, EmpresaMapper empresaMapper) {
        this.empresaRepository = empresaRepository;
        this.empresaMapper = empresaMapper;
    }

    public List<EmpresaResponseDTO> listarTodas() {
        return empresaRepository.findAll()
                .stream()
                .map(empresaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<EmpresaResponseDTO> listarActivas() {
        return empresaRepository.findByActivoTrue()
                .stream()
                .map(empresaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<EmpresaResponseDTO> obtenerPorId(Long id) {
        return empresaRepository.findById(id)
                .map(empresaMapper::toDTO);
    }

    public EmpresaResponseDTO guardar(EmpresaRequestDTO dto) {
        if (dto.getNit() != null && !dto.getNit().isBlank() && empresaRepository.existsByNit(dto.getNit())) {
            throw new DuplicateResourceException("Ya existe una empresa registrada con el NIT: " + dto.getNit());
        }
        Empresa empresa = empresaMapper.toEntity(dto);
        Empresa guardada = empresaRepository.save(empresa);
        return empresaMapper.toDTO(guardada);
    }

    public EmpresaResponseDTO actualizar(Long id, EmpresaRequestDTO dto) {
        Empresa existente = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + id));

        if (dto.getNit() != null && !dto.getNit().isBlank() && !dto.getNit().equals(existente.getNit())
                && empresaRepository.existsByNit(dto.getNit())) {
            throw new DuplicateResourceException("El NIT " + dto.getNit() + " ya pertenece a otra empresa registrada");
        }

        empresaMapper.updateEntityFromDTO(dto, existente);
        Empresa actualizada = empresaRepository.save(existente);
        return empresaMapper.toDTO(actualizada);
    }

    public void desactivar(Long id) {
        Empresa existente = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + id));
        existente.setActivo(false);
        empresaRepository.save(existente);
    }
}
