package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.UbicacionRequestDTO;
import com.constructora_backend.dto.response.UbicacionResponseDTO;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import org.springframework.stereotype.Component;

@Component
public class UbicacionMapper {

    public UbicacionResponseDTO toDto(Ubicacion ubicacion) {
        if (ubicacion == null) return null;

        UbicacionResponseDTO dto = new UbicacionResponseDTO();
        dto.setId(ubicacion.getId());
        dto.setDireccion(ubicacion.getDireccion());
        dto.setCiudad(ubicacion.getCiudad());
        dto.setDepartamento(ubicacion.getDepartamento());
        dto.setReferencias(ubicacion.getReferencias());
        dto.setLatitud(ubicacion.getLatitud());
        dto.setLongitud(ubicacion.getLongitud());
        dto.setGoogleMapsUrl(ubicacion.getGoogleMapsUrl());
        dto.setUrbanismoUrl(ubicacion.getUrbanismoUrl());
        dto.setVistaAereaUrl(ubicacion.getVistaAereaUrl());
        dto.setRecorrido360Url(ubicacion.getRecorrido360Url());
        dto.setVideoComoLlegarUrl(ubicacion.getVideoComoLlegarUrl());
        dto.setCreadoEn(ubicacion.getCreadoEn());
        dto.setActualizadoEn(ubicacion.getActualizadoEn());

        if (ubicacion.getProyecto() != null) {
            dto.setProyectoId(ubicacion.getProyecto().getId());
            dto.setProyectoNombre(ubicacion.getProyecto().getNombre());
        }

        return dto;
    }

    public Ubicacion toEntity(UbicacionRequestDTO dto, Proyecto proyecto) {
        if (dto == null) return null;

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setProyecto(proyecto);
        ubicacion.setDireccion(dto.getDireccion());
        ubicacion.setCiudad(dto.getCiudad());
        ubicacion.setDepartamento(dto.getDepartamento());
        ubicacion.setReferencias(dto.getReferencia());
        ubicacion.setLatitud(dto.getLatitud());
        ubicacion.setLongitud(dto.getLongitud());
        ubicacion.setGoogleMapsUrl(dto.getGoogleMapUrl());
        ubicacion.setUrbanismoUrl(dto.getUrbanismoUrl());
        ubicacion.setVistaAereaUrl(dto.getVistaAereaUrl());
        ubicacion.setRecorrido360Url(dto.getRecorrido360Url());
        ubicacion.setVideoComoLlegarUrl(dto.getVideoComoLlegarUrl());

        return ubicacion;
    }

    public void updateEntityFromDTO(UbicacionRequestDTO dto, Ubicacion ubicacion, Proyecto proyecto) {
        if (dto == null || ubicacion == null) return;

        ubicacion.setProyecto(proyecto);
        ubicacion.setDireccion(dto.getDireccion());
        ubicacion.setCiudad(dto.getCiudad());
        ubicacion.setDepartamento(dto.getDepartamento());
        ubicacion.setReferencias(dto.getReferencia());
        ubicacion.setLatitud(dto.getLatitud());
        ubicacion.setLongitud(dto.getLongitud());
        ubicacion.setGoogleMapsUrl(dto.getGoogleMapUrl());
        ubicacion.setUrbanismoUrl(dto.getUrbanismoUrl());
        ubicacion.setVistaAereaUrl(dto.getVistaAereaUrl());
        ubicacion.setRecorrido360Url(dto.getRecorrido360Url());
        ubicacion.setVideoComoLlegarUrl(dto.getVideoComoLlegarUrl());
    }
}
