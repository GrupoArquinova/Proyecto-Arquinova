package com.constructora_backend.controller;

import com.constructora_backend.dto.response.ContenidoInstitucionalResponseDTO;
import com.constructora_backend.dto.response.ProyectoResponseDTO;
import com.constructora_backend.service.ContenidoInstitucionalService;
import com.constructora_backend.service.MultimediaService;
import com.constructora_backend.service.ProyectoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Los endpoints de lectura abiertos al sitio público solo deben exponer contenido publicado,
 * mientras que un ADMINISTRADOR conserva la vista completa.
 */
@ExtendWith(MockitoExtension.class)
class LecturaPublicaControllerTest {

    private static final Authentication ADMIN = new TestingAuthenticationToken("admin", "x", "ROLE_ADMINISTRADOR");

    @Mock private ProyectoService proyectoService;
    @Mock private MultimediaService multimediaService;
    @Mock private ContenidoInstitucionalService contenidoService;

    @InjectMocks private ProyectoController proyectoController;
    @InjectMocks private MultimediaController multimediaController;
    @InjectMocks private ContenidoInstitucionalController contenidoController;

    private ProyectoResponseDTO proyecto(boolean publicado, boolean activo) {
        ProyectoResponseDTO dto = new ProyectoResponseDTO();
        dto.setId(1L);
        dto.setPublicado(publicado);
        dto.setActivo(activo);
        return dto;
    }

    @Test
    @DisplayName("Proyectos: sin sesion el listado usa solo publicados aunque se pida todo")
    void listadoProyectosPublicoSoloPublicados() {
        when(proyectoService.listarPublicados()).thenReturn(List.of(proyecto(true, true)));

        var respuesta = proyectoController.listar(false, null);

        assertEquals(1, respuesta.getBody().size());
        verify(proyectoService).listarPublicados();
    }

    @Test
    @DisplayName("Proyectos: el administrador ve todos")
    void listadoProyectosAdminVeTodos() {
        when(proyectoService.listarTodos()).thenReturn(List.of(proyecto(true, true), proyecto(false, true)));

        assertEquals(2, proyectoController.listar(false, ADMIN).getBody().size());
    }

    @Test
    @DisplayName("Proyectos: un proyecto no publicado responde 404 al publico pero no al admin")
    void detalleProyectoNoPublicado() {
        when(proyectoService.obtenerPorId(1L)).thenReturn(Optional.of(proyecto(false, true)));

        assertEquals(404, proyectoController.obtenerPorId(1L, null).getStatusCode().value());
        assertEquals(200, proyectoController.obtenerPorId(1L, ADMIN).getStatusCode().value());
    }

    @Test
    @DisplayName("Proyectos: un proyecto inactivo tampoco se expone al publico")
    void detalleProyectoInactivo() {
        when(proyectoService.obtenerPorId(1L)).thenReturn(Optional.of(proyecto(true, false)));

        assertEquals(404, proyectoController.obtenerPorId(1L, null).getStatusCode().value());
    }

    @Test
    @DisplayName("Multimedia: sin sesion se fuerza soloPublicados=true")
    void multimediaPublicoForzaSoloPublicados() {
        multimediaController.listarPorEntidad("proyecto", 1L, false, null);

        verify(multimediaService).listarPorEntidad("proyecto", 1L, true);
    }

    @Test
    @DisplayName("Multimedia: el administrador puede ver tambien lo no publicado")
    void multimediaAdminVeTodo() {
        multimediaController.listarPorEntidad("proyecto", 1L, false, ADMIN);

        verify(multimediaService).listarPorEntidad("proyecto", 1L, false);
    }

    @Test
    @DisplayName("Contenido institucional: sin sesion solo se listan los publicados")
    void contenidoPublicoSoloPublicados() {
        when(contenidoService.listarPublicadosPorEmpresa(1L)).thenReturn(List.of());

        assertTrue(contenidoController.listarPorEmpresa(1L, false, null).getBody().isEmpty());
        verify(contenidoService).listarPublicadosPorEmpresa(1L);
    }

    @Test
    @DisplayName("Contenido institucional: una seccion sin publicar responde 404 al publico")
    void contenidoSeccionNoPublicada() {
        ContenidoInstitucionalResponseDTO borrador = new ContenidoInstitucionalResponseDTO();
        borrador.setPublicado(false);
        when(contenidoService.obtenerPorEmpresaYSeccion(1L, "RESPALDO")).thenReturn(Optional.of(borrador));

        assertEquals(404, contenidoController.obtenerPorEmpresaYSeccion(1L, "RESPALDO", null).getStatusCode().value());
        assertEquals(200, contenidoController.obtenerPorEmpresaYSeccion(1L, "RESPALDO", ADMIN).getStatusCode().value());
    }
}
