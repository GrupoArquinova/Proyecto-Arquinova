package com.constructora_backend.service;

import com.constructora_backend.enums.TemaChat;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Asigna un tema a la pregunta del asistente con reglas simples de palabras clave (español e inglés), sin IA.
 * Es tolerante a tildes y mayúsculas: "¿Dónde ESTÁ?" y "donde esta" dan lo mismo. Si varias reglas aplican gana la de mayor
 * prioridad (el orden de {@link #REGLAS}): una pregunta por el precio de un lote es PRECIOS, no LOTES.
 */
public final class ChatTemaClasificador {

    /** En orden de prioridad. Los patrones trabajan sobre texto ya normalizado (minúsculas, sin tildes, solo letras y números). */
    private static final Map<TemaChat, Pattern> REGLAS = new LinkedHashMap<>();

    static {
        regla(TemaChat.CONTACTO_VISITA, "visita\\w*|visit(?:s|ar|ing|or)?|agendar|agenda|cita|citas|appointment|schedule|whatsapp|telefono|"
                + "llamar|llamada|celular|contactar\\w*|contacto|contact|correo|email|asesor\\w*|advisor|hablar con|speak to|reunion|call|phone");
        regla(TemaChat.PRECIOS, "precio\\w*|costo\\w*|cuesta\\w*|cuanto (?:vale|cuesta|sale|es)|vale|valor|financ\\w*|credito|cuota\\w*|"
                + "separar|separacion|descuento\\w*|price\\w*|cost\\w*|how much|afford\\w*|payment\\w*|mortgage|installment\\w*|"
                + "down payment|deposit|presupuesto|budget|usd|cop|millones");
        regla(TemaChat.TOUR_PLANOS, "tour|recorrido\\w*|360|virtual\\w*|plano\\w*|planta\\w*|blueprint\\w*|floor ?plan\\w*|render\\w*|"
                + "video\\w*|fotos?|galeria|imagenes|photos?|gallery|panoram\\w*");
        regla(TemaChat.UBICACION, "donde|ubicad\\w*|ubicacion\\w*|direccion|mapa|llegar|como llego|queda\\w*|cerca|distancia|kilometr\\w*|"
                + "minutos de|vereda|sector|quindio|armenia|calarca|circasia|montenegro|salento|filandia|eje cafetero|"
                + "where|location|located|address|map|directions?|how far|near(?:by)?|city|get there|distance");
        regla(TemaChat.AMENIDADES, "amenidad\\w*|amenit\\w*|piscina\\w*|pool\\w*|jacuzzi\\w*|gimnasio\\w*|gym|club ?house|zonas? comun\\w*|"
                + "common areas?|parqueadero\\w*|parking|cancha\\w*|bbq|asador\\w*|spa|sauna|turco|senderos?|trails?|juegos|playground|"
                + "parque\\w*|seguridad|security|porteria|gated|areas? verdes?|green areas?|lago|lake|mascotas|pets?|salon social");
        regla(TemaChat.VILLAS, "villa\\w*|casa\\w*|vivienda\\w*|tipolog\\w*|habitacion\\w*|alcoba\\w*|cuarto\\w*|bano\\w*|house\\w*|home\\w*|"
                + "bedroom\\w*|bath\\w*|townhouse|apartamento\\w*|apartment\\w*|construid\\w*|built area");
        regla(TemaChat.LOTES, "lote\\w*|lot|lots|terreno\\w*|parcela\\w*|land|plot\\w*|area|m2|metros?|hectarea\\w*|acre\\w*|disponib\\w*|"
                + "availab\\w*|reservad\\w*|vendid\\w*|etapa\\w*|stage\\w*");
        regla(TemaChat.SERVICIOS, "servicio\\w*|service\\w*|diseno\\w*|disenar|design\\w*|arquitect\\w*|licencia\\w*|permit\\w*|licens\\w*|"
                + "construccion|construir|construct\\w*|build\\w*|interventoria|estructuracion|tramite\\w*|consultoria|consult\\w*|"
                + "asesoria|estudio\\w*|ingenier\\w*|engineer\\w*|remodel\\w*|obra\\w*");
        regla(TemaChat.PROYECTOS, "proyecto\\w*|project\\w*|portafolio|portfolio|encanto|arquinova|grupo|empresa|company|trayectoria|"
                + "experiencia|about|quienes son|who are|nosotros|historia|desarrollo\\w*|development\\w*");
    }

    private ChatTemaClasificador() {
    }

    private static void regla(TemaChat tema, String alternativas) {
        // \b a ambos lados: "lot" no coincide dentro de "pilot"; los sufijos (\w*) están escritos donde se aceptan
        REGLAS.put(tema, Pattern.compile("\\b(?:" + alternativas + ")\\b"));
    }

    /** Tema de la pregunta; OTRO si no coincide ninguna regla o la pregunta viene vacía. */
    public static TemaChat clasificar(String pregunta) {
        String texto = normalizar(pregunta);
        if (texto.isEmpty()) {
            return TemaChat.OTRO;
        }
        for (Map.Entry<TemaChat, Pattern> regla : REGLAS.entrySet()) {
            if (regla.getValue().matcher(texto).find()) {
                return regla.getKey();
            }
        }
        return TemaChat.OTRO;
    }

    /** Minúsculas, sin tildes ni signos: solo letras, números y espacios simples. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("[^a-z0-9]+", " ").trim();
    }
}
