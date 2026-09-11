package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.ContenidoInstitucionalRequestDTO;
import com.constructora_backend.dto.response.ContenidoInstitucionalResponseDTO;
import com.constructora_backend.entity.ContenidoInstitucional;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class ContenidoInstitucionalMapper {

    public ContenidoInstitucionalResponseDTO toDTO(ContenidoInstitucional entidad) {
        if (entidad == null) return null;

        ContenidoInstitucionalResponseDTO dto = new ContenidoInstitucionalResponseDTO();
        dto.setId(entidad.getId());
        dto.setSeccion(entidad.getSeccion());
        dto.setTitulo(entidad.getTitulo());
        dto.setContenido(entidad.getContenido());
        dto.setPublicado(entidad.getPublicado());
        dto.setCreadoEn(entidad.getCreadoEn());
        dto.setActualizadoEn(entidad.getActualizadoEn());

        if (entidad.getEmpresa() != null) {
            dto.setEmpresaId(entidad.getEmpresa().getId());
            dto.setEmpresaNombre(entidad.getEmpresa().getNombre());
        }

        if (entidad.getActualizadoPor() != null) {
            dto.setActualizadoPorId(entidad.getActualizadoPor().getId());
            dto.setActualizadoPorNombre(entidad.getActualizadoPor().getNombreCompleto());
        }

        return dto;
    }

    public ContenidoInstitucional toEntity(ContenidoInstitucionalRequestDTO dto, Empresa empresa, Usuario actualizadoPor) {
        if (dto == null) return null;

        ContenidoInstitucional entidad = new ContenidoInstitucional();
        entidad.setEmpresa(empresa);
        entidad.setSeccion(dto.getSeccion());
        entidad.setTitulo(dto.getTitulo());
        entidad.setContenido(dto.getContenido());
        entidad.setActualizadoPor(actualizadoPor);

        if (dto.getPublicado() != null) {
            entidad.setPublicado(dto.getPublicado());
        }

        return entidad;
    }

    public void updateEntityFromDTO(ContenidoInstitucionalRequestDTO dto, ContenidoInstitucional entidad,
                                     Empresa empresa, Usuario actualizadoPor) {
        if (dto == null || entidad == null) return;

        entidad.setEmpresa(empresa);
        entidad.setSeccion(dto.getSeccion());
        entidad.setTitulo(dto.getTitulo());
        entidad.setContenido(dto.getContenido());
        entidad.setActualizadoPor(actualizadoPor);

        if (dto.getPublicado() != null) {
            entidad.setPublicado(dto.getPublicado());
        }
    }
}
