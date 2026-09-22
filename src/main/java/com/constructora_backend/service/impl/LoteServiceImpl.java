package com.constructora_backend.service.impl;

import com.constructora_backend.dto.request.CambiarEstadoLoteDTO;
import com.constructora_backend.dto.response.HistorialEstadoLoteDTO;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.HistorialEstadoLote;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.repository.EstadoLoteRepository;
import com.constructora_backend.repository.HistorialEstadoLoteRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoteServiceImpl {

    private final LoteRepository loteRepository;
    private final EstadoLoteRepository estadoLoteRepository;
    private final HistorialEstadoLoteRepository historialEstadoLoteRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void cambiarEstadoLote(Long loteId, CambiarEstadoLoteDTO dto, Long usuarioId) {
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado con ID: " + loteId));

        EstadoLote nuevoEstado = estadoLoteRepository.findById(Integer.valueOf(dto.getNuevoEstadoId()))
                .orElseThrow(() -> new EntityNotFoundException("Estado no encontrado con Id: " + dto.getNuevoEstadoId()));

        EstadoLote estadoAnterior = lote.getEstado();

        if (estadoAnterior != null && estadoAnterior.getId().equals(nuevoEstado.getId())) {
            throw new IllegalArgumentException("El lote ya se encuentra en el estado: " + nuevoEstado.getNombre());
        }

        Usuario usuario = null;
        if (usuarioId != null) {
            usuario = usuarioRepository.findById(usuarioId).orElse(null);
        }
        if (usuario == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                usuario = usuarioRepository.findByCorreo(auth.getName()).orElse(null);
            }
        }

        lote.setEstado(nuevoEstado);
        loteRepository.save(lote);

        HistorialEstadoLote historial = HistorialEstadoLote.builder()
                .lote(lote)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(nuevoEstado)
                .usuario(usuario)
                .observaciones(dto.getObservaciones())
                .build();

        historialEstadoLoteRepository.save(historial);
    }

    @Transactional(readOnly = true)
    public List<HistorialEstadoLoteDTO> consultarHistorial(Long loteId) {
        return historialEstadoLoteRepository.findByLote_IdOrderByCambiadoEnDesc(loteId)
                .stream()
                .map(h -> HistorialEstadoLoteDTO.builder()
                        .id(h.getId())
                        .loteId(h.getLote().getId())
                        .estadoAnterior(h.getEstadoAnterior() != null ? h.getEstadoAnterior().getNombre() : "SIN ESTADO PREVIO")
                        .estadoNuevo(h.getEstadoNuevo().getNombre())
                        .usuarioNombre(h.getUsuario() != null ? h.getUsuario().getNombreCompleto(): "SISTEMA")
                        .observaciones(h.getObservaciones())
                        .cambiadoEn(h.getCambiadoEn())
                        .build())
                .toList();
    }
}
