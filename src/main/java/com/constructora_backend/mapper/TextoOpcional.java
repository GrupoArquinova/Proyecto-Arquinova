package com.constructora_backend.mapper;

/** Normaliza los textos opcionales que llegan del panel: un texto vacío o solo con espacios se guarda como "sin texto". */
final class TextoOpcional {

    private TextoOpcional() {
    }

    static String limpiar(String texto) {
        if (texto == null) return null;
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
