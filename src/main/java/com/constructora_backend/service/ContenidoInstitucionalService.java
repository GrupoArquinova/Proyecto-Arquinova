package com.constructora_backend.service;

import com.constructora_backend.dto.request.ContenidoInstitucionalRequestDTO;
import com.constructora_backend.dto.response.ContenidoInstitucionalResponseDTO;
import com.constructora_backend.entity.ContenidoInstitucional;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.DuplicateResourceException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.ContenidoInstitucionalMapper;
import com.constructora_backend.repository.ContenidoInstitucionalRepository;
import com.constructora_backend.repository.EmpresaRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContenidoInstitucionalService {

    private final ContenidoInstitucionalRepository contenidoRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContenidoInstitucionalMapper contenidoMapper;

    @Autowired
    public ContenidoInstitucionalService(ContenidoInstitucionalRepository contenidoRepository,
                                          EmpresaRepository empresaRepository,
                                          UsuarioRepository usuarioRepository,
                                          ContenidoInstitucionalMapper contenidoMapper) {
        this.contenidoRepository = contenidoRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.contenidoMapper = contenidoMapper;
    }

    // ───── Listar todos ─────
    public List<ContenidoInstitucionalResponseDTO> listarTodos() {
        return contenidoRepository.findAll()
                .stream()
                .map(contenidoMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ───── Listar por empresa ─────
    public List<ContenidoInstitucionalResponseDTO> listarPorEmpresa(Long empresaId) {
        validarEmpresaExiste(empresaId);
        return contenidoRepository.findByEmpresaId(empresaId)
                .stream()
                .map(contenidoMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ───── Listar publicados por empresa (para vista pública) ─────
    public List<ContenidoInstitucionalResponseDTO> listarPublicadosPorEmpresa(Long empresaId) {
        validarEmpresaExiste(empresaId);
        return contenidoRepository.findByEmpresaIdAndPublicadoTrue(empresaId)
                .stream()
                .map(contenidoMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ───── Obtener por ID ─────
    public Optional<ContenidoInstitucionalResponseDTO> obtenerPorId(Long id) {
        return contenidoRepository.findById(id)
                .map(contenidoMapper::toDTO);
    }

    // ───── Obtener por empresa y sección ─────
    public Optional<ContenidoInstitucionalResponseDTO> obtenerPorEmpresaYSeccion(Long empresaId, String seccion) {
        validarEmpresaExiste(empresaId);
        return contenidoRepository.findByEmpresaIdAndSeccion(empresaId, seccion)
                .map(contenidoMapper::toDTO);
    }

    // ───── Crear ─────
    public ContenidoInstitucionalResponseDTO guardar(ContenidoInstitucionalRequestDTO dto) {
        Empresa empresa = resolverEmpresa(dto.getEmpresaId());
        Usuario usuario = resolverUsuario(dto.getActualizadoPorId());

        // Validar unicidad empresa + sección
        if (contenidoRepository.existsByEmpresaIdAndSeccion(dto.getEmpresaId(), dto.getSeccion())) {
            throw new DuplicateResourceException(
                "Ya existe un contenido para la empresa ID " + dto.getEmpresaId()
                + " con la sección: " + dto.getSeccion());
        }

        ContenidoInstitucional entidad = contenidoMapper.toEntity(dto, empresa, usuario);
        ContenidoInstitucional guardado = contenidoRepository.save(entidad);
        return contenidoMapper.toDTO(guardado);
    }

    // ───── Actualizar ─────
    public ContenidoInstitucionalResponseDTO actualizar(Long id, ContenidoInstitucionalRequestDTO dto) {
        ContenidoInstitucional existente = contenidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido institucional no encontrado con ID: " + id));

        Empresa empresa = resolverEmpresa(dto.getEmpresaId());
        Usuario usuario = resolverUsuario(dto.getActualizadoPorId());

        // Validar que la combinación empresa+sección no pertenezca a otro registro
        if (!existente.getEmpresa().getId().equals(dto.getEmpresaId())
                || !existente.getSeccion().equals(dto.getSeccion())) {
            if (contenidoRepository.existsByEmpresaIdAndSeccion(dto.getEmpresaId(), dto.getSeccion())) {
                throw new DuplicateResourceException(
                    "Ya existe un contenido para la empresa ID " + dto.getEmpresaId()
                    + " con la sección: " + dto.getSeccion());
            }
        }

        contenidoMapper.updateEntityFromDTO(dto, existente, empresa, usuario);
        ContenidoInstitucional actualizado = contenidoRepository.save(existente);
        return contenidoMapper.toDTO(actualizado);
    }

    // ───── Eliminar ─────
    public void eliminar(Long id) {
        ContenidoInstitucional existente = contenidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido institucional no encontrado con ID: " + id));
        contenidoRepository.delete(existente);
    }

    // ───── Métodos privados de resolución ─────

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
