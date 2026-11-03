package com.constructora_backend.enums;

/**
 * Eventos de la analítica propia del sitio (sin cookies). Cada uno lo registra una sola parte para no contarlo doble:
 * <ul>
 *   <li>El NAVEGADOR (POST /api/analitica/evento): VISTA_PAGINA, VISTA_PROYECTO, CLICK_WHATSAPP y COMPARTIR.</li>
 *   <li>El SERVIDOR, solo: FORMULARIO_ENVIADO (al guardar una solicitud de contacto), INICIO_CHAT (primera pregunta de una
 *       conversación del asistente) y DESCARGA_BROCHURE (al entregar el PDF). El endpoint público ignora estos tres tipos.</li>
 * </ul>
 */
public enum TipoEventoAnalitica {
    VISTA_PAGINA(false),
    VISTA_PROYECTO(false),
    CLICK_WHATSAPP(false),
    FORMULARIO_ENVIADO(true),
    DESCARGA_BROCHURE(true),
    COMPARTIR(false),
    INICIO_CHAT(true);

    private final boolean soloServidor;

    TipoEventoAnalitica(boolean soloServidor) {
        this.soloServidor = soloServidor;
    }

    /** true si solo lo registra el servidor; el navegador no puede enviarlo. */
    public boolean esSoloServidor() {
        return soloServidor;
    }
}
