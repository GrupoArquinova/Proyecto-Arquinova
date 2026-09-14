package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.EtapaRequestDTO;
import com.constructora_backend.dto.response.EtapaResponseDTO;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Proyecto;
import org.springframework.stereotype.Component;

@Component
public class EtapaMapper {

    public EtapaResponseDTO toDTO(Etapa etapa) {
        if (etapa == null) return null;

        EtapaResponseDTO dto = new EtapaResponseDTO();
        dto.setId(etapa.getId());
        dto.setNombre(etapa.getNombre());
        dto.setDescripcion(etapa.getDescripcion());
        dto.setOrden(etapa.getOrden());
        dto.setActivo(etapa.getActivo());
        dto.setCreadoEn(etapa.getCreadoEn());

        if (etapa.getProyecto() != null) {
            dto.setProyectoId(etapa.getProyecto().getId());
            dto.setProyectoNombre(etapa.getProyecto().getNombre());
        }

        return dto;
    }


    public Etapa toEntity(EtapaRequestDTO dto, Proyecto proyecto) {
        if (dto == null) return null;

        Etapa etapa = new Etapa();
        etapa.setProyecto(proyecto);
        etapa.setNombre(dto.getNombre().trim());
        etapa.setDescripcion(dto.getDescripcion());
        if (dto.getOrden() != null) {
            etapa.setOrden(dto.getOrden());
        }
        if (dto.getActivo() != null) {
            etapa.setActivo(dto.getActivo());
        }

        return etapa;
    }

    public void updateEntityFromDTO(EtapaRequestDTO dto, Etapa etapa, Proyecto proyecto) {
        if (dto == null || etapa == null) return;

        etapa.setProyecto(proyecto);
        etapa.setNombre(dto.getNombre().trim());
        etapa.setDescripcion(dto.getDescripcion());
        if (dto.getOrden() != null) {
            etapa.setOrden(dto.getOrden());
        }
        if (dto.getActivo() != null) {
            etapa.setActivo(dto.getActivo());
        }
    }
}
