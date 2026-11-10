package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.EmpresaRequestDTO;
import com.constructora_backend.dto.response.EmpresaResponseDTO;
import com.constructora_backend.entity.Empresa;
import org.springframework.stereotype.Component;

@Component
public class EmpresaMapper {

    public EmpresaResponseDTO toDTO(Empresa empresa) {
        if (empresa == null) return null;

        EmpresaResponseDTO dto = new EmpresaResponseDTO();
        dto.setId(empresa.getId());
        dto.setNombre(empresa.getNombre());
        dto.setNit(empresa.getNit());
        dto.setDescripcion(empresa.getDescripcion());
        dto.setTrayectoria(empresa.getTrayectoria());
        dto.setServicios(empresa.getServicios());
        dto.setCorreoComercial(empresa.getCorreoComercial());
        dto.setTelefono(empresa.getTelefono());
        dto.setWhatsapp(empresa.getWhatsapp());
        dto.setSitioWeb(empresa.getSitioWeb());
        dto.setDireccion(empresa.getDireccion());
        dto.setLogoUrl(empresa.getLogoUrl());
        dto.setActivo(empresa.getActivo());
        dto.setCreadoEn(empresa.getCreadoEn());
        dto.setActualizadoEn(empresa.getActualizadoEn());
        return dto;
    }

    public Empresa toEntity(EmpresaRequestDTO dto) {
        if (dto == null) return null;

        Empresa empresa = new Empresa();
        empresa.setNombre(dto.getNombre());
        empresa.setNit(dto.getNit());
        empresa.setDescripcion(dto.getDescripcion());
        empresa.setTrayectoria(dto.getTrayectoria());
        empresa.setServicios(dto.getServicios());
        empresa.setCorreoComercial(dto.getCorreoComercial());
        empresa.setTelefono(dto.getTelefono());
        empresa.setWhatsapp(dto.getWhatsapp());
        empresa.setSitioWeb(dto.getSitioWeb());
        empresa.setDireccion(dto.getDireccion());
        empresa.setLogoUrl(dto.getLogoUrl());
        if (dto.getActivo() != null) {
            empresa.setActivo(dto.getActivo());
        }
        return empresa;
    }

    public void updateEntityFromDTO(EmpresaRequestDTO dto, Empresa empresa) {
        if (dto == null || empresa == null) return;

        empresa.setNombre(dto.getNombre());
        empresa.setNit(dto.getNit());
        empresa.setDescripcion(dto.getDescripcion());
        empresa.setTrayectoria(dto.getTrayectoria());
        empresa.setServicios(dto.getServicios());
        empresa.setCorreoComercial(dto.getCorreoComercial());
        empresa.setTelefono(dto.getTelefono());
        empresa.setWhatsapp(dto.getWhatsapp());
        empresa.setSitioWeb(dto.getSitioWeb());
        empresa.setDireccion(dto.getDireccion());
        empresa.setLogoUrl(dto.getLogoUrl());
        if (dto.getActivo() != null) {
            empresa.setActivo(dto.getActivo());
        }
    }
}
