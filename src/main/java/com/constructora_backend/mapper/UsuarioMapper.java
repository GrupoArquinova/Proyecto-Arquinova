package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.UsuarioRequestDTO;
import com.constructora_backend.dto.request.UsuarioUpdateDTO;
import com.constructora_backend.dto.response.UsuarioResponseDTO;
import com.constructora_backend.entity.Rol;
import com.constructora_backend.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponseDTO toDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(usuario.getId());
        dto.setNombreCompleto(usuario.getNombreCompleto());
        dto.setCorreo(usuario.getCorreo());
        dto.setRolId(usuario.getRol() != null ? usuario.getRol().getId() : null);
        dto.setActivo(usuario.getActivo());
        dto.setUltimoAccesoEn(usuario.getUltimoAccesoEn());
        dto.setCreadoEn(usuario.getCreadoEn());
        dto.setActualizadoEn(usuario.getActualizadoEn());
        return dto;
    }

    public Usuario toEntity(UsuarioRequestDTO dto, Rol rol) {
        if (dto == null) {
            return null;
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setCorreo(dto.getCorreo());
        usuario.setPasswordHash(dto.getPassword());
        usuario.setRol(rol);
        if (dto.getActivo() != null) {
            usuario.setActivo(dto.getActivo());
        }
        return usuario;
    }

    public void updateEntityFromDTO(UsuarioUpdateDTO dto, Usuario usuario, Rol rol) {
        if (dto == null || usuario == null) {
            return;
        }
        if (dto.getNombreCompleto() != null && !dto.getNombreCompleto().isBlank()) {
            usuario.setNombreCompleto(dto.getNombreCompleto());
        }
        if (dto.getCorreo() != null && !dto.getCorreo().isBlank()) {
            usuario.setCorreo(dto.getCorreo());
        }
        if (rol != null) {
            usuario.setRol(rol);
        }
        if (dto.getActivo() != null) {
            usuario.setActivo(dto.getActivo());
        }
    }
}