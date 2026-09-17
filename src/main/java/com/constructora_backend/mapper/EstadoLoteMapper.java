package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.EstadoLoteRequestDTO;
import com.constructora_backend.dto.response.EstadoLoteResponseDTO;
import com.constructora_backend.entity.EstadoLote;
import org.springframework.stereotype.Component;

@Component
public class EstadoLoteMapper {

    public EstadoLoteResponseDTO toDTO(EstadoLote estado){
        if (estado == null) return null;

        EstadoLoteResponseDTO dto = new EstadoLoteResponseDTO();
        dto.setId(estado.getId());
        dto.setNombre(estado.getNombre());
        dto.setDescripcion(estado.getDescripcion());
        dto.setOrden(estado.getOrden());
        dto.setActivo(estado.getActivo());

        return dto;
    }

    public EstadoLote toEntity(EstadoLoteRequestDTO dto) {
        if (dto == null) return null;

        EstadoLote estado = new EstadoLote();
        estado.setNombre(dto.getNombre());
        estado.setDescripcion(dto.getDescripcion());

        if (dto.getOrden() != null) estado.setOrden(dto.getOrden());
        if (dto.getActivo() != null) estado.setActivo(dto.getActivo());

        return estado;
    }

    public void updateEntityFromDTO(EstadoLoteRequestDTO dto, EstadoLote estado) {
        if (dto == null || estado == null) return;

        estado.setNombre(dto.getNombre());
        estado.setDescripcion(dto.getDescripcion());

        if (dto.getOrden() != null) estado.setOrden(dto.getOrden());
        if (dto.getActivo() != null) estado.setActivo(dto.getActivo());
    }
}
