package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.ZonaComunImagenRequestDTO;
import com.constructora_backend.dto.response.ZonaComunImagenResponseDTO;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.entity.ZonaComunImagen;
import org.springframework.stereotype.Component;

@Component
public class ZonaComunImagenMapper {

    public ZonaComunImagenResponseDTO toDTO(ZonaComunImagen imagen) {
        if (imagen == null) return null;

        ZonaComunImagenResponseDTO dto = new ZonaComunImagenResponseDTO();
        dto.setId(imagen.getId());
        dto.setImagenUrl(imagen.getImagenUrl());
        dto.setTitulo(imagen.getTitulo());
        dto.setOrden(imagen.getOrden());
        dto.setEsPrincipal(imagen.getEsPrincipal());
        dto.setCreadoEn(imagen.getCreadoEn());

        if (imagen.getZonaComun() != null) {
            dto.setZonaComunId(imagen.getZonaComun().getId());
            dto.setZonaComunNombre(imagen.getZonaComun().getNombre());
        }

        return dto;
    }

    public ZonaComunImagen toEntity(ZonaComunImagenRequestDTO dto, ZonaComun zonaComun) {
        if (dto == null) return null;

        ZonaComunImagen imagen = new ZonaComunImagen();
        imagen.setZonaComun(zonaComun);
        imagen.setImagenUrl(dto.getImagenUrl());
        imagen.setTitulo(dto.getTitulo());
        if (dto.getOrden() != null){
            imagen.setOrden(dto.getOrden());
        }
        if (dto.getEsPrincipal() != null){
            imagen.setEsPrincipal(dto.getEsPrincipal());
        }

        return imagen;
    }

    public void updateEntityFromDTO(ZonaComunImagenRequestDTO dto, ZonaComunImagen imagen, ZonaComun zonaComun) {
        if (dto == null || imagen == null) return;

        imagen.setZonaComun(zonaComun);
        imagen.setImagenUrl(dto.getImagenUrl());
        imagen.setTitulo(dto.getTitulo());
        if (dto.getOrden() != null) {
            imagen.setOrden(dto.getOrden());
        }
        if (dto.getEsPrincipal() != null) {
            imagen.setEsPrincipal(dto.getEsPrincipal());
        }
    }
}
