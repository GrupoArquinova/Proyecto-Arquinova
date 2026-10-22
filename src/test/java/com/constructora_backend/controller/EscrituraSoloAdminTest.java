package com.constructora_backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guarda contra olvidos: todo método que escribe (POST, PUT, PATCH, DELETE) en un controlador debe llevar
 * @PreAuthorize, salvo la lista corta de endpoints públicos o de perfil propio. Además de la regla por defecto de
 * SecurityConfig, así cada método declara su protección y se ve al leerlo.
 */
class EscrituraSoloAdminTest {

    private static final List<Class<?>> CONTROLADORES = List.of(
            AIController.class, AuditoriaController.class, AuthController.class, CasaModeloController.class,
            CloudinaryController.class, ContenidoInstitucionalController.class, DocumentoController.class,
            EmpresaController.class, EstadoLoteController.class, EstadoSolicitudController.class, EtapaController.class,
            LoteController.class, MultimediaController.class, PasswordResetController.class, ProyectoController.class,
            Punto360Controller.class, ReporteController.class, SolicitudContactoController.class,
            UbicacionController.class, UsuarioController.class, ZonaComunController.class, ZonaComunImagenController.class);

    /** Escrituras que NO exigen administrador por diseño. */
    private static final Set<String> PUBLICOS = Set.of(
            "AIController.chatearConIA",
            "AuthController.login", "AuthController.logout", "AuthController.renovar",
            "PasswordResetController.forgotPassword", "PasswordResetController.resetPassword",
            "SolicitudContactoController.crearPublica",
            "UsuarioController.actualizar"); // perfil propio: el servicio decide qué puede cambiar cada quien

    private static boolean escribe(Method m) {
        return m.isAnnotationPresent(PostMapping.class) || m.isAnnotationPresent(PutMapping.class)
                || m.isAnnotationPresent(PatchMapping.class) || m.isAnnotationPresent(DeleteMapping.class);
    }

    @Test
    @DisplayName("Todo método de escritura de un controlador lleva @PreAuthorize de administrador (salvo los públicos listados)")
    void todaEscrituraEstaProtegida() {
        List<String> sinProteger = new ArrayList<>();
        for (Class<?> controlador : CONTROLADORES) {
            for (Method m : controlador.getDeclaredMethods()) {
                if (!escribe(m)) continue;
                String nombre = controlador.getSimpleName() + "." + m.getName();
                if (PUBLICOS.contains(nombre)) continue;
                PreAuthorize pre = m.getAnnotation(PreAuthorize.class);
                if (pre == null || !pre.value().contains("hasRole('ADMINISTRADOR')")) {
                    sinProteger.add(nombre);
                }
            }
        }
        assertTrue(sinProteger.isEmpty(), "Escrituras sin @PreAuthorize de administrador: " + sinProteger);
    }

    @Test
    @DisplayName("Proyectos y empresas: crear, actualizar y desactivar exigen administrador")
    void proyectosYEmpresasProtegidos() throws NoSuchMethodException {
        for (Class<?> controlador : List.of(ProyectoController.class, EmpresaController.class)) {
            int escrituras = 0;
            for (Method m : controlador.getDeclaredMethods()) {
                if (!escribe(m)) continue;
                escrituras++;
                PreAuthorize pre = m.getAnnotation(PreAuthorize.class);
                assertTrue(pre != null && pre.value().equals("hasRole('ADMINISTRADOR')"),
                        controlador.getSimpleName() + "." + m.getName() + " debe exigir ADMINISTRADOR");
            }
            assertTrue(escrituras == 3, controlador.getSimpleName() + " tiene POST, PUT y DELETE");
        }
    }
}
