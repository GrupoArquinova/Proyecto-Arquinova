package com.constructora_backend.service;

import com.constructora_backend.dto.request.Punto360RequestDTO;
import com.constructora_backend.dto.response.Punto360ResponseDTO;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Punto360;
import com.constructora_backend.enums.EscenaPunto360;
import com.constructora_backend.repository.EtapaRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.Punto360Repository;
import com.constructora_backend.repository.ProyectoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class Punto360Service {

    @Autowired
    private Punto360Repository punto360Repository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Transactional(readOnly = true)
    public List<Punto360ResponseDTO> listarPorProyecto(Long proyectoId, EscenaPunto360 escena) {
        List<Punto360> puntos = escena == null
                ? punto360Repository.findByProyectoIdAndActivoTrueOrderByIdAsc(proyectoId)
                : punto360Repository.findByProyectoIdAndEscenaAndActivoTrueOrderByIdAsc(proyectoId, escena);
        return puntos.stream().map(this::aDTOSiExiste).filter(Objects::nonNull).toList();
    }

    @Transactional
    public Punto360ResponseDTO guardar(Punto360RequestDTO dto) {
        Punto360 punto = new Punto360();
        aplicar(punto, dto);
        return aDTO(punto360Repository.save(punto));
    }

    @Transactional
    public Punto360ResponseDTO actualizar(Long id, Punto360RequestDTO dto) {
        Punto360 punto = punto360Repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Punto no encontrado con ID: " + id));
        aplicar(punto, dto);
        return aDTO(punto360Repository.save(punto));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!punto360Repository.existsById(id)) {
            throw new EntityNotFoundException("Punto no encontrado con ID: " + id);
        }
        punto360Repository.deleteById(id);
    }

    /** Valida que el punto traiga la posición que su escena necesita y que lote/etapa sean del mismo proyecto. */
    private void aplicar(Punto360 punto, Punto360RequestDTO dto) {
        boolean esPlano = dto.getEscena() == EscenaPunto360.URBANISMO;

        if (esPlano && (dto.getPosX() == null || dto.getPosY() == null)) {
            throw new IllegalArgumentException("Un punto del plano de urbanismo necesita posX y posY.");
        }
        if (!esPlano && (dto.getYaw() == null || dto.getPitch() == null)) {
            throw new IllegalArgumentException("Un punto de una imagen 360° necesita yaw y pitch.");
        }

        Proyecto proyecto = proyectoRepository.findById(dto.getProyectoId())
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado con ID: " + dto.getProyectoId()));

        Lote lote = null;
        if (dto.getLoteId() != null) {
            lote = loteRepository.findById(dto.getLoteId())
                    .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado con ID: " + dto.getLoteId()));
            if (!proyecto.getId().equals(lote.getEtapa().getProyecto().getId())) {
                throw new IllegalArgumentException("El lote no pertenece a este proyecto.");
            }
        }

        Etapa etapa = null;
        if (dto.getEtapaId() != null) {
            etapa = etapaRepository.findById(dto.getEtapaId())
                    .orElseThrow(() -> new EntityNotFoundException("Etapa no encontrada con ID: " + dto.getEtapaId()));
            if (!proyecto.getId().equals(etapa.getProyecto().getId())) {
                throw new IllegalArgumentException("La etapa no pertenece a este proyecto.");
            }
        }

        punto.setProyecto(proyecto);
        punto.setEscena(dto.getEscena());
        punto.setLote(lote);
        punto.setEtapa(etapa);
        punto.setEtiqueta(dto.getEtiqueta().trim());
        punto.setYaw(esPlano ? null : dto.getYaw());
        punto.setPitch(esPlano ? null : dto.getPitch());
        punto.setPosX(esPlano ? dto.getPosX() : null);
        punto.setPosY(esPlano ? dto.getPosY() : null);
    }

    /**
     * Sin clave foránea en la base, un lote o una etapa borrados dejan su punto huérfano: ese punto se omite
     * en lugar de romper la lectura pública de todo el proyecto.
     */
    private Punto360ResponseDTO aDTOSiExiste(Punto360 p) {
        try {
            return aDTO(p);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    private Punto360ResponseDTO aDTO(Punto360 p) {
        Lote lote = p.getLote();
        Etapa etapa = p.getEtapa();
        return Punto360ResponseDTO.builder()
                .id(p.getId())
                .proyectoId(p.getProyecto().getId())
                .escena(p.getEscena())
                .etiqueta(p.getEtiqueta())
                .loteId(lote != null ? lote.getId() : null)
                .loteCodigo(lote != null ? lote.getCodigo() : null)
                .loteAreaM2(lote != null ? lote.getAreaM2() : null)
                .loteEstado(lote != null && lote.getEstado() != null ? lote.getEstado().getNombre() : null)
                .etapaId(etapa != null ? etapa.getId() : null)
                .etapaNombre(etapa != null ? etapa.getNombre() : null)
                .yaw(p.getYaw())
                .pitch(p.getPitch())
                .posX(p.getPosX())
                .posY(p.getPosY())
                .build();
    }
}
