package com.constructora_backend.service;

import com.constructora_backend.entity.EventoAnalitica;
import com.constructora_backend.enums.TipoEventoAnalitica;
import com.constructora_backend.repository.EventoAnaliticaRepository;
import com.constructora_backend.repository.ProyectoRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnaliticaServiceTest {

    private EventoAnaliticaRepository eventos;
    private ProyectoRepository proyectos;
    private AnaliticaService servicio;

    @BeforeEach
    void preparar() {
        eventos = mock(EventoAnaliticaRepository.class);
        proyectos = mock(ProyectoRepository.class);
        servicio = new AnaliticaService(eventos, proyectos);
    }

    private EventoAnalitica guardado() {
        ArgumentCaptor<EventoAnalitica> captor = ArgumentCaptor.forClass(EventoAnalitica.class);
        verify(eventos).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Un evento válido del navegador se guarda con tipo, proyecto y ruta sin querystring")
    void guardaEventoValido() {
        when(proyectos.existsByIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(true);

        assertTrue(servicio.registrarDelNavegador(TipoEventoAnalitica.VISTA_PROYECTO, 6L, "/proyectos/6/ubicacion?utm_source=x&correo=a@b.co#mapa"));

        EventoAnalitica e = guardado();
        assertEquals(TipoEventoAnalitica.VISTA_PROYECTO, e.getTipo());
        assertEquals(6L, e.getProyectoId());
        assertEquals("/proyectos/6/ubicacion", e.getRuta());
    }

    @Test
    @DisplayName("Un proyecto inexistente o sin publicar se ignora en silencio (nada se guarda)")
    void proyectoNoPublicadoSeIgnora() {
        when(proyectos.existsByIdAndPublicadoTrueAndActivoTrue(99L)).thenReturn(false);

        assertFalse(servicio.registrarDelNavegador(TipoEventoAnalitica.VISTA_PROYECTO, 99L, "/proyectos/99"));
        assertFalse(servicio.registrarDelNavegador(TipoEventoAnalitica.CLICK_WHATSAPP, 99L, null));

        verify(eventos, never()).save(any());
    }

    @Test
    @DisplayName("Los tipos que registra el servidor (formulario, inicio de chat, brochure) se ignoran si los manda el navegador")
    void tiposDelServidorNoSeAceptanDelNavegador() {
        when(proyectos.existsByIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(true);

        for (TipoEventoAnalitica tipo : List.of(TipoEventoAnalitica.FORMULARIO_ENVIADO, TipoEventoAnalitica.INICIO_CHAT, TipoEventoAnalitica.DESCARGA_BROCHURE)) {
            assertTrue(tipo.esSoloServidor());
            assertFalse(servicio.registrarDelNavegador(tipo, 6L, "/x"), tipo + " no debe contarse dos veces");
        }
        assertFalse(servicio.registrarDelNavegador(null, null, null));
        verify(eventos, never()).save(any());
    }

    @Test
    @DisplayName("Vista de página y compartir no necesitan proyecto; vista de proyecto y clic de WhatsApp sí")
    void tiposQueExigenProyecto() {
        assertTrue(servicio.registrarDelNavegador(TipoEventoAnalitica.VISTA_PAGINA, null, "/"));
        assertTrue(servicio.registrarDelNavegador(TipoEventoAnalitica.COMPARTIR, null, "/proyectos"));
        assertFalse(servicio.registrarDelNavegador(TipoEventoAnalitica.VISTA_PROYECTO, null, "/proyectos/1"));
        assertFalse(servicio.registrarDelNavegador(TipoEventoAnalitica.CLICK_WHATSAPP, null, "/"));
    }

    @Test
    @DisplayName("El servidor registra sus eventos y un fallo de la base nunca sale como excepción")
    void eventosDelServidor() {
        servicio.registrarDelServidor(TipoEventoAnalitica.FORMULARIO_ENVIADO, 6L);
        EventoAnalitica e = guardado();
        assertEquals(TipoEventoAnalitica.FORMULARIO_ENVIADO, e.getTipo());
        assertEquals(6L, e.getProyectoId());
        assertNull(e.getRuta());

        when(eventos.save(any())).thenThrow(new RuntimeException("BD caída"));
        servicio.registrarDelServidor(TipoEventoAnalitica.INICIO_CHAT, null); // no lanza
    }

    @Test
    @DisplayName("Rutas: sin dominio, querystring ni fragmento; máximo 200 caracteres; sin caracteres de control")
    void sanitizarRuta() {
        assertEquals("/proyectos/6", AnaliticaService.sanitizarRuta("/proyectos/6?x=1"));
        assertEquals("/proyectos/6", AnaliticaService.sanitizarRuta("  /proyectos/6#seccion  "));
        assertEquals("/proyectos/6/lotes", AnaliticaService.sanitizarRuta("https://www.ejemplo.com/proyectos/6/lotes?token=abc"));
        assertEquals("/", AnaliticaService.sanitizarRuta("https://www.ejemplo.com"));
        assertEquals("/a/b", AnaliticaService.sanitizarRuta("/a/\nb"));
        assertEquals(200, AnaliticaService.sanitizarRuta("/" + "x".repeat(500)).length());
        assertNull(AnaliticaService.sanitizarRuta(null));
        assertNull(AnaliticaService.sanitizarRuta("   "));
        assertNull(AnaliticaService.sanitizarRuta("?solo=query"));
    }

    @Test
    @DisplayName("Privacidad: la tabla no tiene columnas de IP, user-agent ni identificador de visitante")
    void elEventoNoTieneDatosDeLaPersona() {
        assertEquals("eventos_analitica", EventoAnalitica.class.getAnnotation(Table.class).name());
        List<String> columnas = Arrays.stream(EventoAnalitica.class.getDeclaredFields()).map(Field::getName).toList();
        assertEquals(List.of("id", "tipo", "proyectoId", "ruta", "creadoEn"), columnas);
        for (Field f : EventoAnalitica.class.getDeclaredFields()) {
            Column c = f.getAnnotation(Column.class);
            String nombre = (c != null && !c.name().isEmpty() ? c.name() : f.getName()).toLowerCase();
            assertFalse(nombre.contains("ip") && !nombre.equals("tipo"), "columna sospechosa: " + nombre);
            assertFalse(nombre.contains("agent") || nombre.contains("visitante") || nombre.contains("cookie") || nombre.contains("usuario"));
        }
    }
}
