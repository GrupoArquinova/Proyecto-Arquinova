package com.constructora_backend.service;

import com.constructora_backend.dto.SolicitudContactoAtencionDTO;
import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.entity.*;
import com.constructora_backend.mapper.SolicitudContactoMapper;
import com.constructora_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SolicitudContactoService {

    private static final Integer ESTADO_NUEVA = 1;

    @Autowired
    private SolicitudContactoRepository solicitudRepository;

    @Autowired
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudContactoMapper solicitudMapper;

    @Transactional(readOnly = true)
    public List<SolicitudContactoResponseDTO> listarTodas() {
        return solicitudRepository.findAllByOrderByCreadoEnDesc()
                .stream()
                .map(solicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoResponseDTO> listarPorEstado(Integer estadoId){
        return solicitudRepository.findByEstadoIdOrderByCreadoEnDesc(estadoId)
                .stream()
                .map(solicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoResponseDTO> listarPorProyecto(Long proyectoId){
        return solicitudRepository.findByProyectoIdOrderByCreadoEnDesc(proyectoId)
                .stream()
                .map(solicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<SolicitudContactoResponseDTO> obtenerPorId(Long id) {
        return solicitudRepository.findById(id)
                .map(solicitudMapper::toDTO);
    }

    @Transactional
    public SolicitudContactoResponseDTO crearPublica(SolicitudContactoPublicDTO dto){
        EstadoSolicitud estadoInicial = estadoSolicitudRepository.findById(ESTADO_NUEVA)
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Estado de solicitud 'NUEVA' no encontrado con ID: " + ESTADO_NUEVA));

        Proyecto proyecto = null;
        if (dto.getProyectoId() != null){
            proyecto = proyectoRepository.findById(dto.getProyectoId())
                    .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Proyecto no encontrado con ID: " + dto.getProyectoId()));
        }

        Lote lote = null;
        if (dto.getLoteId() != null){
            lote = loteRepository.findById(dto.getLoteId())
                    .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Lote no encontrado con ID: "+ dto.getLoteId()));
        }

        SolicitudContacto solicitud = solicitudMapper.toEntity(dto, estadoInicial, proyecto, lote);
        SolicitudContacto guardada = solicitudRepository.save(solicitud);
        return solicitudMapper.toDTO(guardada);
    }

    @Transactional
    public SolicitudContactoResponseDTO atenderSolicitud(Long id, SolicitudContactoAtencionDTO dto) {
        SolicitudContacto existente = solicitudRepository.findById(id)
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Solicitud de contacto no encontrada con ID: " + id));

        EstadoSolicitud nuevoEstado = estadoSolicitudRepository.findById(dto.getEstadoId())
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Estado de solicitud no encontrado con ID: " + dto.getEstadoId()));

        existente.setEstado(nuevoEstado);
        existente.setObservacionesInternas(dto.getObservacionesInternas());

        if (dto.getAtendidaPorId() != null) {
            Usuario usuario = usuarioRepository.findById(dto.getAtendidaPorId())
                    .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getAtendidaPorId()));
            existente.setAtendidaPor(usuario);
        }

        if (existente.getAtendidaEn() == null) {
            existente.setAtendidaEn(LocalDateTime.now());
        }

        SolicitudContacto actualizada = solicitudRepository.save(existente);
        return solicitudMapper.toDTO(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!solicitudRepository.existsById(id)) {
            throw new com.constructora_backend.exception.ResourceNotFoundException("Solicitud de contacto no encontrada con ID: " + id);
        }
        solicitudRepository.deleteById(id);
    }
}
