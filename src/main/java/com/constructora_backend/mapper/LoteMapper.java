package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.LoteRequestDTO;
import com.constructora_backend.dto.response.LoteResponseDTO;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class LoteMapper {

    public LoteResponseDTO toDTO(Lote lote) {
        if (lote == null) return null;

        LoteResponseDTO dto = new LoteResponseDTO();
        dto.setId(lote.getId());
        dto.setCodigo(lote.getCodigo());
        dto.setNombre(lote.getNombre());
        dto.setAreaM2(lote.getAreaM2());
        dto.setDescripcion(lote.getDescripcion());
        dto.setCaracteristicas(lote.getCaracteristicas());
        dto.setPosicionX(lote.getPosicionX());
        dto.setPosicionY(lote.getPosicionY());
        dto.setPublicado(lote.getPublicado());
        dto.setActivo(lote.getActivo());
        dto.setCreadoEn(lote.getCreadoEn());
        dto.setActualizadoEn(lote.getActualizadoEn());

        if (lote.getEtapa() != null) {
            dto.setEtapaId(lote.getEtapa().getId());
            dto.setEtapaNombre(lote.getEtapa().getNombre());
        }

        if (lote.getEstado() != null) {
            dto.setEstadoId(lote.getEstado().getId());
            dto.setEstadoNombre(lote.getEstado().getNombre());
        }

        if (lote.getCreadoPor() != null) {
            dto.setCreadoPorId(lote.getCreadoPor().getId());
            dto.setCreadoPorNombre(lote.getCreadoPor().getNombreCompleto());
        }

        if (lote.getActualizadoPor() != null) {
            dto.setActualizadoPorId(lote.getActualizadoPor().getId());
            dto.setActualizadoPorNombre(lote.getActualizadoPor().getNombreCompleto());
        }

        return dto;
    }

    public Lote toEntity(LoteRequestDTO dto, Etapa etapa, EstadoLote estado, Usuario creadoPor) {
        if (dto == null) return null;

        Lote lote = new Lote();
        lote.setEtapa(etapa);
        lote.setEstado(estado);
        lote.setCodigo(dto.getCodigo());
        lote.setNombre(dto.getNombre());
        lote.setAreaM2(dto.getAreaM2());
        lote.setDescripcion(dto.getDescripcion());
        lote.setCaracteristicas(dto.getCaracteristicas());
        lote.setPosicionX(dto.getPosicionX());
        lote.setPosicionY(dto.getPosicionY());
        lote.setCreadoPor(creadoPor);

        if (dto.getPublicado() != null) lote.setPublicado(dto.getPublicado());
        if (dto.getActivo() != null) lote.setActivo(dto.getActivo());

        return lote;
    }

    public void updateEntityFromDTO(LoteRequestDTO dto, Lote lote, Etapa etapa, EstadoLote estado, Usuario actualizadoPor) {
        if (dto == null || lote == null) return;

        lote.setEtapa(etapa);
        lote.setEstado(estado);
        lote.setCodigo(dto.getCodigo());
        lote.setNombre(dto.getNombre());
        lote.setAreaM2(dto.getAreaM2());
        lote.setDescripcion(dto.getDescripcion());
        lote.setCaracteristicas(dto.getCaracteristicas());
        lote.setPosicionX(dto.getPosicionX());
        lote.setPosicionY(dto.getPosicionY());
        lote.setActualizadoPor(actualizadoPor);

        if (dto.getPublicado() != null) lote.setPublicado(dto.getPublicado());
        if (dto.getActivo() != null) lote.setActivo(dto.getActivo());
    }
}
