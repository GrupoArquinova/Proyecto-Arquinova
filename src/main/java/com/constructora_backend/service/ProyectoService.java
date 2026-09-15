package com.constructora_backend.service;

import com.constructora_backend.dto.request.ProyectoRequestDTO;
import com.constructora_backend.dto.response.ProyectoResponseDTO;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.DuplicateResourceException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.ProyectoMapper;
import com.constructora_backend.repository.EmpresaRepository;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoMapper proyectoMapper;

    @Autowired
    public ProyectoService(ProyectoRepository proyectoRepository,
                           EmpresaRepository empresaRepository,
                           UsuarioRepository usuarioRepository,
                           ProyectoMapper proyectoMapper) {
        this.proyectoRepository = proyectoRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.proyectoMapper = proyectoMapper;
    }

    public List<ProyectoResponseDTO> listarTodos() {
        return proyectoRepository.findAll()
                .stream()
                .map(proyectoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProyectoResponseDTO> listarPublicados() {
        return proyectoRepository.findByPublicadoTrueAndActivoTrue()
                .stream()
                .map(proyectoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProyectoResponseDTO> listarPorEmpresa(Long empresaId) {
        validarEmpresaExiste(empresaId);
        return proyectoRepository.findByEmpresaId(empresaId)
                .stream()
                .map(proyectoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ProyectoResponseDTO> obtenerPorId(Long id) {
        return proyectoRepository.findById(id)
                .map(proyectoMapper::toDTO);
    }

    public Optional<ProyectoResponseDTO> obtenerPorEmpresaYSlug(Long empresaId, String slug) {
        validarEmpresaExiste(empresaId);
        return proyectoRepository.findByEmpresaIdAndSlug(empresaId, slug.toLowerCase().trim())
                .map(proyectoMapper::toDTO);
    }

    public ProyectoResponseDTO guardar(ProyectoRequestDTO dto) {
        Empresa empresa = resolverEmpresa(dto.getEmpresaId());

        String slugFormateado = dto.getSlug().toLowerCase().trim();
        if (proyectoRepository.existsByEmpresaIdAndSlug(dto.getEmpresaId(), slugFormateado)) {
            throw new DuplicateResourceException("Ya existe un proyecto con el slug '" + slugFormateado + "' para esta empresa.");
        }

        Usuario creadoPor = resolverUsuario(dto.getCreadoPorId());
        Usuario actualizadoPor = resolverUsuario(dto.getActualizadoPorId());

        Proyecto proyecto = proyectoMapper.toEntity(dto, empresa, creadoPor, actualizadoPor != null ? actualizadoPor : creadoPor);
        Proyecto guardado = proyectoRepository.save(proyecto);
        return proyectoMapper.toDTO(guardado);
    }

    public ProyectoResponseDTO actualizar(Long id, ProyectoRequestDTO dto) {
        Proyecto existente = proyectoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + id));

        Empresa empresa = resolverEmpresa(dto.getEmpresaId());

        String slugFormateado = dto.getSlug().toLowerCase().trim();
        if ((!existente.getEmpresa().getId().equals(dto.getEmpresaId()) || !existente.getSlug().equalsIgnoreCase(slugFormateado))
                && proyectoRepository.existsByEmpresaIdAndSlug(dto.getEmpresaId(), slugFormateado)) {
            throw new DuplicateResourceException("El slug '" + slugFormateado + "' ya pertenece a otro proyecto de esta empresa.");
        }

        Usuario actualizadoPor = resolverUsuario(dto.getActualizadoPorId());

        proyectoMapper.updateEntityFromDTO(dto, existente, empresa, actualizadoPor);
        Proyecto actualizado = proyectoRepository.save(existente);
        return proyectoMapper.toDTO(actualizado);
    }

    public void desactivar(Long id) {
        Proyecto existente = proyectoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + id));
        existente.setActivo(false);
        proyectoRepository.save(existente);
    }

    private Empresa resolverEmpresa(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + empresaId));
    }

    private Usuario resolverUsuario(Long usuarioId) {
        if (usuarioId == null) return null;
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));
    }

    private void validarEmpresaExiste(Long empresaId) {
        if (!empresaRepository.existsById(empresaId)) {
            throw new ResourceNotFoundException("Empresa no encontrada con ID: " + empresaId);
        }
    }
}
