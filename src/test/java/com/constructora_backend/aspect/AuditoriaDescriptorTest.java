package com.constructora_backend.aspect;

import com.constructora_backend.aspect.AuditoriaDescriptor.Instantanea;
import com.constructora_backend.service.AuditoriaService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Descripciones legibles del historial de cambios y su aplicación desde el aspecto. */
class AuditoriaDescriptorTest {

    private final AuditoriaDescriptor descriptor = new AuditoriaDescriptor(mock(PlatformTransactionManager.class));

    private static Map<String, String> valores(String... paresClaveValor) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            m.put(paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return m;
    }

    @Test
    @DisplayName("Crear: 'Creó proyecto 'El Encanto''")
    void crear() {
        Instantanea despues = new Instantanea("El Encanto", valores("nombre", "T:1:10"));
        assertEquals("Creó proyecto 'El Encanto'",
                descriptor.describir("CREAR_PROYECTO", "PROYECTOS", 6L, null, despues, "fijo"));
    }

    @Test
    @DisplayName("Editar: lista los campos que cambiaron; los sí/no y las relaciones con su antes y después; los textos solo por nombre")
    void editarConCampos() {
        Instantanea antes = new Instantanea("El Encanto", valores(
                "descripcion", "T:111:30", "publicado", "V:no", "estado", "V:En diseño", "slug", "T:5:2"));
        Instantanea despues = new Instantanea("El Encanto", valores(
                "descripcion", "T:999:35", "publicado", "V:sí", "estado", "V:En construcción", "slug", "T:5:2"));

        String texto = descriptor.describir("ACTUALIZAR_PROYECTO", "PROYECTOS", 6L, antes, despues, "fijo");

        assertEquals("Editó proyecto 'El Encanto' (cambió: descripcion, estado: En diseño → En construcción, publicado: no → sí)", texto);
    }

    @Test
    @DisplayName("Editar sin cambios reales no inventa campos")
    void editarSinCambios() {
        Instantanea igual = new Instantanea("Etapa 1", valores("nombre", "T:1:7", "activo", "V:sí"));
        assertEquals("Editó etapa 'Etapa 1'", descriptor.describir("ACTUALIZAR_ETAPA", "ETAPAS", 3L, igual, igual, "fijo"));
    }

    @Test
    @DisplayName("Eliminar: usa el nombre que tenía antes de borrarse")
    void eliminarUsaElNombreAnterior() {
        Instantanea antes = new Instantanea("LT-101", valores("codigo", "T:1:6"));
        assertEquals("Eliminó lote 'LT-101'", descriptor.describir("ELIMINAR_LOTE", "LOTES", 9L, antes, null, "fijo"));
    }

    @Test
    @DisplayName("Cambio de estado de lote y atención de solicitud tienen frase propia")
    void frasesPropias() {
        Instantanea antes = new Instantanea("LT-101", valores("estado", "V:DISPONIBLE"));
        Instantanea despues = new Instantanea("LT-101", valores("estado", "V:RESERVADO"));
        assertEquals("Cambió el estado del lote 'LT-101' (cambió: estado: DISPONIBLE → RESERVADO)",
                descriptor.describir("CAMBIAR_ESTADO_LOTE", "LOTES", 9L, antes, despues, "fijo"));

        // La solicitud se identifica por su número, nunca por el nombre de la persona
        Instantanea sa = new Instantanea(null, valores("estado", "V:NUEVO", "observacionesInternas", "T:0:0"));
        Instantanea sd = new Instantanea(null, valores("estado", "V:CONTACTADO", "observacionesInternas", "T:44:12"));
        assertEquals("Atendió la solicitud de contacto #12 (cambió: estado: NUEVO → CONTACTADO, observacionesInternas)",
                descriptor.describir("ATENDER_SOLICITUD_CONTACTO", "SOLICITUDES_CONTACTO", 12L, sa, sd, "fijo"));
    }

    @Test
    @DisplayName("Usuarios: se ve el cambio de rol y de estado, y nunca la contraseña")
    void usuarioCambioDeRol() {
        Instantanea antes = new Instantanea("Ana Pérez", valores("rol", "V:EDITOR", "activo", "V:sí", "nombreCompleto", "T:1:9"));
        Instantanea despues = new Instantanea("Ana Pérez", valores("rol", "V:COMERCIAL", "activo", "V:no", "nombreCompleto", "T:1:9"));
        assertEquals("Editó usuario 'Ana Pérez' (cambió: activo: sí → no, rol: EDITOR → COMERCIAL)",
                descriptor.describir("ACTUALIZAR_USUARIO", "USUARIOS", 4L, antes, despues, "fijo"));

        assertTrue(AuditoriaDescriptor.esCampoSensible("passwordHash"));
        assertTrue(AuditoriaDescriptor.esCampoSensible("password"));
        assertTrue(AuditoriaDescriptor.esCampoSensible("tokenValidoDesde"));
        assertTrue(AuditoriaDescriptor.esCampoSensible("clientSecret"));
        assertFalse(AuditoriaDescriptor.esCampoSensible("nombreCompleto"));
        assertFalse(AuditoriaDescriptor.esCampoSensible("rol"));
    }

    @Test
    @DisplayName("Entidad o acción sin frase propia: se conserva la descripción fija de @Auditable")
    void sinFrasePropiaSeUsaLaFija() {
        assertEquals("fijo", descriptor.describir("EXPORTAR_EXCEL_LOTES", "DOCUMENTOS", 1L, null, null, "fijo"));
        assertEquals("fijo", descriptor.describir("CONSULTAR_DASHBOARD", "PROYECTOS", 1L, null, null, "fijo"));
        assertEquals("fijo", descriptor.describir(null, "PROYECTOS", 1L, null, null, "fijo"));
    }

    @Test
    @DisplayName("La descripción nunca supera los 500 caracteres de la columna ni lleva saltos de línea")
    void largoYSaltos() {
        String texto = AuditoriaDescriptor.construir("Editó", "proyecto", "x".repeat(900), 1L, "campo");
        assertEquals(500, texto.length());
        assertTrue(texto.endsWith("..."));
        assertFalse(AuditoriaDescriptor.construir("Creó", "proyecto", "línea1\nlínea2\r", 1L, "").contains("\n"));
    }

    @Test
    @DisplayName("capturar no revienta si la entidad no se conoce o no hay id")
    void capturarSinDatos() {
        assertEquals(null, descriptor.capturar("DOCUMENTOS", 1L));
        assertEquals(null, descriptor.capturar("PROYECTOS", null));
        assertTrue(descriptor.conoce("PROYECTOS"));
        assertFalse(descriptor.conoce("DOCUMENTOS"));
    }

    // ------------------------------------------------------------------ el aspecto

    @Test
    @DisplayName("El aspecto toma la instantánea antes (por el id de la ruta), ejecuta y registra la descripción armada")
    void elAspectoUsaElDescriptor() throws Throwable {
        AuditoriaService servicio = mock(AuditoriaService.class);
        AuditoriaDescriptor desc = mock(AuditoriaDescriptor.class);
        AuditoriaAspect aspecto = new AuditoriaAspect(servicio, desc);
        ProceedingJoinPoint punto = mock(ProceedingJoinPoint.class);
        Auditable auditable = mock(Auditable.class);
        Instantanea antes = new Instantanea("Viejo", Map.of());
        Instantanea despues = new Instantanea("Nuevo", Map.of());

        when(auditable.accion()).thenReturn("ACTUALIZAR_PROYECTO");
        when(auditable.entidad()).thenReturn("PROYECTOS");
        when(auditable.descripcion()).thenReturn("fija");
        when(punto.getArgs()).thenReturn(new Object[]{6L, "cuerpo"});
        when(punto.proceed()).thenReturn(ResponseEntity.ok("listo"));
        when(desc.conoce("PROYECTOS")).thenReturn(true);
        when(desc.capturar("PROYECTOS", 6L)).thenReturn(antes, despues);
        when(desc.describir("ACTUALIZAR_PROYECTO", "PROYECTOS", 6L, antes, despues, "fija")).thenReturn("Editó proyecto 'Nuevo'");

        aspecto.auditarOperacion(punto, auditable);

        verify(servicio).registrarEvento(eq("ACTUALIZAR_PROYECTO"), eq("PROYECTOS"), eq(6L), eq("Editó proyecto 'Nuevo'"), any());
    }

    @Test
    @DisplayName("En una baja no se vuelve a leer la fila (ya no existe) y el id Integer de los estados también se reconoce")
    void bajaYEnterosComoId() throws Throwable {
        AuditoriaService servicio = mock(AuditoriaService.class);
        AuditoriaDescriptor desc = mock(AuditoriaDescriptor.class);
        AuditoriaAspect aspecto = new AuditoriaAspect(servicio, desc);
        ProceedingJoinPoint punto = mock(ProceedingJoinPoint.class);
        Auditable auditable = mock(Auditable.class);
        Instantanea antes = new Instantanea("Reservado", Map.of());

        when(auditable.accion()).thenReturn("ELIMINAR_ESTADO_LOTE");
        when(auditable.entidad()).thenReturn("ESTADOS_LOTE");
        when(auditable.descripcion()).thenReturn("fija");
        when(punto.getArgs()).thenReturn(new Object[]{3});
        when(punto.proceed()).thenReturn(ResponseEntity.noContent().build());
        when(desc.conoce("ESTADOS_LOTE")).thenReturn(true);
        when(desc.capturar("ESTADOS_LOTE", 3L)).thenReturn(antes);
        when(desc.describir(anyString(), anyString(), eq(3L), eq(antes), eq(null), anyString())).thenReturn("Eliminó estado de lote 'Reservado'");

        aspecto.auditarOperacion(punto, auditable);

        verify(desc).capturar("ESTADOS_LOTE", 3L); // una sola lectura: la previa
        verify(servicio).registrarEvento(eq("ELIMINAR_ESTADO_LOTE"), eq("ESTADOS_LOTE"), eq(3L), eq("Eliminó estado de lote 'Reservado'"), any());
    }

    @Test
    @DisplayName("Si el descriptor falla, el aspecto no rompe la operación del usuario")
    void fallaDelDescriptorNoRompe() throws Throwable {
        AuditoriaService servicio = mock(AuditoriaService.class);
        AuditoriaDescriptor desc = mock(AuditoriaDescriptor.class);
        AuditoriaAspect aspecto = new AuditoriaAspect(servicio, desc);
        ProceedingJoinPoint punto = mock(ProceedingJoinPoint.class);
        Auditable auditable = mock(Auditable.class);

        when(auditable.accion()).thenReturn("CREAR_PROYECTO");
        when(auditable.entidad()).thenReturn("PROYECTOS");
        when(auditable.descripcion()).thenReturn("fija");
        when(punto.getArgs()).thenReturn(new Object[]{});
        when(punto.proceed()).thenReturn("resultado");
        when(desc.conoce("PROYECTOS")).thenReturn(true);
        when(desc.capturar(any(), any())).thenThrow(new IllegalStateException("BD caída"));

        assertEquals("resultado", aspecto.auditarOperacion(punto, auditable));
        verify(servicio, never()).registrarEvento(any(), any(), any(), anyString(), any());
    }
}
