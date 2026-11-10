package com.constructora_backend.service;

import com.constructora_backend.dto.request.EstadoSolicitudRequestDTO;
import com.constructora_backend.dto.response.EstadoSolicitudResponseDTO;
import com.constructora_backend.entity.EstadoSolicitud;
import com.constructora_backend.mapper.EstadoSolicitudMapper;
import com.constructora_backend.repository.EstadoSolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EstadoSolicitudService {

    @Autowired
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @Autowired
    private EstadoSolicitudMapper estadoSolicitudMapper;

    @Transactional(readOnly = true)
    public List<EstadoSolicitudResponseDTO> listarTodos(){
        return estadoSolicitudRepository.findAllByOrderByOrdenAsc()
                .stream()
                .map(estadoSolicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EstadoSolicitudResponseDTO> listarActivos() {
        return estadoSolicitudRepository.findByActivoTrueOrderByOrdenAsc()
                .stream()
                .map(estadoSolicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<EstadoSolicitudResponseDTO> obtenerPorId(Integer id){
        return estadoSolicitudRepository.findById(id)
                .map(estadoSolicitudMapper::toDTO);
    }

    @Transactional
    public EstadoSolicitudResponseDTO guardar(EstadoSolicitudRequestDTO dto) {
        String nombreTrim = dto.getNombre().trim();
        if (estadoSolicitudRepository.existsByNombre(nombreTrim)) {
            throw new com.constructora_backend.exception.DuplicateResourceException("Ya existe un estado de solicitud con el nombre '" + nombreTrim + "'.");
        }

        EstadoSolicitud estado = estadoSolicitudMapper.toEntity(dto);
        EstadoSolicitud guardado = estadoSolicitudRepository.save(estado);
        return estadoSolicitudMapper.toDTO(guardado);
    }

    @Transactional
    public EstadoSolicitudResponseDTO actualizar(Integer id, EstadoSolicitudRequestDTO dto) {
        EstadoSolicitud existente = estadoSolicitudRepository.findById(id)
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Estado de solicitud no encontrado con ID: " + id));

        String nombreTrim = dto.getNombre().trim();
        if (estadoSolicitudRepository.existsByNombreAndIdNot(nombreTrim, id)) {
            throw new com.constructora_backend.exception.DuplicateResourceException("Ya existe otro estado de solicitud con el nombre '" + nombreTrim + "'.");
        }

        estadoSolicitudMapper.updateEntityFromDTO(dto, existente);
        EstadoSolicitud actualizado = estadoSolicitudRepository.save(existente);
        return estadoSolicitudMapper.toDTO(actualizado);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!estadoSolicitudRepository.existsById(id)) {
            throw new com.constructora_backend.exception.ResourceNotFoundException("Estado de solicitud no encontrado con ID: " + id);
        }
        estadoSolicitudRepository.deleteById(id);
    }
}
