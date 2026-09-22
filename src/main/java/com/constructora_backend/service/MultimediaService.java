package com.constructora_backend.service;

import com.constructora_backend.dto.request.MultimediaRequestDTO;
import com.constructora_backend.dto.response.MultimediaResponseDTO;
import com.constructora_backend.entity.*;
import com.constructora_backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MultimediaService {

    private final MultimediaRepository multimediaRepository;
    private final ProyectoRepository proyectoRepository;
    private final LoteRepository loteRepository;
    private final ZonaComunRepository zonaComunRepository;
    private final CasaModeloRepository casaModeloRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public MultimediaResponseDTO guardar(MultimediaRequestDTO dto, Long usuarioId) {
        validarUnSoloPadre(dto);

        Proyecto proyecto = dto.getProyectoId() != null
                ? proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado con ID: " + dto.getProyectoId()))
                : null;

        Lote lote = dto.getLoteId() != null
                ? loteRepository.findById(dto.getLoteId())
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado con ID: " + dto.getLoteId()))
                : null;

        ZonaComun zonaComun = dto.getZonaComunId() != null
                ? zonaComunRepository.findById(dto.getZonaComunId())
                .orElseThrow(() -> new EntityNotFoundException("Zona común no encontrada con ID: " + dto.getZonaComunId()))
                : null;

        CasaModelo casaModelo = dto.getCasaModeloId() != null
                ?casaModeloRepository.findById(dto.getCasaModeloId())
                .orElseThrow(() -> new EntityNotFoundException("Casa modelo no encontrada con ID: " + dto.getCasaModeloId()))
                : null;

        Usuario usuario = usuarioId != null
                ? usuarioRepository.findById(usuarioId).orElse(null)
                : null;

        Multimedia multimedia = Multimedia.builder()
                .proyecto(proyecto)
                .lote(lote)
                .zonaComun(zonaComun)
                .casaModelo(casaModelo)
                .tipo(dto.getTipo())
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .url(dto.getUrl())
                .nombreArchivo(dto.getNombreArchivo())
                .mimeType(dto.getMimeType())
                .tamanoBytes(dto.getTamanoBytes())
                .orden(dto.getOrden() != null ? dto.getOrden() : 1)
                .portada(Boolean.TRUE.equals(dto.getPortada()))
                .publicado(dto.getPublicado() == null || dto.getPublicado())
                .activo(dto.getActivo() == null || dto.getActivo())
                .creadoPor(usuario)
                .build();

        Multimedia guardada = multimediaRepository.save(multimedia);
        return mapToDTO(guardada);
    }

    @Transactional(readOnly = true)
    public MultimediaResponseDTO obtenerPorId(Long id) {
        Multimedia multimedia = multimediaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso multimedia no encontrado con ID: " + id));
        return mapToDTO(multimedia);
    }

    @Transactional(readOnly = true)
    public List<MultimediaResponseDTO> listarPorEntidad(String tipoEntidad, Long entidadId, boolean soloPublicados) {
        List<Multimedia> lista = switch (tipoEntidad.toLowerCase()) {
            case "proyecto" -> soloPublicados
                    ? multimediaRepository.findByProyectoIdAndPublicadoTrueAndActivoTrueOrderByOrdenAsc(entidadId)
                    : multimediaRepository.findByProyectoIdAndActivoTrueOrderByOrdenAsc(entidadId);
            case "lote" -> soloPublicados
                    ? multimediaRepository.findByLoteIdAndPublicadoTrueAndActivoTrueOrderByOrdenAsc(entidadId)
                    : multimediaRepository.findByLoteIdAndActivoTrueOrderByOrdenAsc(entidadId);
            case "zonacomun", "zona_comun" -> multimediaRepository.findByZonaComunIdAndActivoTrueOrderByOrdenAsc(entidadId);
            case "casamodelo", "casa_modelo" -> multimediaRepository.findByCasaModelo_IdAndActivoTrueOrderByOrdenAsc(entidadId);
            default -> throw new IllegalArgumentException("Tipo de entidad no soportado: " + tipoEntidad);
        };

        return lista.stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public MultimediaResponseDTO actualizar(Long id, MultimediaRequestDTO dto) {
        Multimedia multimedia = multimediaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso multimedia no encontrado con ID: " + id));

        multimedia.setTitulo(dto.getTitulo());
        multimedia.setDescripcion(dto.getDescripcion());
        multimedia.setUrl(dto.getUrl());
        if (dto.getOrden() != null) multimedia.setOrden(dto.getOrden());
        if (dto.getPortada() != null) multimedia.setPortada(dto.getPortada());
        if (dto.getPublicado() != null) multimedia.setPublicado(dto.getPublicado());
        if (dto.getActivo() != null) multimedia.setActivo(dto.getActivo());

        Multimedia actualizada = multimediaRepository.save(multimedia);
        return mapToDTO(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        Multimedia multimedia = multimediaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso multimedia no encontrado con ID: " + id));
        multimedia.setActivo(false);
        multimediaRepository.save(multimedia);
    }

    private void validarUnSoloPadre(MultimediaRequestDTO dto) {
        int count = 0;
        if (dto.getProyectoId() != null) count++;
        if (dto.getLoteId() != null) count++;
        if (dto.getZonaComunId() != null) count++;
        if (dto.getCasaModeloId() != null) count++;

        if (count != 1) {
            throw new IllegalArgumentException("Debe asociar el archivo multimedia exactamente a una entidad padre (Proyecto, Lote, Zona Común o Casa Modelo).");
        }
    }

    private MultimediaResponseDTO mapToDTO(Multimedia entity) {
        return MultimediaResponseDTO.builder()
                .id(entity.getId())
                .proyectoId(entity.getProyecto() != null ? entity.getProyecto().getId() : null)
                .loteId(entity.getLote() != null ? entity.getLote().getId() : null)
                .zonaComunId(entity.getZonaComun() != null ? entity.getZonaComun().getId() : null)
                .casaModeloId(entity.getCasaModelo() != null ? entity.getCasaModelo().getId() : null)
                .tipo(entity.getTipo())
                .titulo(entity.getTitulo())
                .descripcion(entity.getDescripcion())
                .url(entity.getUrl())
                .nombreArchivo(entity.getNombreArchivo())
                .mimeType(entity.getMimeType())
                .tamanoBytes(entity.getTamanoBytes())
                .orden(entity.getOrden())
                .portada(entity.getPortada())
                .publicado(entity.getPublicado())
                .activo(entity.getActivo())
                .creadoPorNombre(entity.getCreadoPor() != null ? entity.getCreadoPor().getNombreCompleto() : null)
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }
}
