package com.constructora_backend.service;

import com.constructora_backend.entity.ChatPregunta;
import com.constructora_backend.repository.ChatPreguntaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guarda las preguntas que las personas le hacen al asistente, para saber qué interesa (ubicación, precios, villas...)
 * y mejorar el sitio y el asistente.
 * <p>
 * IMPORTANTE (datos personales): se guarda SOLO la pregunta. No se guarda la respuesta, el historial de la conversación,
 * la IP ni ningún identificador. La gente a veces escribe sus datos en el chat ("escríbanme a juan@correo.com, mi cel es
 * 316 865 3715"), así que antes de guardar se reemplaza todo lo que parezca un correo electrónico o un teléfono por
 * {@value #DATO_OMITIDO}. Esta regla no debe relajarse: la pregunta se muestra a los administradores en las métricas.
 * Se conserva 12 meses (RetencionDatosJob).
 * <p>
 * Guardar es secundario: si falla, el chat sigue funcionando (se deja un aviso en el log, sin el texto de la pregunta).
 */
@Service
@Slf4j
public class ChatPreguntaService {

    public static final String DATO_OMITIDO = "[dato omitido]";
    /** Largo de la columna (y de la pregunta que acepta el chat). */
    public static final int MAX_PREGUNTA = 500;

    private static final Pattern CORREO = Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
    /** Secuencia que empieza y termina en dígito, con separadores comunes de teléfono (espacios, puntos, guiones, paréntesis, +). */
    private static final Pattern TELEFONO = Pattern.compile("(?<![\\w])(?:\\+|\\()?\\d[\\d\\s().\\-]{5,}\\d(?![\\w])");
    /** Con 7 dígitos o más se considera un teléfono (o un documento de identidad): se omite. */
    private static final int MIN_DIGITOS_TELEFONO = 7;

    private final ChatPreguntaRepository repositorio;

    @Autowired
    public ChatPreguntaService(ChatPreguntaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Guarda la pregunta ya depurada. Nunca lanza excepciones.
     *
     * @return true si se guardó
     */
    public boolean guardar(String pregunta, String idioma) {
        try {
            String depurada = depurar(pregunta);
            if (depurada.isBlank()) {
                return false;
            }
            ChatPregunta registro = new ChatPregunta();
            registro.setPregunta(depurada);
            registro.setIdioma("es".equals(idioma) || "en".equals(idioma) ? idioma : null);
            registro.setTema(ChatTemaClasificador.clasificar(depurada));
            repositorio.save(registro);
            return true;
        } catch (Exception ex) {
            log.warn("Chat: no se pudo guardar la pregunta: {}", ex.getClass().getSimpleName());
            return false;
        }
    }

    /** Borra las preguntas anteriores a la fecha (retención); devuelve cuántas eran. */
    @Transactional
    public int eliminarAnterioresA(LocalDateTime limite) {
        return repositorio.eliminarAnterioresA(limite);
    }

    /** Texto listo para guardar: sin caracteres de control, con correos y teléfonos omitidos y con tope de largo. */
    static String depurar(String pregunta) {
        if (pregunta == null) {
            return "";
        }
        String texto = pregunta.replaceAll("\\p{Cntrl}", " ").replaceAll(" {2,}", " ").trim();
        texto = omitirDatosPersonales(texto);
        return texto.length() > MAX_PREGUNTA ? texto.substring(0, MAX_PREGUNTA) : texto;
    }

    /** Reemplaza correos electrónicos y números que parecen teléfonos por {@value #DATO_OMITIDO}. */
    static String omitirDatosPersonales(String texto) {
        String sinCorreos = CORREO.matcher(texto).replaceAll(Matcher.quoteReplacement(DATO_OMITIDO));
        Matcher m = TELEFONO.matcher(sinCorreos);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            long digitos = m.group().chars().filter(Character::isDigit).count();
            m.appendReplacement(sb, Matcher.quoteReplacement(digitos >= MIN_DIGITOS_TELEFONO ? DATO_OMITIDO : m.group()));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
