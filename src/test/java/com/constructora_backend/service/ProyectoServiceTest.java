package com.constructora_backend.service;

import com.constructora_backend.dto.request.ProyectoRequestDTO;
import com.constructora_backend.dto.response.ProyectoResponseDTO;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.enums.TipoRegistroProyecto;
import com.constructora_backend.mapper.ProyectoMapper;
import com.constructora_backend.repository.EmpresaRepository;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProyectoMapper proyectoMapper;

    @InjectMocks
    private ProyectoService proyectoService;

    private ProyectoRequestDTO requestDTO;
    private Empresa empresa;
    private Proyecto proyecto;
    private ProyectoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNombre("Constructora Central");

        requestDTO = new ProyectoRequestDTO();
        requestDTO.setEmpresaId(1L);
        requestDTO.setNombre("Residencial El Bosque");
        requestDTO.setSlug("residencial-el-bosque");

        proyecto = new Proyecto();
        proyecto.setId(10L);
        proyecto.setEmpresa(empresa);
        proyecto.setNombre("Residencial El Bosque");
        proyecto.setSlug("residencial-el-bosque");

        responseDTO = new ProyectoResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setEmpresaId(1L);
        responseDTO.setNombre("Residencial El Bosque");
        responseDTO.setSlug("residencial-el-bosque");
    }

    @Test
    void guardarProyectoExitosamente() {
        when(proyectoRepository.existsByEmpresaIdAndSlug(1L, "residencial-el-bosque")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(proyectoMapper.toEntity(requestDTO, empresa, null, null)).thenReturn(proyecto);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(proyectoMapper.toDTO(proyecto)).thenReturn(responseDTO);

        ProyectoResponseDTO resultado = proyectoService.guardar(requestDTO);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("residencial-el-bosque", resultado.getSlug());
        verify(proyectoRepository, times(1)).save(any(Proyecto.class));
    }

    @Test
    void guardarProyectoConSlugDuplicadoLanzaExcepcion() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(proyectoRepository.existsByEmpresaIdAndSlug(1L, "residencial-el-bosque")).thenReturn(true);

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> proyectoService.guardar(requestDTO));
        assertTrue(excepcion.getMessage().contains("Ya existe un proyecto con el slug"));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void alDestacarUnProyectoSeLeQuitaElDestacadoAlosDemasDeLaEmpresa() {
        requestDTO.setDestacado(true);
        proyecto.setDestacado(true);
        when(proyectoRepository.existsByEmpresaIdAndSlug(1L, "residencial-el-bosque")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(proyectoMapper.toEntity(requestDTO, empresa, null, null)).thenReturn(proyecto);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(proyectoMapper.toDTO(proyecto)).thenReturn(responseDTO);

        proyectoService.guardar(requestDTO);

        verify(proyectoRepository).quitarDestacadoDeOtros(1L, 10L);
    }

    @Test
    void siElProyectoNoEsDestacadoNoSeTocanLosDemas() {
        proyecto.setDestacado(false);
        when(proyectoRepository.existsByEmpresaIdAndSlug(1L, "residencial-el-bosque")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(proyectoMapper.toEntity(requestDTO, empresa, null, null)).thenReturn(proyecto);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(proyectoMapper.toDTO(proyecto)).thenReturn(responseDTO);

        proyectoService.guardar(requestDTO);

        verify(proyectoRepository, never()).quitarDestacadoDeOtros(any(), any());
    }

    @Test
    void unProyectoNuevoEsOfertaComercialPorDefectoYNoEsDestacado() {
        assertEquals(TipoRegistroProyecto.OFERTA_COMERCIAL, new Proyecto().getTipoRegistro());
        assertFalse(new Proyecto().getDestacado());
        // En la petición no hay valores por defecto: una actualización parcial no debe borrar lo ya guardado
        assertNull(new ProyectoRequestDTO().getTipoRegistro());
        assertNull(new ProyectoRequestDTO().getDestacado());
    }
}