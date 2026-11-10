package com.constructora_backend.service;

import com.constructora_backend.dto.request.AuditoriaRequestDTO;
import com.constructora_backend.dto.response.AuditoriaResponseDTO;
import com.constructora_backend.entity.Auditoria;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.mapper.AuditoriaMapper;
import com.constructora_backend.repository.AuditoriaRepository;
import com.constructora_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Spy
    private AuditoriaMapper auditoriaMapper = new AuditoriaMapper();

    @InjectMocks
    private AuditoriaService auditoriaService;

    private Usuario usuario;
    private Auditoria auditoria;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombreCompleto("Admin Test");
        usuario.setCorreo("admin@test.com");

        auditoria = new Auditoria();
        auditoria.setId(10L);
        auditoria.setUsuario(usuario);
        auditoria.setAccion("CREAR_PROYECTO");
        auditoria.setEntidad("PROYECTOS");
        auditoria.setEntidadId(5L);
        auditoria.setDescripcion("Creación de proyecto campestre");
        auditoria.setIp("127.0.0.1");
        auditoria.setUserAgent("JUnit");
    }

    @Test
    @DisplayName("listarTodas debe retornar lista de DTOs de auditoría")
    void listarTodas_exito() {
        when(auditoriaRepository.findAll()).thenReturn(List.of(auditoria));

        List<AuditoriaResponseDTO> resultado = auditoriaService.listarTodas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("CREAR_PROYECTO", resultado.get(0).getAccion());
        assertEquals("PROYECTOS", resultado.get(0).getEntidad());
    }

    @Test
    @DisplayName("obtenerPorId debe retornar Optional con AuditoriaResponseDTO si existe")
    void obtenerPorId_exito() {
        when(auditoriaRepository.findById(10L)).thenReturn(Optional.of(auditoria));

        Optional<AuditoriaResponseDTO> resultado = auditoriaService.obtenerPorId(10L);

        assertTrue(resultado.isPresent());
        assertEquals("CREAR_PROYECTO", resultado.get().getAccion());
    }

    @Test
    @DisplayName("obtenerPorEntidad debe filtrar por entidad y ID")
    void obtenerPorEntidad_exito() {
        when(auditoriaRepository.findByEntidadAndEntidadId("PROYECTOS", 5L)).thenReturn(List.of(auditoria));

        List<AuditoriaResponseDTO> resultado = auditoriaService.obtenerPorEntidad("PROYECTOS", 5L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("PROYECTOS", resultado.get(0).getEntidad());
    }

    @Test
    @DisplayName("registrar debe guardar auditoría correctamente")
    void registrar_exito() {
        AuditoriaRequestDTO dto = new AuditoriaRequestDTO();
        dto.setAccion("ELIMINAR_LOTE");
        dto.setEntidad("LOTES");
        dto.setEntidadId(20L);
        dto.setUsuarioId(1L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(i -> {
            Auditoria a = i.getArgument(0);
            a.setId(100L);
            return a;
        });

        AuditoriaResponseDTO response = auditoriaService.registrar(dto);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("ELIMINAR_LOTE", response.getAccion());
        verify(auditoriaRepository).save(any(Auditoria.class));
    }

    @Test
    @DisplayName("registrarEvento debe capturar IP y User-Agent desde HttpServletRequest")
    void registrarEvento_exito() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("User-Agent", "Mozilla/5.0 Test");

        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(i -> {
            Auditoria a = i.getArgument(0);
            a.setId(101L);
            return a;
        });

        AuditoriaResponseDTO response = auditoriaService.registrarEvento("LOGIN_EXITOSO", "AUTH", null, "Inicio de sesión exitoso", request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("LOGIN_EXITOSO", response.getAccion());
        assertEquals("192.168.1.100", response.getIp());
        assertEquals("Mozilla/5.0 Test", response.getUserAgent());
    }
}