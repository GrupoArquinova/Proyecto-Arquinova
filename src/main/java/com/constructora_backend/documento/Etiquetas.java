package com.constructora_backend.documento;

import java.util.Map;

/** Textos legibles para los nombres guardados en la base (EN_CONSTRUCCION, EN_GESTION...). */
public final class Etiquetas {

    private static final Map<String, String> CONOCIDAS = Map.ofEntries(
            Map.entry("EN_DISENO", "En diseño"),
            Map.entry("EN_TRAMITE", "En trámite"),
            Map.entry("EN_CONSTRUCCION", "En construcción"),
            Map.entry("FINALIZADO", "Finalizado"),
            Map.entry("EN_GESTION", "En gestión"),
            Map.entry("OFERTA_COMERCIAL", "Oferta comercial"),
            Map.entry("PORTAFOLIO", "Portafolio"));

    private Etiquetas() {
    }

    public static String legible(String valor) {
        if (valor == null || valor.isBlank()) return "";
        String conocida = CONOCIDAS.get(valor.trim().toUpperCase());
        if (conocida != null) return conocida;
        String limpio = valor.trim().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
    }
}
