package com.constructora_backend.service;

import com.constructora_backend.entity.ChatPregunta;
import com.constructora_backend.enums.TemaChat;
import com.constructora_backend.repository.ChatPreguntaRepository;
import jakarta.persistence.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

/** Clasificador de temas, omisión de datos personales y guardado de las preguntas del asistente. */
class ChatPreguntasTest {

    // ------------------------------------------------------------------ clasificador

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "¿Dónde queda El Encanto?|UBICACION",
            "DONDE ESTA UBICADO EL PROYECTO|UBICACION",
            "How far is it from Armenia?|UBICACION",
            "What is the location?|UBICACION",
            "¿Cuánto cuesta una villa?|PRECIOS",
            "cual es el precio de un lote|PRECIOS",
            "Do you offer financing? How much is it?|PRECIOS",
            "¿Qué lotes hay disponibles?|LOTES",
            "Tienen terrenos de 1000 m2|LOTES",
            "Are there lots available?|LOTES",
            "¿Cuántas habitaciones tiene la Villa Samán?|VILLAS",
            "How many bedrooms does the house have?|VILLAS",
            "¿Tienen piscina o jacuzzi?|AMENIDADES",
            "What amenities are there? Is there a gym?|AMENIDADES",
            "Quiero agendar una visita|CONTACTO_VISITA",
            "Can I schedule a tour with an advisor?|CONTACTO_VISITA",
            "¿Cuál es su whatsapp?|CONTACTO_VISITA",
            "¿Hacen diseño arquitectónico y licencias?|SERVICIOS",
            "Do you offer construction services?|SERVICIOS",
            "¿Qué proyectos tiene Arquinova?|PROYECTOS",
            "Tell me about your portfolio|PROYECTOS",
            "¿Hay recorrido virtual 360?|TOUR_PLANOS",
            "Can I see the floor plans?|TOUR_PLANOS",
            "Hola, buenos días|OTRO",
            "asdfgh qwerty|OTRO",
            "Pilot program|OTRO"})
    @DisplayName("Clasifica por palabras clave en español e inglés, sin importar tildes ni mayúsculas")
    void clasifica(String pregunta, TemaChat esperado) {
        assertEquals(esperado, ChatTemaClasificador.clasificar(pregunta));
    }

    @Test
    @DisplayName("Si aplican varias reglas gana la de mayor prioridad; vacío o nulo es OTRO")
    void prioridadYVacios() {
        // precio + lote => PRECIOS; visita + villa => CONTACTO_VISITA; ubicación + proyecto => UBICACION
        assertEquals(TemaChat.PRECIOS, ChatTemaClasificador.clasificar("precio de un lote"));
        assertEquals(TemaChat.CONTACTO_VISITA, ChatTemaClasificador.clasificar("quiero visitar la villa"));
        assertEquals(TemaChat.UBICACION, ChatTemaClasificador.clasificar("donde queda el proyecto"));
        assertEquals(TemaChat.OTRO, ChatTemaClasificador.clasificar(""));
        assertEquals(TemaChat.OTRO, ChatTemaClasificador.clasificar("   "));
        assertEquals(TemaChat.OTRO, ChatTemaClasificador.clasificar(null));
    }

    @Test
    @DisplayName("Tolera tildes, mayúsculas y signos")
    void normaliza() {
        assertEquals("donde esta la ubicacion", ChatTemaClasificador.normalizar("¿DÓNDE ESTÁ la ubicación?!"));
        assertEquals(ChatTemaClasificador.clasificar("ubicacion"), ChatTemaClasificador.clasificar("UBICACIÓN"));
        assertEquals(TemaChat.AMENIDADES, ChatTemaClasificador.clasificar("¿TIENEN PISCINA?"));
    }

    // ------------------------------------------------------------------ datos personales

    @Test
    @DisplayName("Los correos electrónicos se reemplazan por [dato omitido]")
    void omiteCorreos() {
        assertEquals("Escríbanme a [dato omitido] por favor",
                ChatPreguntaService.omitirDatosPersonales("Escríbanme a juan.perez+villas@correo.com.co por favor"));
        assertEquals("[dato omitido] y [dato omitido]",
                ChatPreguntaService.omitirDatosPersonales("a@b.co y MARIA_L@Dominio.Org"));
    }

    @Test
    @DisplayName("Los teléfonos (celular, con espacios, guiones, paréntesis o +57) se reemplazan; los números cortos no")
    void omiteTelefonos() {
        assertEquals("Mi cel es [dato omitido]", ChatPreguntaService.omitirDatosPersonales("Mi cel es 3168653715"));
        assertEquals("Llámenme al [dato omitido] hoy", ChatPreguntaService.omitirDatosPersonales("Llámenme al +57 316 865 3715 hoy"));
        assertEquals("tel [dato omitido]", ChatPreguntaService.omitirDatosPersonales("tel (606) 735-1234"));
        assertEquals("tel [dato omitido]", ChatPreguntaService.omitirDatosPersonales("tel 316-865-3715"));
        // Cifras normales de una pregunta sobre el proyecto no se tocan
        assertEquals("¿Hay lotes de 1500 m2 en 33 hectáreas, etapa 2?", ChatPreguntaService.omitirDatosPersonales("¿Hay lotes de 1500 m2 en 33 hectáreas, etapa 2?"));
        assertEquals("en 2026 o 2027", ChatPreguntaService.omitirDatosPersonales("en 2026 o 2027"));
    }

    @Test
    @DisplayName("Depurar también quita caracteres de control y respeta el tope de 500 caracteres")
    void depurar() {
        assertEquals("hola mundo", ChatPreguntaService.depurar("hola\n\n  mundo\t"));
        assertEquals(500, ChatPreguntaService.depurar("a".repeat(900)).length());
        assertEquals("", ChatPreguntaService.depurar(null));
    }

    // ------------------------------------------------------------------ guardado

    private final ChatPreguntaRepository repositorio = mock(ChatPreguntaRepository.class);
    private final ChatPreguntaService servicio = new ChatPreguntaService(repositorio);

    @Test
    @DisplayName("Guarda la pregunta ya depurada con su idioma y su tema (y nada más)")
    void guarda() {
        assertTrue(servicio.guardar("¿Cuánto cuesta? Escríbanme a ana@correo.com o al 3168653715", "es"));

        ArgumentCaptor<ChatPregunta> captor = ArgumentCaptor.forClass(ChatPregunta.class);
        verify(repositorio).save(captor.capture());
        ChatPregunta p = captor.getValue();
        assertEquals("¿Cuánto cuesta? Escríbanme a [dato omitido] o al [dato omitido]", p.getPregunta());
        assertEquals("es", p.getIdioma());
        assertEquals(TemaChat.PRECIOS, p.getTema());
        assertFalse(p.getPregunta().contains("@"));
    }

    @Test
    @DisplayName("Un idioma desconocido se guarda vacío; una pregunta en blanco no se guarda")
    void idiomaYBlancos() {
        servicio.guardar("hello", "fr");
        ArgumentCaptor<ChatPregunta> captor = ArgumentCaptor.forClass(ChatPregunta.class);
        verify(repositorio).save(captor.capture());
        assertNull(captor.getValue().getIdioma());

        assertFalse(servicio.guardar("   ", "es"));
        assertFalse(servicio.guardar(null, "es"));
    }

    @Test
    @DisplayName("Si la base falla, guardar no lanza excepciones (el chat sigue funcionando)")
    void fallaDeLaBase() {
        when(repositorio.save(any())).thenThrow(new RuntimeException("BD caída"));

        assertFalse(servicio.guardar("hola", "es"));
    }

    @Test
    @DisplayName("Privacidad: la tabla guarda solo pregunta, idioma, tema y fecha (sin respuesta, IP ni identificadores)")
    void soloLoNecesario() {
        List<String> campos = Arrays.stream(ChatPregunta.class.getDeclaredFields()).map(Field::getName).toList();
        assertEquals(List.of("id", "pregunta", "idioma", "tema", "creadoEn"), campos);
        Column columna = Arrays.stream(ChatPregunta.class.getDeclaredFields()).filter(f -> f.getName().equals("pregunta"))
                .findFirst().orElseThrow().getAnnotation(Column.class);
        assertEquals(500, columna.length());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Retención: delega el borrado al repositorio con la fecha límite")
    void retencion() {
        var limite = java.time.LocalDateTime.of(2025, 11, 3, 3, 0);
        when(repositorio.eliminarAnterioresA(limite)).thenReturn(7);

        assertEquals(7, servicio.eliminarAnterioresA(limite));
    }
}
