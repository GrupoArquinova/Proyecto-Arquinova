package com.constructora_backend.service;

import com.constructora_backend.dto.request.AuditoriaRequestDTO;
import com.constructora_backend.dto.response.AuditoriaResponseDTO;
import com.constructora_backend.entity.Auditoria;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.AuditoriaMapper;
import com.constructora_backend.repository.AuditoriaRepository;
import com.constructora_backend.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaMapper auditoriaMapper;

    @Autowired
    public AuditoriaService(AuditoriaRepository auditoriaRepository,
                            UsuarioRepository usuarioRepository,
                            AuditoriaMapper auditoriaMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaMapper = auditoriaMapper;
    }

    public List<AuditoriaResponseDTO> listarTodas() {
        return auditoriaRepository.findAll()
                .stream()
                .map(auditoriaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<AuditoriaResponseDTO> obtenerPorId(Long id) {
        return auditoriaRepository.findById(id)
                .map(auditoriaMapper::toDTO);
    }

    public AuditoriaResponseDTO registrar(AuditoriaRequestDTO dto) {
        Usuario usuario = null;
        if (dto.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId()));
        }

        Auditoria auditoria = auditoriaMapper.toEntity(dto, usuario);
        Auditoria guardada = auditoriaRepository.save(auditoria);
        return auditoriaMapper.toDTO(guardada);
    }

    public List<AuditoriaResponseDTO> obtenerPorEntidad(String entidad, Long entidadId) {
        return auditoriaRepository.findByEntidadAndEntidadId(entidad, entidadId)
                .stream()
                .map(auditoriaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public AuditoriaResponseDTO registrarEvento(String accion, String entidad, Long entidadId, String descripcion, HttpServletRequest request) {
        Usuario usuario = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String correo = authentication.getName();
            usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        }

        String ip = null;
        String userAgent = null;
        if (request != null) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            userAgent = request.getHeader("User-Agent");
        }

        AuditoriaRequestDTO dto = new AuditoriaRequestDTO();
        dto.setAccion(accion);
        dto.setEntidad(entidad);
        dto.setEntidadId(entidadId);
        dto.setDescripcion(descripcion);
        dto.setIp(ip);
        dto.setUserAgent(userAgent);
        if (usuario != null) {
            dto.setUsuarioId(usuario.getId());
        }

        return registrar(dto);
    }
}
