package com.constructora_backend.service;

import com.constructora_backend.dto.request.EmpresaRequestDTO;
import com.constructora_backend.dto.response.EmpresaResponseDTO;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.exception.DuplicateResourceException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.EmpresaMapper;
import com.constructora_backend.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Spy
    private EmpresaMapper empresaMapper = new EmpresaMapper();

    @InjectMocks
    private EmpresaService empresaService;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNombre("Constructora Conclave");
        empresa.setNit("900123456-1");
        empresa.setCorreoComercial("contacto@conclave.com");
        empresa.setActivo(true);
    }

    @Test
    @DisplayName("listarTodas debe retornar lista de EmpresaResponseDTO")
    void listarTodas_exito() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresa));

        List<EmpresaResponseDTO> resultado = empresaService.listarTodas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Constructora Conclave", resultado.get(0).getNombre());
        assertEquals("900123456-1", resultado.get(0).getNit());
    }

    @Test
    @DisplayName("obtenerPorId debe retornar Optional con DTO si existe")
    void obtenerPorId_exito() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));

        Optional<EmpresaResponseDTO> resultado = empresaService.obtenerPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals("Constructora Conclave", resultado.get().getNombre());
    }

    @Test
    @DisplayName("guardar debe registrar la empresa si el NIT no está en uso")
    void guardar_exito() {
        EmpresaRequestDTO dto = new EmpresaRequestDTO();
        dto.setNombre("Nueva Constructora");
        dto.setNit("900999888-7");

        when(empresaRepository.existsByNit("900999888-7")).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> {
            Empresa e = i.getArgument(0);
            e.setId(10L);
            return e;
        });

        EmpresaResponseDTO response = empresaService.guardar(dto);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Nueva Constructora", response.getNombre());
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("guardar debe lanzar DuplicateResourceException si el NIT ya existe")
    void guardar_nitDuplicado() {
        EmpresaRequestDTO dto = new EmpresaRequestDTO();
        dto.setNit("900123456-1");

        when(empresaRepository.existsByNit("900123456-1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> empresaService.guardar(dto));
        verify(empresaRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar debe modificar los datos si la empresa existe")
    void actualizar_exito() {
        EmpresaRequestDTO dto = new EmpresaRequestDTO();
        dto.setNombre("Conclave Modificado");
        dto.setNit("900123456-1");

        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));

        EmpresaResponseDTO response = empresaService.actualizar(1L, dto);

        assertNotNull(response);
        assertEquals("Conclave Modificado", response.getNombre());
    }

    @Test
    @DisplayName("desactivar debe cambiar el atributo activo a false")
    void desactivar_exito() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));

        empresaService.desactivar(1L);

        assertFalse(empresa.getActivo());
        verify(empresaRepository).save(empresa);
    }

    @Test
    @DisplayName("desactivar debe lanzar ResourceNotFoundException si la empresa no existe")
    void desactivar_noExiste() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> empresaService.desactivar(99L));
    }
}