package com.constructora_backend.mapper;

import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.entity.CasaModelo;
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
        dto.setServicioInteres(entidad.getServicioInteres());
        dto.setIdioma(entidad.getIdioma());
        dto.setMensaje(entidad.getMensaje());
        dto.setConsentimientoDatos(entidad.getCosentimientoDatos());
        dto.setVersionPolitica(entidad.getVersionPolitica());
        dto.setFechaConsentimiento(entidad.getFechaConsentimiento());
        dto.setIp(entidad.getIp());
        dto.setUserAgent(entidad.getUserAgent());
        dto.setAtendidaEn(entidad.getAtendidaEn());
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

        if (entidad.getCasaModelo() != null) {
            dto.setCasaModeloId(entidad.getCasaModelo().getId());
            dto.setCasaModeloNombre(entidad.getCasaModelo().getNombre());
        }

        if (entidad.getAtendidaPor() != null) {
            dto.setAtendidaPorId(entidad.getAtendidaPor().getId());
            dto.setAtendidaPorNombre(entidad.getAtendidaPor().getNombreCompleto());
        }

        return dto;
    }

    public SolicitudContacto toEntity(SolicitudContactoPublicDTO dto, EstadoSolicitud estadoInicial, Proyecto proyecto, Lote lote) {
        return toEntity(dto, estadoInicial, proyecto, lote, null);
    }

    public SolicitudContacto toEntity(SolicitudContactoPublicDTO dto, EstadoSolicitud estadoInicial, Proyecto proyecto, Lote lote, CasaModelo casaModelo) {
        if (dto == null) return null;

        SolicitudContacto entidad = new SolicitudContacto();
        entidad.setEstado(estadoInicial);
        entidad.setProyecto(proyecto);
        entidad.setLote(lote);
        entidad.setCasaModelo(casaModelo);
        entidad.setNombre(dto.getNombre().trim());
        entidad.setCorreo(limpiar(dto.getCorreo()) == null ? null : limpiar(dto.getCorreo()).toLowerCase());
        entidad.setTelefono(limpiar(dto.getTelefono()));
        entidad.setServicioInteres(limpiar(dto.getServicioInteres()));
        entidad.setIdioma(dto.getIdioma() != null ? dto.getIdioma() : "es");
        entidad.setMensaje(dto.getMensaje());
        entidad.setCosentimientoDatos(dto.getConsentimientoDatos());
        // La IP y el user-agent los fija el servicio a partir de la petición, nunca del cuerpo

        return entidad;
    }

    /**
     * Lo que se le devuelve a quien envía el formulario público: su propio envío sin la IP, el user-agent ni nada de la
     * gestión interna (observaciones, quién la atendió, la política aceptada).
     */
    public static SolicitudContactoResponseDTO sinDatosInternos(SolicitudContactoResponseDTO dto) {
        if (dto == null) return null;
        dto.setIp(null);
        dto.setUserAgent(null);
        dto.setObservacionesInternas(null);
        dto.setAtendidaPorId(null);
        dto.setAtendidaPorNombre(null);
        dto.setAtendidaEn(null);
        dto.setActualizadoEn(null);
        dto.setVersionPolitica(null);
        dto.setFechaConsentimiento(null);
        return dto;
    }

    /** Respuesta para un envío descartado por el campo trampa: parece la de un envío normal, pero sin identificador. */
    public static SolicitudContactoResponseDTO respuestaDescartada(SolicitudContactoPublicDTO dto) {
        SolicitudContactoResponseDTO respuesta = new SolicitudContactoResponseDTO();
        respuesta.setNombre(dto.getNombre() == null ? null : dto.getNombre().trim());
        respuesta.setCorreo(limpiar(dto.getCorreo()));
        respuesta.setTelefono(limpiar(dto.getTelefono()));
        respuesta.setIdioma(dto.getIdioma() != null ? dto.getIdioma() : "es");
        respuesta.setProyectoId(dto.getProyectoId());
        respuesta.setConsentimientoDatos(dto.getConsentimientoDatos());
        return respuesta;
    }

    /** Texto sin espacios sobrantes; un texto vacío se guarda como null. */
    private static String limpiar(String texto) {
        if (texto == null) return null;
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}