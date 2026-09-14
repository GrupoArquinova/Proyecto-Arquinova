package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.ZonaComunRequestDTO;
import com.constructora_backend.dto.response.ZonaComunResponseDTO;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.ZonaComun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class ZonaComunMapper {

    @Autowired
    private ZonaComunImagenMapper imagenMapper;

    public ZonaComunResponseDTO toDTO(ZonaComun zona) {
        if (zona == null) return null;

        ZonaComunResponseDTO dto = new ZonaComunResponseDTO();
        dto.setId(zona.getId());
        dto.setNombre(zona.getNombre());
        dto.setDescripcion(zona.getDescripcion());
        dto.setPublicado(zona.getPublicado());
        dto.setActivo(zona.getActivo());
        dto.setCreadoEn(zona.getCreadoEn());
        dto.setActualizadoEn(zona.getActualizadoEn());

        if (zona.getProyecto() != null) {
            dto.setProyectoId(zona.getProyecto().getId());
            dto.setProyectoNombre(zona.getProyecto().getNombre());
        }

        if (zona.getImagenes() != null && !zona.getImagenes().isEmpty()) {
            dto.setImagenes(
                    zona.getImagenes().stream()
                            .map(imagenMapper::toDTO)
                            .collect(Collectors.toList())
            );

            dto.getImagenes().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getEsPrincipal()))
                    .findFirst()
                    .ifPresentOrElse(
                            principal -> dto.setImagenPrincipalUrl(principal.getImagenUrl()),
                            () -> dto.setImagenPrincipalUrl(dto.getImagenes().get(0).getImagenUrl())
                    );
        } else {
            dto.setImagenes(Collections.emptyList());
        }

        return dto;
    }

    public ZonaComun toEntity(ZonaComunRequestDTO dto, Proyecto proyecto) {
        if (dto == null) return null;

        ZonaComun zona = new ZonaComun();
        zona.setProyecto(proyecto);
        zona.setNombre(dto.getNombre());
        zona.setDescripcion(dto.getDescripcion());
        if (dto.getPublicado() != null) zona.setPublicado(dto.getPublicado());
        if (dto.getActivo() != null) zona.setActivo(dto.getActivo());

        return zona;
    }

    public void updateEntityFromDTO(ZonaComunRequestDTO dto, ZonaComun zona, Proyecto proyecto) {
        if (dto == null || zona == null) return;

        zona.setProyecto(proyecto);
        zona.setNombre(dto.getNombre());
        zona.setDescripcion(dto.getDescripcion());
        if (dto.getPublicado() != null) zona.setPublicado(dto.getPublicado());
        if (dto.getActivo() != null) zona.setActivo(dto.getActivo());
    }
}
