package com.constructora_backend.service;

import com.constructora_backend.entity.EstadoSolicitud;
import com.constructora_backend.entity.Rol;
import com.constructora_backend.entity.SolicitudContacto;
import com.constructora_backend.entity.TokenRecuperacion;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.repository.EstadoSolicitudRepository;
import com.constructora_backend.repository.RolRepository;
import com.constructora_backend.repository.SolicitudContactoRepository;
import com.constructora_backend.repository.TokenRecuperacionRepository;
import com.constructora_backend.repository.UsuarioRepository;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.security.UsuarioPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Con la base de datos real (y la migración V13 aplicada): las consultas nuevas (retención, borrado por correo,
 * administradores activos, invalidación de tokens) y las columnas nuevas. Todo se revierte al terminar cada prueba.
 */
@SpringBootTest
@Transactional
class SeguridadIntegracionTest {

    @Autowired private SolicitudContactoRepository solicitudes;
    @Autowired private EstadoSolicitudRepository estados;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RolRepository roles;
    @Autowired private TokenRecuperacionRepository tokens;
    @Autowired private UserDetailsServiceImpl userDetailsService;
    @PersistenceContext private EntityManager em;

    private SolicitudContacto solicitud(String correo) {
        EstadoSolicitud nuevo = estados.findFirstByNombreIgnoreCase("NUEVO").orElseThrow();
        SolicitudContacto s = new SolicitudContacto();
        s.setEstado(nuevo);
        s.setNombre("Prueba retención");
        s.setCorreo(correo);
        s.setIdioma("es");
        s.setCosentimientoDatos(true);
        s.setVersionPolitica("2026-10");
        s.setFechaConsentimiento(LocalDateTime.now());
        return solicitudes.saveAndFlush(s);
    }

    private void fecharCreacion(Long id, LocalDateTime fecha) {
        em.createNativeQuery("UPDATE solicitudes_contacto SET creado_en = ?1 WHERE id = ?2")
                .setParameter(1, fecha).setParameter(2, id).executeUpdate();
    }

    @Test
    void laRetencionBorraSoloLasSolicitudesAntiguas() {
        SolicitudContacto vieja = solicitud("vieja-" + UUID.randomUUID() + "@prueba.test");
        SolicitudContacto reciente = solicitud("reciente-" + UUID.randomUUID() + "@prueba.test");
        fecharCreacion(vieja.getId(), LocalDateTime.now().minusMonths(30));
        em.clear();

        solicitudes.eliminarAnterioresA(LocalDateTime.now().minusMonths(24));
        em.clear();

        assertFalse(solicitudes.findById(vieja.getId()).isPresent(), "la de 30 meses se borra");
        assertTrue(solicitudes.findById(reciente.getId()).isPresent(), "la reciente se conserva");
    }

    @Test
    void laPoliticaYElConsentimientoSePersisten() {
        SolicitudContacto s = solicitud("politica-" + UUID.randomUUID() + "@prueba.test");
        em.clear();

        SolicitudContacto leida = solicitudes.findById(s.getId()).orElseThrow();

        assertEquals("2026-10", leida.getVersionPolitica());
        assertNotNull(leida.getFechaConsentimiento());
    }

    @Test
    void eliminarPorCorreoNoDistingueMayusculasYSoloBorraEseCorreo() {
        String base = UUID.randomUUID().toString();
        SolicitudContacto a1 = solicitud("titular-" + base + "@prueba.test");
        SolicitudContacto a2 = solicitud("TITULAR-" + base + "@PRUEBA.test");
        SolicitudContacto otra = solicitud("otra-" + base + "@prueba.test");
        em.clear();

        int borradas = solicitudes.eliminarPorCorreo("titular-" + base + "@prueba.test");
        em.clear();

        assertEquals(2, borradas);
        assertFalse(solicitudes.findById(a1.getId()).isPresent());
        assertFalse(solicitudes.findById(a2.getId()).isPresent());
        assertTrue(solicitudes.findById(otra.getId()).isPresent());
    }

    private Usuario nuevoUsuario(Rol rol, boolean activo) {
        Usuario u = new Usuario();
        u.setRol(rol);
        u.setNombreCompleto("Prueba Seguridad");
        u.setCorreo("seg-" + UUID.randomUUID() + "@prueba.test");
        u.setPasswordHash("$2a$10$hashDePruebaNoValidoParaNadaxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        u.setActivo(activo);
        return usuarios.saveAndFlush(u);
    }

    @Test
    void cuentaLosAdministradoresActivos() {
        Rol admin = roles.findByNombre("ADMINISTRADOR").orElse(null);
        assumeTrue(admin != null, "la base de pruebas tiene el rol ADMINISTRADOR");
        long antes = usuarios.contarAdministradoresActivos();

        Usuario nuevo = nuevoUsuario(admin, true);
        assertEquals(antes + 1, usuarios.contarAdministradoresActivos());

        nuevo.setActivo(false);
        usuarios.saveAndFlush(nuevo);
        assertEquals(antes, usuarios.contarAdministradoresActivos(), "un administrador inactivo no cuenta");
    }

    @Test
    void tokenValidoDesdeSePersisteYLlegaAlUsuarioAutenticado() {
        Rol rol = roles.findAll().stream().findFirst().orElse(null);
        assumeTrue(rol != null, "hay al menos un rol");
        Usuario u = nuevoUsuario(rol, true);
        LocalDateTime desde = LocalDateTime.now().withNano(0).minusMinutes(5);
        u.setTokenValidoDesde(desde);
        usuarios.saveAndFlush(u);
        em.clear();

        var detalles = userDetailsService.loadUserByUsername(u.getCorreo());

        UsuarioPrincipal principal = assertInstanceOf(UsuarioPrincipal.class, detalles);
        assertEquals(desde, principal.getTokenValidoDesde());
        assertTrue(principal.isEnabled());
        long hace1h = System.currentTimeMillis() - 3_600_000;
        assertFalse(principal.aceptaTokenEmitidoEn(hace1h), "un token de hace una hora quedó revocado");
        assertTrue(principal.aceptaTokenEmitidoEn(System.currentTimeMillis()));
    }

    @Test
    void invalidarPendientesMarcaTodosLosTokensSinUsarDelUsuario() {
        Rol rol = roles.findAll().stream().findFirst().orElse(null);
        assumeTrue(rol != null, "hay al menos un rol");
        Usuario u = nuevoUsuario(rol, true);
        TokenRecuperacion pendiente1 = guardarToken(u, null);
        TokenRecuperacion pendiente2 = guardarToken(u, null);
        TokenRecuperacion usado = guardarToken(u, LocalDateTime.now().minusDays(1));
        em.clear();

        int invalidados = tokens.invalidarPendientes(u.getId(), LocalDateTime.now());
        em.clear();

        assertEquals(2, invalidados);
        assertNotNull(tokens.findById(pendiente1.getId()).orElseThrow().getUsadoEn());
        assertNotNull(tokens.findById(pendiente2.getId()).orElseThrow().getUsadoEn());
        assertNotNull(tokens.findById(usado.getId()).orElseThrow().getUsadoEn());
    }

    private TokenRecuperacion guardarToken(Usuario u, LocalDateTime usadoEn) {
        return tokens.saveAndFlush(TokenRecuperacion.builder()
                .usuario(u)
                .tokenHash(PasswordResetService.hashear(UUID.randomUUID().toString()))
                .expiraEn(LocalDateTime.now().plusMinutes(15))
                .usadoEn(usadoEn)
                .build());
    }
}
