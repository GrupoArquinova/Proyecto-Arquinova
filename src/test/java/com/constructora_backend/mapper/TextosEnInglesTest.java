package com.constructora_backend.mapper;

import com.constructora_backend.dto.request.CasaModeloRequestDTO;
import com.constructora_backend.dto.request.ContenidoInstitucionalRequestDTO;
import com.constructora_backend.dto.response.CasaModeloResponseDTO;
import com.constructora_backend.dto.response.ContenidoInstitucionalResponseDTO;
import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.ContenidoInstitucional;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Proyecto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextosEnInglesTest {

    private final CasaModeloMapper casaMapper = new CasaModeloMapper();
    private final ContenidoInstitucionalMapper contenidoMapper = new ContenidoInstitucionalMapper();

    private CasaModeloRequestDTO pedidoCasa(String descripcionEn) {
        CasaModeloRequestDTO dto = new CasaModeloRequestDTO();
        dto.setProyectoId(1L);
        dto.setNombre("Villa Samán");
        dto.setDescripcion("Villa de dos plantas");
        dto.setDescripcionEn(descripcionEn);
        return dto;
    }

    @Test
    void guardaElTextoEnInglesLimpiandoLosEspacios() {
        CasaModelo casa = casaMapper.toEntity(pedidoCasa("  Two-storey villa  "), new Proyecto());
        assertEquals("Two-storey villa", casa.getDescripcionEn());
        assertEquals("Villa de dos plantas", casa.getDescripcion());
    }

    @Test
    void unTextoEnInglesVacioSeGuardaComoSinTexto() {
        CasaModelo casa = casaMapper.toEntity(pedidoCasa("   "), new Proyecto());
        assertNull(casa.getDescripcionEn());
    }

    @Test
    void siElPedidoNoTraeElIngles_noBorraElQueYaHabia() {
        CasaModelo existente = new CasaModelo();
        existente.setDescripcionEn("Two-storey villa");
        casaMapper.updateEntityFromDTO(pedidoCasa(null), existente, new Proyecto());
        assertEquals("Two-storey villa", existente.getDescripcionEn());
    }

    @Test
    void siElPedidoTraeElInglesVacio_loBorra() {
        CasaModelo existente = new CasaModelo();
        existente.setDescripcionEn("Two-storey villa");
        casaMapper.updateEntityFromDTO(pedidoCasa(""), existente, new Proyecto());
        assertNull(existente.getDescripcionEn());
    }

    @Test
    void laRespuestaIncluyeElIngles() {
        CasaModelo casa = new CasaModelo();
        casa.setProyecto(new Proyecto());
        casa.setDescripcionEn("Two-storey villa");
        CasaModeloResponseDTO dto = casaMapper.toDTO(casa);
        assertEquals("Two-storey villa", dto.getDescripcionEn());
    }

    @Test
    void elContenidoInstitucionalGuardaTituloYContenidoEnIngles() {
        ContenidoInstitucionalRequestDTO pedido = new ContenidoInstitucionalRequestDTO();
        pedido.setSeccion("MISION");
        pedido.setTitulo("Misión");
        pedido.setContenido("Acompañar proyectos");
        pedido.setTituloEn("Mission");
        pedido.setContenidoEn("  To support projects ");

        ContenidoInstitucional entidad = contenidoMapper.toEntity(pedido, new Empresa(), null);
        assertEquals("Mission", entidad.getTituloEn());
        assertEquals("To support projects", entidad.getContenidoEn());

        ContenidoInstitucionalResponseDTO respuesta = contenidoMapper.toDTO(entidad);
        assertEquals("To support projects", respuesta.getContenidoEn());
    }
}
