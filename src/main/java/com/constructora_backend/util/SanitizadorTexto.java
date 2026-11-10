package com.constructora_backend.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Limpia el texto que escribe una persona antes de guardarlo: quita etiquetas HTML (con el contenido de los bloques
 * &lt;script&gt;, &lt;style&gt;, &lt;iframe&gt;…), caracteres de control invisibles y los que invierten el sentido del texto,
 * y unifica la forma de los acentos (NFC). Conserva los saltos de línea y las tabulaciones de los mensajes largos.
 * <p>
 * Es una defensa en profundidad: el sitio ya escapa todo al mostrarlo y los correos se escapan antes de enviarse, pero así
 * nada con marcado llega a la base de datos, a los reportes ni a los correos del equipo.
 * <p>
 * No toca contraseñas ni tokens ({@link #esCampoSensible}): cambiarlos impediría iniciar sesión o validar un enlace.
 */
public final class SanitizadorTexto {

    /** Bloques cuyo contenido tampoco debe quedar (el texto dentro de un script no es texto de una persona). */
    private static final Pattern BLOQUE = Pattern.compile(
            "<\\s*(script|style|iframe|object|embed)\\b.*?<\\s*/\\s*\\1\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /** Una etiqueta: empieza con letra, "/", "!" o "?" para no tocar un "<3" ni un "a < b". */
    private static final Pattern ETIQUETA = Pattern.compile("</?\\s*[a-zA-Z!?][^<>]*>", Pattern.DOTALL);
    /** Un "<" que abre algo y nunca se cierra. */
    private static final Pattern ABIERTA = Pattern.compile("<(?=\\s*[a-zA-Z/!?])");
    /** Controles salvo salto de línea, retorno y tabulación; más los que invierten el sentido del texto y el byte nulo. */
    private static final Pattern INVISIBLES = Pattern.compile("[\\p{Cntrl}&&[^\\n\\r\\t]]|[\\u202A-\\u202E\\u2066-\\u2069\\u200E\\u200F]");
    private static final Pattern NOMBRE_SENSIBLE = Pattern.compile("pass|contras|token|clave|secret|authorization", Pattern.CASE_INSENSITIVE);

    /** Para no entrar en un ciclo con entradas hechas a propósito: se repite hasta que no cambie, con tope. */
    private static final int MAX_PASADAS = 6;

    private SanitizadorTexto() {
    }

    /** El mismo texto sin marcado ni caracteres invisibles; {@code null} si era {@code null}. */
    public static String limpiar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        String actual = texto;
        for (int i = 0; i < MAX_PASADAS; i++) {
            String siguiente = ABIERTA.matcher(ETIQUETA.matcher(BLOQUE.matcher(actual).replaceAll("")).replaceAll("")).replaceAll("");
            if (siguiente.equals(actual)) break;
            actual = siguiente;
        }
        actual = INVISIBLES.matcher(actual).replaceAll("");
        return Normalizer.isNormalized(actual, Normalizer.Form.NFC) ? actual : Normalizer.normalize(actual, Normalizer.Form.NFC);
    }

    /** true para los campos que no se deben tocar (contraseñas, tokens, claves): se reconocen por el nombre. */
    public static boolean esCampoSensible(String nombre) {
        return nombre != null && NOMBRE_SENSIBLE.matcher(nombre.toLowerCase(Locale.ROOT)).find();
    }

    /** true si Excel (o un CSV hecho a partir de la hoja) podría leer el texto como una fórmula. */
    public static boolean pareceFormula(String texto) {
        if (texto == null || texto.isEmpty()) return false;
        char primero = texto.charAt(0);
        return primero == '=' || primero == '+' || primero == '-' || primero == '@' || primero == '\t' || primero == '\r';
    }
}
