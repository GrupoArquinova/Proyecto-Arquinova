package com.constructora_backend.security;

import com.constructora_backend.config.SecurityConfig;
import com.constructora_backend.controller.CloudinaryController;
import com.constructora_backend.controller.DocumentoController;
import com.constructora_backend.controller.EmpresaController;
import com.constructora_backend.controller.EstadoLoteController;
import com.constructora_backend.controller.EstadoSolicitudController;
import com.constructora_backend.controller.HistorialCambiosController;
import com.constructora_backend.controller.LoteController;
import com.constructora_backend.controller.MetricasController;
import com.constructora_backend.controller.ProyectoController;
import com.constructora_backend.controller.ReporteController;
import com.constructora_backend.controller.RolController;
import com.constructora_backend.controller.SolicitudContactoController;
import com.constructora_backend.controller.UsuarioController;
import com.constructora_backend.controller.AuditoriaController;
import com.constructora_backend.repository.RolRepository;
import com.constructora_backend.service.AuditoriaService;
import com.constructora_backend.service.CloudinaryFirmaService;
import com.constructora_backend.service.EmpresaService;
import com.constructora_backend.service.EstadoLoteService;
import com.constructora_backend.service.EstadoSolicitudService;
import com.constructora_backend.service.LoteService;
import com.constructora_backend.service.MetricasService;
import com.constructora_backend.service.ProyectoService;
import com.constructora_backend.service.ReporteDocumentoService;
import com.constructora_backend.service.ReporteService;
import com.constructora_backend.service.SolicitudContactoService;
import com.constructora_backend.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Matriz rol x endpoint con la seguridad REAL (SecurityConfig + @PreAuthorize) y los servicios simulados.
 * "Permitido" significa que la seguridad deja pasar (la respuesta no es 401 ni 403: con servicios simulados y cuerpos vacíos
 * puede ser 200, 201, 204, 400 o 404); "prohibido" es 403 exacto.
 */
@WebMvcTest(controllers = {
        ProyectoController.class, LoteController.class, EstadoLoteController.class, SolicitudContactoController.class,
        EstadoSolicitudController.class, UsuarioController.class, AuditoriaController.class, HistorialCambiosController.class,
        MetricasController.class, RolController.class, EmpresaController.class, ReporteController.class,
        DocumentoController.class, CloudinaryController.class})
@Import({SecurityConfig.class, JwtFilter.class})
class PermisosPorRolTest {

    private static final String ADMIN = "ADMINISTRADOR";
    private static final String COMERCIAL = "COMERCIAL";
    private static final String EDITOR = "EDITOR";

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean(name = "userSecurity") private UserSecurityHelper userSecurity;

    @MockBean private ProyectoService proyectoService;
    @MockBean private LoteService loteService;
    @MockBean private EstadoLoteService estadoLoteService;
    @MockBean private SolicitudContactoService solicitudService;
    @MockBean private EstadoSolicitudService estadoSolicitudService;
    @MockBean private UsuarioService usuarioService;
    @MockBean private AuditoriaService auditoriaService;
    @MockBean private MetricasService metricasService;
    @MockBean private RolRepository rolRepository;
    @MockBean private EmpresaService empresaService;
    @MockBean private ReporteService reporteService;
    @MockBean private ReporteDocumentoService reporteDocumentoService;
    @MockBean private CloudinaryFirmaService cloudinaryFirmaService;

    /** Una fila de la matriz: método, ruta y quiénes pasan (el resto debe recibir 403). */
    private record Caso(HttpMethod metodo, String ruta, Set<String> permitidos) {
        @Override
        public String toString() {
            return metodo + " " + ruta;
        }
    }

    private static Caso caso(HttpMethod metodo, String ruta, String... permitidos) {
        return new Caso(metodo, ruta, Set.of(permitidos));
    }

    private static final List<Caso> MATRIZ = List.of(
            // Contenido: administrador y editor
            caso(HttpMethod.POST, "/api/proyectos", ADMIN, EDITOR),
            caso(HttpMethod.PUT, "/api/proyectos/1", ADMIN, EDITOR),
            caso(HttpMethod.DELETE, "/api/proyectos/1", ADMIN, EDITOR),
            caso(HttpMethod.PATCH, "/api/lotes/1/estado", ADMIN, EDITOR),
            caso(HttpMethod.PATCH, "/api/lotes/1/activo", ADMIN, EDITOR),
            caso(HttpMethod.DELETE, "/api/lotes/1", ADMIN, EDITOR),
            caso(HttpMethod.POST, "/api/estados-lote", ADMIN, EDITOR),
            caso(HttpMethod.POST, "/api/empresas", ADMIN, EDITOR),
            caso(HttpMethod.GET, "/api/empresas", ADMIN, EDITOR),
            caso(HttpMethod.POST, "/api/cloudinary/firma", ADMIN, EDITOR),
            // Lectura interna de proyectos y lotes: también el comercial
            caso(HttpMethod.GET, "/api/lotes", ADMIN, EDITOR, COMERCIAL),
            caso(HttpMethod.GET, "/api/lotes/1/historial", ADMIN, EDITOR, COMERCIAL),
            caso(HttpMethod.GET, "/api/estados-lote", ADMIN, EDITOR, COMERCIAL),
            // Solicitudes: administrador y comercial; borrarlas solo el administrador
            caso(HttpMethod.GET, "/api/solicitudes-contacto", ADMIN, COMERCIAL),
            caso(HttpMethod.GET, "/api/solicitudes-contacto/1", ADMIN, COMERCIAL),
            caso(HttpMethod.PATCH, "/api/solicitudes-contacto/1/atender", ADMIN, COMERCIAL),
            caso(HttpMethod.DELETE, "/api/solicitudes-contacto/1", ADMIN),
            caso(HttpMethod.DELETE, "/api/solicitudes-contacto/por-correo?correo=a@b.co", ADMIN),
            caso(HttpMethod.GET, "/api/estados-solicitud", ADMIN, COMERCIAL),
            caso(HttpMethod.POST, "/api/estados-solicitud", ADMIN, COMERCIAL),
            caso(HttpMethod.DELETE, "/api/estados-solicitud/1", ADMIN, COMERCIAL),
            // Métricas: administrador y comercial
            caso(HttpMethod.GET, "/api/admin/metricas", ADMIN, COMERCIAL),
            // Solo administrador
            caso(HttpMethod.GET, "/api/usuarios", ADMIN),
            caso(HttpMethod.POST, "/api/usuarios", ADMIN),
            caso(HttpMethod.DELETE, "/api/usuarios/9", ADMIN),
            caso(HttpMethod.GET, "/api/roles", ADMIN),
            caso(HttpMethod.GET, "/api/auditoria", ADMIN),
            caso(HttpMethod.POST, "/api/auditoria", ADMIN),
            caso(HttpMethod.GET, "/api/admin/auditoria", ADMIN),
            caso(HttpMethod.GET, "/api/admin/auditoria/filtros", ADMIN),
            caso(HttpMethod.POST, "/api/admin/auditoria/eliminar-seleccion", ADMIN),
            caso(HttpMethod.POST, "/api/admin/auditoria/eliminar-filtrados", ADMIN),
            caso(HttpMethod.GET, "/api/reportes/dashboard", ADMIN),
            caso(HttpMethod.GET, "/api/documentos/excel/solicitudes", ADMIN),
            caso(HttpMethod.POST, "/api/documentos/word/promesa-compraventa", ADMIN));

    private MockHttpServletRequestBuilder peticion(Caso caso, String rol) {
        MockHttpServletRequestBuilder b = MockMvcRequestBuilders.request(caso.metodo(), caso.ruta())
                .contentType(MediaType.APPLICATION_JSON).content("{}");
        if (rol != null) {
            String token = "tk-" + rol;
            when(jwtUtils.validarToken(token)).thenReturn(true);
            when(jwtUtils.obtenerCorreoDelToken(token)).thenReturn(token + "@arquinova.com");
            when(userDetailsService.loadUserByUsername(token + "@arquinova.com"))
                    .thenReturn(new User(token + "@arquinova.com", "x", List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
            b.header("Authorization", "Bearer " + token);
        }
        return b;
    }

    private int estado(Caso caso, String rol) throws Exception {
        MvcResult r = mockMvc.perform(peticion(caso, rol)).andReturn();
        return r.getResponse().getStatus();
    }

    @Test
    @DisplayName("Matriz rol x endpoint: cada rol pasa donde le corresponde y recibe 403 en todo lo demás")
    void matrizDePermisos() throws Exception {
        for (Caso c : MATRIZ) {
            for (String rol : List.of(ADMIN, COMERCIAL, EDITOR)) {
                int estado = estado(c, rol);
                if (c.permitidos().contains(rol)) {
                    assertTrue(estado != 401 && estado != 403, rol + " debería poder " + c + " pero recibió " + estado);
                } else {
                    assertEquals(403, estado, rol + " NO debería poder " + c + " pero recibió " + estado);
                }
            }
        }
    }

    @Test
    @DisplayName("Sin sesión nada protegido se puede usar: 401 (o 403 en las lecturas internas bajo rutas públicas, donde decide @PreAuthorize)")
    void sinSesionNadaProtegidoPasa() throws Exception {
        for (Caso c : MATRIZ) {
            int estado = estado(c, null);
            assertTrue(estado == 401 || estado == 403, "sin sesión " + c + " respondió " + estado);
        }
    }

    @Test
    @DisplayName("Regresión: un rol sin permiso no escala; un rol desconocido (ROLE_USUARIO) no puede nada de la matriz")
    void unRolDesconocidoNoPuedeNada() throws Exception {
        for (Caso c : MATRIZ) {
            assertEquals(403, estado(c, "USUARIO"), "ROLE_USUARIO no debe poder " + c);
        }
    }

    @Test
    @DisplayName("Regresión: COMERCIAL y EDITOR no editan a otra persona ni asignan roles (PUT /api/usuarios/{otro} es 403)")
    void ningunRolNoAdminEditaAOtroUsuario() throws Exception {
        when(userSecurity.isOwnerOrAdmin(2L)).thenReturn(false);
        for (String rol : List.of(COMERCIAL, EDITOR)) {
            assertEquals(403, estado(caso(HttpMethod.PUT, "/api/usuarios/2", ADMIN), rol), rol + " no debe editar a otro usuario");
            assertEquals(403, estado(caso(HttpMethod.GET, "/api/usuarios/2", ADMIN), rol), rol + " no debe leer a otro usuario");
        }
    }

    @Test
    @DisplayName("Regresión: COMERCIAL y EDITOR sí pueden entrar a su propio perfil (la regla es de dueño, no de rol)")
    void elPerfilPropioSigueAbierto() throws Exception {
        when(userSecurity.isOwnerOrAdmin(5L)).thenReturn(true);
        for (String rol : List.of(COMERCIAL, EDITOR)) {
            int estado = estado(caso(HttpMethod.PUT, "/api/usuarios/5", ADMIN), rol);
            assertTrue(estado != 401 && estado != 403, rol + " debe poder editar su perfil, recibió " + estado);
        }
    }

    @Test
    @DisplayName("Proyectos: administrador, editor y comercial ven también lo no publicado; sin sesión o con otro rol, solo lo publicado")
    void listadoDeProyectosSegunRol() throws Exception {
        when(proyectoService.listarTodos()).thenReturn(List.of());
        when(proyectoService.listarPublicados()).thenReturn(List.of());
        for (String rol : List.of(ADMIN, EDITOR, COMERCIAL)) {
            mockMvc.perform(peticion(caso(HttpMethod.GET, "/api/proyectos", ADMIN), rol)).andReturn();
        }
        verify(proyectoService, org.mockito.Mockito.times(3)).listarTodos();

        mockMvc.perform(peticion(caso(HttpMethod.GET, "/api/proyectos", ADMIN), null)).andReturn();
        mockMvc.perform(peticion(caso(HttpMethod.GET, "/api/proyectos", ADMIN), "USUARIO")).andReturn();
        verify(proyectoService, org.mockito.Mockito.times(2)).listarPublicados();
        // el listado público de lectura es abierto
        assertEquals(200, estado(caso(HttpMethod.GET, "/api/proyectos", ADMIN), null));
    }

    @Test
    @DisplayName("Métricas: el administrador recibe las preguntas recientes del chat y el comercial no (se pide sin ellas)")
    void metricasPreguntasSoloParaAdministrador() throws Exception {
        when(metricasService.obtener(any(), any(), anyBoolean())).thenReturn(null);
        mockMvc.perform(peticion(caso(HttpMethod.GET, "/api/admin/metricas?desde=2026-08-01&hasta=2026-10-31", ADMIN), ADMIN)).andReturn();
        verify(metricasService).obtener(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31), true);

        mockMvc.perform(peticion(caso(HttpMethod.GET, "/api/admin/metricas", ADMIN), COMERCIAL)).andReturn();
        verify(metricasService).obtener(null, null, false);
    }
}
