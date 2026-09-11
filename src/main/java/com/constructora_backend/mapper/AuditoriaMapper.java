package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.AuditoriaRequestDTO;
import com.constructora_backend.dto.response.AuditoriaResponseDTO;
import com.constructora_backend.entity.Auditoria;
import com.constructora_backend.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaMapper {

    public AuditoriaResponseDTO toDTO(Auditoria auditoria) {
        if (auditoria == null) return null;

        AuditoriaResponseDTO dto = new AuditoriaResponseDTO();
        dto.setId(auditoria.getId());
        dto.setAccion(auditoria.getAccion());
        dto.setEntidad(auditoria.getEntidad());
        dto.setEntidadId(auditoria.getEntidadId());
        dto.setDescripcion(auditoria.getDescripcion());
        dto.setIp(auditoria.getIp());
        dto.setUserAgent(auditoria.getUserAgent());
        dto.setCreadoEn(auditoria.getCreadoEn());

        if (auditoria.getUsuario() != null){
            dto.setUsuarioId(auditoria.getUsuario().getId());
            dto.setUsuarioNombre(auditoria.getUsuario().getNombreCompleto());
        }

        return dto;
    }

    public Auditoria toEntity(AuditoriaRequestDTO dto, Usuario usuario) {
        if (dto == null) return null;

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setAccion(dto.getAccion());
        auditoria.setEntidad(dto.getEntidad());
        auditoria.setEntidadId(dto.getEntidadId());
        auditoria.setDescripcion(dto.getDescripcion());
        auditoria.setIp(dto.getIp());
        auditoria.setUserAgent(dto.getUserAgent());

        return auditoria;
    }
}
