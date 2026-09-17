package com.constructora_backend.service;

import com.constructora_backend.dto.request.ZonaComunImagenRequestDTO;
import com.constructora_backend.dto.response.ZonaComunImagenResponseDTO;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.entity.ZonaComunImagen;
import com.constructora_backend.mapper.ZonaComunImagenMapper;
import com.constructora_backend.repository.ZonaComunImagenRepository;
import com.constructora_backend.repository.ZonaComunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ZonaComunImagenService {

    @Autowired
    private ZonaComunImagenRepository imagenRepository;

    @Autowired
    private ZonaComunRepository zonaComunRepository;

    @Autowired
    private ZonaComunImagenMapper imagenMapper;

    public List<ZonaComunImagenResponseDTO> listarPorZonaComun(Long zonaComunId) {
        return imagenRepository.findByZonaComunIdOrderByOrdenAsc(zonaComunId)
                .stream()
                .map(imagenMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ZonaComunImagenResponseDTO> obtenerPorId(Long id) {
        return imagenRepository.findById(id)
                .map(imagenMapper::toDTO);
    }

    @Transactional
    public ZonaComunImagenResponseDTO guardar(ZonaComunImagenRequestDTO dto) {
        ZonaComun zonaComun = zonaComunRepository.findById(dto.getZonaComunId())
                .orElseThrow(() -> new RuntimeException("Zona común no encontrada con ID: " + dto.getZonaComunId()));

        if (Boolean.TRUE.equals(dto.getEsPrincipal())) {
            imagenRepository.desmarcarPrincipalesDeZonaComun(dto.getZonaComunId());
        }

        ZonaComunImagen imagen = imagenMapper.toEntity(dto, zonaComun);
        ZonaComunImagen guardada = imagenRepository.save(imagen);
        return imagenMapper.toDTO(guardada);
    }

    @Transactional
    public ZonaComunImagenResponseDTO actualizar(Long id, ZonaComunImagenRequestDTO dto) {
        ZonaComunImagen existente = imagenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagen de zona común no encontrada con ID: " + id));

        ZonaComun zonaComun = zonaComunRepository.findById(dto.getZonaComunId())
                .orElseThrow(() -> new RuntimeException("Zona común no encontrada con ID: " + dto.getZonaComunId()));

        if (Boolean.TRUE.equals(dto.getEsPrincipal())) {
            imagenRepository.desmarcarPrincipalesDeZonaComun(dto.getZonaComunId());
        }

        imagenMapper.updateEntityFromDTO(dto, existente, zonaComun);
        ZonaComunImagen actualizada = imagenRepository.save(existente);
        return imagenMapper.toDTO(actualizada);
    }

    @Transactional
    public void marcarComoPrincipal(Long id) {
        ZonaComunImagen existente = imagenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagen de zona común no encontrada con ID: " + id));

        imagenRepository.desmarcarPrincipalesDeZonaComun(existente.getZonaComun().getId());
        existente.setEsPrincipal(true);
        imagenRepository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!imagenRepository.existsById(id)) {
            throw new RuntimeException("Imagen no encontrada con ID: " + id);
        }
        imagenRepository.deleteById(id);
    }
}
