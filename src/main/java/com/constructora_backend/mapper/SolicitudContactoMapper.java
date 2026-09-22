package com.constructora_backend.mapper;

import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.entity.EstadoSolicitud;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.SolicitudContacto;
import org.springframework.stereotype.Component;

@Component
public class SolicitudContactoMapper {

    public SolicitudContactoResponseDTO toDTO(SolicitudContacto entidad) {
        if (entidad == null) return null;

        SolicitudContactoResponseDTO dto = new SolicitudContactoResponseDTO();
        dto.setId(entidad.getId());
        dto.setNombre(entidad.getNombre());
        dto.setCorreo(entidad.getCorreo());
        dto.setTelefono(entidad.getTelefono());
        dto.setMensaje(entidad.getMensaje());
        dto.setConsentimientoDatos(entidad.getCosentimientoDatos());
        dto.setIp(entidad.getIp());
        dto.setUserAgent(entidad.getUserAgent());
        dto.setAtentidaEn(entidad.getAtendidaEn());
        dto.setObservacionesInternas(entidad.getObservacionesInternas());
        dto.setCreadoEn(entidad.getCreadoEn());
        dto.setActualizadoEn(entidad.getActualizadoEn());

        if (entidad.getEstado() != null) {
            dto.setEstadoId(entidad.getEstado().getId());
            dto.setEstadoNombre(entidad.getEstado().getNombre());
        }

        if (entidad.getProyecto() != null) {
            dto.setProyectoId(entidad.getProyecto().getId());
            dto.setProyectoNombre(entidad.getProyecto().getNombre());
        }

        if (entidad.getLote() != null) {
            dto.setLoteId(entidad.getLote().getId());
            dto.setLoteCodigo(entidad.getLote().getCodigo());
        }

        if (entidad.getAtendidaPor() != null) {
            dto.setAtendidaPorId(entidad.getAtendidaPor().getId());
            dto.setAtentidaPorNombre(entidad.getAtendidaPor().getNombreCompleto());
        }

        return dto;
    }

    public SolicitudContacto toEntity(SolicitudContactoPublicDTO dto, EstadoSolicitud estadoInicial, Proyecto proyecto, Lote lote) {
        if (dto == null) return null;

        SolicitudContacto entidad = new SolicitudContacto();
        entidad.setEstado(estadoInicial);
        entidad.setProyecto(proyecto);
        entidad.setLote(lote);
        entidad.setNombre(dto.getNombre().trim());
        entidad.setCorreo(dto.getCorreo().trim().toLowerCase());
        entidad.setTelefono(dto.getTelefono().trim());
        entidad.setMensaje(dto.getMensaje());
        entidad.setCosentimientoDatos(dto.getConsentimientoDatos());
        entidad.setIp(dto.getIp());
        entidad.setUserAgent(dto.getUserAgent());

        return entidad;
    }
}
