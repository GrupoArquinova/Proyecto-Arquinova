package com.constructora_backend.service;

import com.constructora_backend.aspect.AuditoriaDescriptor;
import com.constructora_backend.dto.response.AuditoriaResponseDTO;
import com.constructora_backend.dto.response.PaginaDTO;
import com.constructora_backend.entity.Auditoria;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Rol;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.enums.TipoRegistroProyecto;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.repository.AuditoriaRepository;
import com.constructora_backend.repository.RolRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Historial de cambios con la base real: instantáneas, descripciones, búsqueda paginada y retención. Todo se revierte. */
@SpringBootTest
@Transactional
class HistorialCambiosIntegracionTest {

    @Autowired private AuditoriaDescriptor descriptor;
    @Autowired private AuditoriaService auditoriaService;
    @Autowired private AuditoriaRepository auditoriaRepository;
    @Autowired private RolRepository roles;
    @PersistenceContext private EntityManager em;

    // ------------------------------------------------------------------ descripciones con datos reales

    @Test
    @DisplayName("Editar un proyecto: la descripción dice cómo se llama y qué campos cambiaron, sin copiar los textos")
    void descripcionDeUnaEdicionReal() {
        Empresa empresa = FixturasContenido.empresa(em);
        Proyecto p = FixturasContenido.proyecto(em, empresa, "El Encanto " + FixturasContenido.sufijo(), false, true, TipoRegistroProyecto.OFERTA_COMERCIAL);

        AuditoriaDescriptor.Instantanea antes = descriptor.capturar("PROYECTOS", p.getId());
        assertNotNull(antes);
        assertEquals(p.getNombre(), antes.nombre());

        p.setPublicado(true);
        p.setDescripcion("Un texto completamente distinto que NO debe aparecer en el historial");
        em.flush();
        AuditoriaDescriptor.Instantanea despues = descriptor.capturar("PROYECTOS", p.getId());

        String texto = descriptor.describir("ACTUALIZAR_PROYECTO", "PROYECTOS", p.getId(), antes, despues, "fija");

        assertEquals("Editó proyecto '" + p.getNombre() + "' (cambió: descripcion, publicado: no → sí)", texto);
        assertFalse(texto.contains("completamente distinto"));
    }

    @Test
    @DisplayName("Eliminar: el nombre sale de la lectura previa; y una fila inexistente no rompe nada")
    void eliminarYFilaInexistente() {
        assertEquals(null, descriptor.capturar("PROYECTOS", 987654321L));
        assertEquals("fija", descriptor.describir("ELIMINAR_PROYECTO", "DOCUMENTOS", 1L, null, null, "fija"));
    }

    @Test
    @DisplayName("Usuarios: la instantánea incluye el rol pero jamás la contraseña ni el hash ni los tokens")
    void usuarioSinSecretos() {
        Rol rol = roles.findByNombre("ADMINISTRADOR").orElseGet(() -> roles.findAll().get(0));
        Usuario u = new Usuario();
        u.setRol(rol);
        u.setNombreCompleto("Persona de Prueba");
        u.setCorreo("prueba-" + FixturasContenido.sufijo() + "@prueba.test");
        u.setPasswordHash("$2a$10$HASH-SECRETO-QUE-NO-DEBE-SALIR");
        u.setActivo(true);
        em.persist(u);
        em.flush();

        AuditoriaDescriptor.Instantanea foto = descriptor.capturar("USUARIOS", u.getId());

        assertNotNull(foto);
        assertEquals("Persona de Prueba", foto.nombre());
        assertTrue(foto.valores().containsKey("rol"));
        assertTrue(foto.valores().get("rol").startsWith("V:"));
        for (String campo : foto.valores().keySet()) {
            String minuscula = campo.toLowerCase();
            assertFalse(minuscula.contains("password") || minuscula.contains("hash") || minuscula.contains("token"),
                    "no debe salir el campo " + campo);
        }
        assertFalse(foto.valores().values().stream().anyMatch(v -> v != null && v.contains("SECRETO")));
    }

    @Test
    @DisplayName("Estados con id Integer (estados de lote) también se leen")
    void idsEnteros() {
        var estado = FixturasContenido.estadoLote(em, "DISPONIBLE");
        AuditoriaDescriptor.Instantanea foto = descriptor.capturar("ESTADOS_LOTE", estado.getId().longValue());
        assertNotNull(foto);
        assertEquals(estado.getNombre(), foto.nombre());
    }

    // ------------------------------------------------------------------ búsqueda paginada

    private Auditoria evento(String entidad, String accion, String descripcion, Usuario usuario) {
        Auditoria a = new Auditoria();
        a.setEntidad(entidad);
        a.setAccion(accion);
        a.setDescripcion(descripcion);
        a.setUsuario(usuario);
        return auditoriaRepository.saveAndFlush(a);
    }

    private void fechar(Auditoria a, LocalDateTime fecha) {
        em.createNativeQuery("UPDATE auditoria SET creado_en = ?1 WHERE id = ?2").setParameter(1, fecha).setParameter(2, a.getId()).executeUpdate();
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("Eliminar lo elegido: borra solo esos registros, lo anota en el historial y rechaza una selección vacía")
    void eliminarSeleccion() {
        String entidad = "ZZ_BORRAR_" + FixturasContenido.sufijo();
        Auditoria a1 = evento(entidad, "CREAR_COSA", "Uno", null);
        Auditoria a2 = evento(entidad, "CREAR_COSA", "Dos", null);
        Auditoria a3 = evento(entidad, "CREAR_COSA", "Tres", null);

        long eliminados = auditoriaService.eliminarSeleccion(java.util.Arrays.asList(a1.getId(), a2.getId(), a2.getId(), null, 987654321L), null);
        em.clear();

        assertEquals(2, eliminados, "los repetidos, los nulos y los que no existen no cuentan");
        assertFalse(auditoriaRepository.existsById(a1.getId()));
        assertFalse(auditoriaRepository.existsById(a2.getId()));
        assertTrue(auditoriaRepository.existsById(a3.getId()), "lo que no se eligió se queda");
        PaginaDTO<AuditoriaResponseDTO> anotacion = auditoriaService.buscar(null, "HISTORIAL", "ELIMINAR_HISTORIAL", null, null, null, 0, 5);
        assertTrue(anotacion.getContenido().stream().anyMatch(r -> r.getDescripcion().contains("Eliminó 2 registros")
                && r.getDescripcion().contains("selección manual")), "queda anotado quién limpió y cuántos");

        assertThrows(BadRequestException.class, () -> auditoriaService.eliminarSeleccion(java.util.List.of(), null));
        assertThrows(BadRequestException.class, () -> auditoriaService.eliminarSeleccion(null, null));
    }

    @Test
    @DisplayName("Eliminar por filtros: borra solo lo que coincide; sin ningún filtro exige confirmar y entonces borra todo menos la anotación")
    void eliminarFiltrados() {
        String entidad = "ZZ_FILTRO_" + FixturasContenido.sufijo();
        Auditoria viejo = evento(entidad, "CREAR_COSA", "Viejo", null);
        Auditoria reciente = evento(entidad, "CREAR_COSA", "Reciente", null);
        Auditoria otro = evento("ZZ_OTRA_" + FixturasContenido.sufijo(), "CREAR_COSA", "Otra entidad", null);
        fechar(viejo, LocalDateTime.of(2020, 1, 10, 8, 0));
        fechar(reciente, LocalDateTime.of(2020, 3, 10, 8, 0));
        fechar(otro, LocalDateTime.of(2020, 1, 10, 8, 0));

        // Antes del 1 de marzo y de esa entidad: solo cae el viejo
        long n = auditoriaService.eliminarFiltrados(null, entidad, null, null, null, LocalDate.of(2020, 2, 28), false, null);
        em.clear();
        assertEquals(1, n);
        assertFalse(auditoriaRepository.existsById(viejo.getId()));
        assertTrue(auditoriaRepository.existsById(reciente.getId()));
        assertTrue(auditoriaRepository.existsById(otro.getId()), "otra entidad no se toca");

        // Sin filtros y sin confirmar: se rechaza y no se borra nada
        long antes = auditoriaRepository.count();
        assertThrows(BadRequestException.class, () -> auditoriaService.eliminarFiltrados(null, null, null, "  ", null, null, false, null));
        assertEquals(antes, auditoriaRepository.count());
        assertThrows(BadRequestException.class,
                () -> auditoriaService.eliminarFiltrados(null, null, null, null, LocalDate.of(2020, 6, 1), LocalDate.of(2020, 5, 1), true, null));

        // Sin filtros y confirmado: se vacía el historial y solo queda la anotación de la limpieza
        long todo = auditoriaService.eliminarFiltrados(null, null, null, null, null, null, true, null);
        em.clear();
        assertEquals(antes, todo);
        assertEquals(1, auditoriaRepository.count(), "queda solo la anotación de que se limpió");
        assertEquals("ELIMINAR_HISTORIAL", auditoriaRepository.findAll().get(0).getAccion());

        // Si ya no hay nada que borrar no se anota nada nuevo
        auditoriaService.eliminarFiltrados(null, "NO_EXISTE_" + FixturasContenido.sufijo(), null, null, null, null, false, null);
        assertEquals(1, auditoriaRepository.count());
    }

    @Test
    @DisplayName("Búsqueda: filtros por entidad, acción, texto, usuario y fechas; paginada y con lo más reciente primero")
    void busquedaFiltradaYPaginada() {
        String entidad = "ZZ_PRUEBA_" + FixturasContenido.sufijo();
        Rol rol = roles.findAll().get(0);
        Usuario autor = new Usuario();
        autor.setRol(rol);
        autor.setNombreCompleto("Autora de Prueba");
        autor.setCorreo("autora-" + FixturasContenido.sufijo() + "@prueba.test");
        autor.setPasswordHash("x");
        em.persist(autor);
        em.flush();

        Auditoria a1 = evento(entidad, "CREAR_COSA", "Creó cosa 'Uno'", autor);
        Auditoria a2 = evento(entidad, "ACTUALIZAR_COSA", "Editó cosa 'Dos' (100% listo)", null);
        Auditoria a3 = evento(entidad, "ACTUALIZAR_COSA", "Editó cosa 'Tres'", autor);
        fechar(a1, LocalDateTime.of(2020, 5, 10, 12, 0));
        fechar(a2, LocalDateTime.of(2020, 5, 11, 12, 0));
        fechar(a3, LocalDateTime.of(2020, 5, 12, 12, 0));

        // Todo de la entidad: 3 filas, en páginas de 2, lo más reciente primero
        PaginaDTO<AuditoriaResponseDTO> p0 = auditoriaService.buscar(null, entidad.toLowerCase(), null, null, null, null, 0, 2);
        assertEquals(3, p0.getTotalElementos());
        assertEquals(2, p0.getTotalPaginas());
        assertEquals(2, p0.getContenido().size());
        assertEquals(a3.getId(), p0.getContenido().get(0).getId());
        assertEquals("Autora de Prueba", p0.getContenido().get(0).getUsuarioNombre());
        PaginaDTO<AuditoriaResponseDTO> p1 = auditoriaService.buscar(null, entidad, null, null, null, null, 1, 2);
        assertEquals(1, p1.getContenido().size());
        assertEquals(a1.getId(), p1.getContenido().get(0).getId());

        // Por acción, por usuario, por texto (los comodines se toman literales) y por rango de fechas
        assertEquals(2, auditoriaService.buscar(null, entidad, "actualizar_cosa", null, null, null, 0, 25).getTotalElementos());
        assertEquals(2, auditoriaService.buscar(autor.getId(), entidad, null, null, null, null, 0, 25).getTotalElementos());
        assertEquals(1, auditoriaService.buscar(null, entidad, null, "100%", null, null, 0, 25).getTotalElementos());
        assertEquals(1, auditoriaService.buscar(null, entidad, null, "%", null, null, 0, 25).getTotalElementos(),
                "un % suelto solo coincide con el texto literal '%' (una fila), no con todo");
        assertEquals(2, auditoriaService.buscar(null, entidad, null, null, LocalDate.of(2020, 5, 11), LocalDate.of(2020, 5, 12), 0, 25).getTotalElementos());
        assertEquals(1, auditoriaService.buscar(null, entidad, null, null, null, LocalDate.of(2020, 5, 10), 0, 25).getTotalElementos(),
                "el día 'hasta' queda incluido completo");

        // El tamaño de página se acota a 100 y las fechas invertidas se rechazan
        assertEquals(AuditoriaService.MAX_POR_PAGINA, auditoriaService.buscar(null, entidad, null, null, null, null, 0, 5000).getTamano());
        assertThrows(BadRequestException.class,
                () -> auditoriaService.buscar(null, entidad, null, null, LocalDate.of(2020, 6, 1), LocalDate.of(2020, 5, 1), 0, 25));

        // Filtros del panel: la entidad de prueba aparece entre las registradas
        assertTrue(auditoriaService.filtrosDisponibles().getEntidades().contains(entidad));
        assertTrue(auditoriaService.filtrosDisponibles().getAcciones().contains("CREAR_COSA"));
    }

    @Test
    @DisplayName("Retención: borra solo el historial anterior a la fecha límite")
    void retencionDelHistorial() {
        String entidad = "ZZ_RETENCION_" + FixturasContenido.sufijo();
        Auditoria vieja = evento(entidad, "CREAR_COSA", "vieja", null);
        Auditoria reciente = evento(entidad, "CREAR_COSA", "reciente", null);
        fechar(vieja, LocalDateTime.now().minusMonths(30));

        int borrados = auditoriaService.eliminarAnterioresA(LocalDateTime.now().minusMonths(24));
        em.clear();

        assertTrue(borrados >= 1);
        assertFalse(auditoriaRepository.findById(vieja.getId()).isPresent());
        assertTrue(auditoriaRepository.findById(reciente.getId()).isPresent());
    }
}
