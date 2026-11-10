package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.EstadoSolicitudRequestDTO;
import com.constructora_backend.dto.response.EstadoSolicitudResponseDTO;
import com.constructora_backend.entity.EstadoSolicitud;
import org.springframework.stereotype.Component;

@Component
public class EstadoSolicitudMapper {

    public EstadoSolicitudResponseDTO toDTO(EstadoSolicitud estado) {
        if (estado == null) return null;

        EstadoSolicitudResponseDTO dto = new EstadoSolicitudResponseDTO();
        dto.setId(estado.getId());
        dto.setNombre(estado.getNombre());
        dto.setDescripcion(estado.getDescripcion());
        dto.setOrden(estado.getOrden());
        dto.setActivo(estado.getActivo());

        return dto;
    }

    public EstadoSolicitud toEntity(EstadoSolicitudRequestDTO dto) {
        if (dto == null) return null;

        EstadoSolicitud estado = new EstadoSolicitud();
        estado.setNombre(dto.getNombre().trim());
        estado.setDescripcion(dto.getDescripcion());

        if (dto.getOrden() != null) estado.setOrden(dto.getOrden());
        if (dto.getActivo() != null) estado.setActivo(dto.getActivo());

        return estado;
    }

    public void updateEntityFromDTO(EstadoSolicitudRequestDTO dto, EstadoSolicitud estado) {
        if (dto == null || estado == null) return;

        estado.setNombre(dto.getNombre().trim());
        estado.setDescripcion(dto.getDescripcion());

        if (dto.getOrden() != null) estado.setOrden(dto.getOrden());
        if (dto.getActivo() != null) estado.setActivo(dto.getActivo());
    }
}
