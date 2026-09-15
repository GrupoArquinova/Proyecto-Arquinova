package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.ProyectoRequestDTO;
import com.constructora_backend.dto.response.ProyectoResponseDTO;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class ProyectoMapper {

    public ProyectoResponseDTO toDTO(Proyecto proyecto) {
        if (proyecto == null) return null;

        ProyectoResponseDTO dto = new ProyectoResponseDTO();
        dto.setId(proyecto.getId());
        dto.setNombre(proyecto.getNombre());
        dto.setSlug(proyecto.getSlug());
        dto.setDescripcion(proyecto.getDescripcion());
        dto.setEstadoProyecto(proyecto.getEstadoProyecto());
        dto.setPublicado(proyecto.getPublicado());
        dto.setActivo(proyecto.getActivo());
        dto.setFechaLanzamiento(proyecto.getFechaLanzamiento());
        dto.setCreadoEn(proyecto.getCreadoEn());
        dto.setActualizadoEn(proyecto.getActualizadoEn());

        if (proyecto.getEmpresa() != null) {
            dto.setEmpresaId(proyecto.getEmpresa().getId());
            dto.setEmpresaNombre(proyecto.getEmpresa().getNombre());
        }

        if (proyecto.getCreadoPor() != null) {
            dto.setCreadoPorId(proyecto.getCreadoPor().getId());
            dto.setCreadoPorNombre(proyecto.getCreadoPor().getNombreCompleto());
        }

        if (proyecto.getActualizadoPor() != null) {
            dto.setActualizadoPorId(proyecto.getActualizadoPor().getId());
            dto.setActualizadoPorNombre(proyecto.getActualizadoPor().getNombreCompleto());
        }

        return dto;
    }

    public Proyecto toEntity(ProyectoRequestDTO dto, Empresa empresa, Usuario creadoPor, Usuario actualizadoPor) {
        if (dto == null) return null;

        Proyecto proyecto = new Proyecto();
        proyecto.setEmpresa(empresa);
        proyecto.setNombre(dto.getNombre());
        proyecto.setSlug(dto.getSlug().toLowerCase().trim());
        proyecto.setDescripcion(dto.getDescripcion());
        if (dto.getEstadoProyecto() != null) {
            proyecto.setEstadoProyecto(dto.getEstadoProyecto());
        }
        if (dto.getPublicado() != null) {
            proyecto.setPublicado(dto.getPublicado());
        }
        if (dto.getActivo() != null) {
            proyecto.setActivo(dto.getActivo());
        }
        proyecto.setFechaLanzamiento(dto.getFechaLanzamiento());
        proyecto.setCreadoPor(creadoPor);
        proyecto.setActualizadoPor(actualizadoPor);

        return proyecto;
    }

    public void updateEntityFromDTO(ProyectoRequestDTO dto, Proyecto proyecto, Empresa empresa, Usuario actualizadoPor) {
        if (dto == null || proyecto == null) return;

        proyecto.setEmpresa(empresa);
        proyecto.setNombre(dto.getNombre());
        proyecto.setSlug(dto.getSlug().toLowerCase().trim());
        proyecto.setDescripcion(dto.getDescripcion());
        if (dto.getEstadoProyecto() != null) {
            proyecto.setEstadoProyecto(dto.getEstadoProyecto());
        }
        if (dto.getPublicado() != null) {
            proyecto.setPublicado(dto.getPublicado());
        }
        if (dto.getActivo() != null) {
            proyecto.setActivo(dto.getActivo());
        }
        proyecto.setFechaLanzamiento(dto.getFechaLanzamiento());
        proyecto.setActualizadoPor(actualizadoPor);
    }
}
