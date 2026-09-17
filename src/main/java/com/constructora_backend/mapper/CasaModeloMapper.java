package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.CasaModeloRequestDTO;
import com.constructora_backend.dto.response.CasaModeloResponseDTO;
import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.Proyecto;
import org.springframework.stereotype.Component;

@Component
public class CasaModeloMapper {

    public CasaModeloResponseDTO toDTO(CasaModelo casa) {
        if (casa == null) return null;

        CasaModeloResponseDTO dto = new CasaModeloResponseDTO();
        dto.setId(casa.getId());
        dto.setNombre(casa.getNombre());
        dto.setDescripcion(casa.getDescripcion());
        dto.setAreaConstruidaM2(casa.getAreaConstruidaM2());
        dto.setNumeroHabitaciones(casa.getNumeroHabitaciones());
        dto.setNumeroBanos(casa.getNumeroBanos());
        dto.setTourVirtualUrl(casa.getTourVirtualUrl());
        dto.setPlanoUrl(casa.getPlanoUrl());
        dto.setPublicado(casa.getPublicado());
        dto.setActivo(casa.getActivo());
        dto.setCreadoEn(casa.getCreadoEn());
        dto.setActualizadoEn(casa.getActualizadoEn());

        if (casa.getProyecto() != null) {
            dto.setProyectoId(casa.getProyecto().getId());
            dto.setProyectoNombre(casa.getProyecto().getNombre());
        }

        return dto;
    }

    public CasaModelo toEntity(CasaModeloRequestDTO dto, Proyecto proyecto) {
        if (dto == null) return null;

        CasaModelo casa = new CasaModelo();
        casa.setProyecto(proyecto);
        casa.setNombre(dto.getNombre());
        casa.setDescripcion(dto.getDescripcion());
        casa.setAreaConstruidaM2(dto.getAreaConstruidaM2());
        casa.setNumeroHabitaciones(dto.getNumeroHabitaciones());
        casa.setNumeroBanos(dto.getNumeroBanos());
        casa.setTourVirtualUrl(dto.getTourVirtualUrl());
        casa.setPlanoUrl(dto.getPlanoUrl());

        if (dto.getPublicado() != null) casa.setPublicado(dto.getPublicado());
        if (dto.getActivo() != null) casa.setActivo(dto.getActivo());

        return casa;
    }

    public void updateEntityFromDTO(CasaModeloRequestDTO dto, CasaModelo casa, Proyecto proyecto) {
        if (dto == null || casa == null) return;

        casa.setProyecto(proyecto);
        casa.setNombre(dto.getNombre());
        casa.setDescripcion(dto.getDescripcion());
        casa.setAreaConstruidaM2(dto.getAreaConstruidaM2());
        casa.setNumeroHabitaciones(dto.getNumeroHabitaciones());
        casa.setNumeroBanos(dto.getNumeroBanos());
        casa.setTourVirtualUrl(dto.getTourVirtualUrl());
        casa.setPlanoUrl(dto.getPlanoUrl());

        if (dto.getPublicado() != null) casa.setPublicado(dto.getPublicado());
        if (dto.getActivo() != null) casa.setActivo(dto.getActivo());
    }
}
