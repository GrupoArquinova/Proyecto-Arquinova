package com.constructora_backend.service;

import com.constructora_backend.dto.request.EtapaRequestDTO;
import com.constructora_backend.dto.response.EtapaResponseDTO;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.mapper.EtapaMapper;
import com.constructora_backend.repository.EtapaRepository;
import com.constructora_backend.repository.ProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EtapaService {

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private EtapaMapper etapaMapper;

    public List<EtapaResponseDTO> listarTodas() {
        return etapaRepository.findAll()
                .stream()
                .map(etapaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<EtapaResponseDTO> obtenerPorId(Long id) {
        return etapaRepository.findById(id)
                .map(etapaMapper::toDTO);
    }

    public List<EtapaResponseDTO> listarPorProyecto(Long proyectoId) {
        return etapaRepository.findByProyectoIdOrderByOrdenAsc(proyectoId)
                .stream()
                .map(etapaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<EtapaResponseDTO> listarActivasPorProyecto(Long proyectoId) {
        return etapaRepository.findByProyectoIdAndActivoTrueOrderByOrdenAsc(proyectoId)
                .stream()
                .map(etapaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public EtapaResponseDTO guardar(EtapaRequestDTO dto) {
        String nombreTrim = dto.getNombre().trim();
        if (etapaRepository.existsByProyectoIdAndNombre(dto.getProyectoId(), nombreTrim)) {
            throw new RuntimeException("Ya existe una etapa con el nombre '" + nombreTrim + "' para este proyecto.");
        }

        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + dto.getProyectoId()));

        Etapa etapa = etapaMapper.toEntity(dto, proyecto);
        Etapa guardada = etapaRepository.save(etapa);
        return etapaMapper.toDTO(guardada);
    }

    @Transactional
    public EtapaResponseDTO actualizar(Long id, EtapaRequestDTO dto) {
        Etapa existente = etapaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etapa no encontrada con ID:" + id));

        String nombreTrim = dto.getNombre().trim();
        if (etapaRepository.existsByProyectoIdAndNombreAndIdNot(dto.getProyectoId(), nombreTrim, id)) {
            throw new RuntimeException("Ya existe otra etapa con el nombre '" + nombreTrim + "' en este proyecto.");
        }

        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + dto.getProyectoId()));

        etapaMapper.updateEntityFromDTO(dto, existente, proyecto);
        Etapa actualizada = etapaRepository.save(existente);
        return etapaMapper.toDTO(actualizada);
    }

    @Transactional
    public void desactivar(Long id) {
        Etapa existente = etapaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etapa no encontrada con ID:" + id));
        existente.setActivo(false);
        etapaRepository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!etapaRepository.existsById(id)) {
            throw new RuntimeException("Etapa no encontrada con ID: " + id);
        }
        etapaRepository.deleteById(id);
    }
}
