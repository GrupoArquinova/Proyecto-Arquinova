package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.response.ProyectoResponseDTO;
import com.constructora_backend.dto.response.UbicacionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Que el historial de cambios no deje agujeros, y que la consulta pública de un proyecto trae lo necesario para el SEO. */
class CoberturaDeAuditoriaTest {

    /** Controladores que cambian contenido, usuarios, solicitudes o lotes: toda escritura debe quedar en el historial. */
    private static final List<Class<?>> CONTROLADORES_AUDITADOS = List.of(
            CasaModeloController.class, ContenidoInstitucionalController.class, DocumentoController.class,
            EmpresaController.class, EstadoLoteController.class, EstadoSolicitudController.class, EtapaController.class,
            LoteController.class, MultimediaController.class, ProyectoController.class, Punto360Controller.class,
            SolicitudContactoController.class, UbicacionController.class, UsuarioController.class,
            ZonaComunController.class, ZonaComunImagenController.class);

    /** Escrituras públicas o de sesión que no son un cambio de contenido (no se auditan). */
    private static final Set<String> EXCLUIDAS = Set.of("SolicitudContactoController.crearPublica");

    private static boolean escribe(Method m) {
        return m.isAnnotationPresent(PostMapping.class) || m.isAnnotationPresent(PutMapping.class)
                || m.isAnnotationPresent(PatchMapping.class) || m.isAnnotationPresent(DeleteMapping.class);
    }

    @Test
    @DisplayName("Toda escritura de contenido, usuarios, solicitudes y lotes lleva @Auditable")
    void todaEscrituraSeAudita() {
        List<String> sinAuditar = new ArrayList<>();
        for (Class<?> controlador : CONTROLADORES_AUDITADOS) {
            for (Method m : controlador.getDeclaredMethods()) {
                String nombre = controlador.getSimpleName() + "." + m.getName();
                if (escribe(m) && !EXCLUIDAS.contains(nombre) && !m.isAnnotationPresent(Auditable.class)) {
                    sinAuditar.add(nombre);
                }
            }
        }
        assertTrue(sinAuditar.isEmpty(), "Escrituras sin @Auditable: " + sinAuditar);
    }

    @Test
    @DisplayName("Los cambios de estado de lote y de solicitud, y las altas, ediciones y bajas de usuarios están auditados con su acción")
    void accionesClave() throws NoSuchMethodException {
        assertTrue(accionDe(LoteController.class, "cambiarEstado").equals("CAMBIAR_ESTADO_LOTE"));
        assertTrue(accionDe(LoteController.class, "toggleActivo").equals("ACTIVAR_DESACTIVAR_LOTE"));
        assertTrue(accionDe(SolicitudContactoController.class, "atenderSolicitud").equals("ATENDER_SOLICITUD_CONTACTO"));
        assertTrue(accionDe(UsuarioController.class, "crear").equals("CREAR_USUARIO"));
        assertTrue(accionDe(UsuarioController.class, "actualizar").equals("ACTUALIZAR_USUARIO"));
        assertTrue(accionDe(UsuarioController.class, "eliminar").equals("ELIMINAR_USUARIO"));
    }

    private static String accionDe(Class<?> controlador, String metodo) {
        for (Method m : controlador.getDeclaredMethods()) {
            if (m.getName().equals(metodo) && m.isAnnotationPresent(Auditable.class)) {
                return m.getAnnotation(Auditable.class).accion();
            }
        }
        return "(sin @Auditable)";
    }

    @Test
    @DisplayName("GET /api/proyectos/{id} ya devuelve título, descripción, imagen de portada, empresa y fecha de modificación (sin cambios al contrato)")
    void elProyectoPublicoTraeLoNecesarioParaElSeo() {
        for (String campo : List.of("nombre", "descripcion", "descripcionEn", "imagenUrl", "empresaNombre", "slug", "actualizadoEn", "creadoEn")) {
            assertDoesNotThrow(() -> ProyectoResponseDTO.class.getDeclaredField(campo), "falta " + campo);
        }
        // La ubicación (ciudad, departamento) sale de GET /api/ubicaciones/proyecto/{id}, también público
        for (String campo : List.of("ciudad", "departamento", "direccion")) {
            assertDoesNotThrow(() -> UbicacionResponseDTO.class.getDeclaredField(campo));
        }
    }
}
