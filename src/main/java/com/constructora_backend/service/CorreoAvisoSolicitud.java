package com.constructora_backend.service;

import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Aviso interno que recibe el equipo cuando alguien deja una solicitud en el sitio: los datos de la persona, lo que
 * preguntó y botones para responder (correo, WhatsApp y llamada). Siempre en español. Todo lo que escribe la persona
 * se escapa antes de ir al HTML.
 */
final class CorreoAvisoSolicitud {

    /** Datos de la solicitud; todos pueden venir vacíos salvo el nombre. */
    record Datos(String nombre, String telefono, String correo, String proyecto, String interes, String servicio,
                 String idioma, String mensaje) {

        boolean ingles() {
            return "en".equals(idioma);
        }
    }

    private CorreoAvisoSolicitud() {
    }

    /** Asunto del aviso. El nombre lo escribe un visitante: se le quitan saltos de línea y caracteres de control (inyección de cabeceras). */
    static String asunto(Datos d) {
        String sobre = vacio(d.interes()) ? "" : " (" + sinControl(d.interes()) + ")";
        return "Nueva solicitud de contacto de: " + sinControl(d.nombre()) + sobre;
    }

    /** Cambia por un espacio cada salto de línea o carácter de control, junta los espacios y recorta. */
    static String sinControl(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[\\r\\n\\p{Cntrl}]", " ").replaceAll(" {2,}", " ").trim();
    }

    /** Teléfono listo para el enlace de llamada: solo dígitos y el + inicial. */
    static String telefonoParaLlamar(String telefono) {
        if (vacio(telefono)) return "";
        String limpio = telefono.trim();
        String digitos = limpio.replaceAll("[^0-9]", "");
        if (digitos.length() < 7) return "";
        return (limpio.startsWith("+") ? "+" : "") + digitos;
    }

    /**
     * Enlace de WhatsApp para escribirle a la persona. Un celular colombiano de 10 dígitos que empieza por 3 se completa
     * con el 57; cualquier otro número necesita su indicativo (empezar por + o tener más de 10 dígitos).
     */
    static String enlaceWhatsappCliente(Datos d) {
        if (vacio(d.telefono())) return "";
        String digitos = d.telefono().replaceAll("[^0-9]", "");
        String numero;
        if (digitos.length() == 10 && digitos.startsWith("3")) numero = "57" + digitos;
        else if (digitos.length() > 10 || (d.telefono().trim().startsWith("+") && digitos.length() >= 8)) numero = digitos;
        else return "";

        String tema = !vacio(d.interes()) ? d.interes().trim() : (!vacio(d.proyecto()) ? d.proyecto().trim() : "");
        String nombre = d.nombre().trim();
        String texto;
        if (d.ingles()) {
            texto = "Hello " + nombre + ", this is Grupo Arquinova. We received your request" + (tema.isEmpty() ? "" : " about " + tema) + ".";
        } else {
            texto = "Hola " + nombre + ", te escribimos de Grupo Arquinova. Recibimos tu solicitud" + (tema.isEmpty() ? "" : " sobre " + tema) + ".";
        }
        return "https://wa.me/" + numero + "?text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String texto(Datos d) {
        StringBuilder t = new StringBuilder("Has recibido una nueva solicitud de información desde la web:\n\n");
        t.append("Nombre: ").append(d.nombre().trim()).append("\n");
        t.append("Teléfono: ").append(vacio(d.telefono()) ? "No indicó" : d.telefono().trim()).append("\n");
        t.append("Correo: ").append(vacio(d.correo()) ? "No indicó" : d.correo().trim()).append("\n");
        t.append("Proyecto de interés: ").append(vacio(d.proyecto()) ? "No especificado" : d.proyecto().trim()).append("\n");
        if (!vacio(d.interes())) t.append("Preguntó por: ").append(d.interes().trim()).append("\n");
        t.append("Servicio de interés: ").append(vacio(d.servicio()) ? "No especificado" : d.servicio().trim()).append("\n");
        t.append("Idioma del sitio: ").append(d.ingles() ? "Inglés (conviene responderle en inglés)" : "Español").append("\n");
        t.append("Mensaje del cliente:\n").append(vacio(d.mensaje()) ? "(sin mensaje)" : d.mensaje().trim()).append("\n\n");
        String wa = enlaceWhatsappCliente(d);
        if (!wa.isEmpty()) t.append("Escribirle por WhatsApp: ").append(wa).append("\n\n");
        t.append("---\nEste correo fue generado automáticamente por el sistema de Grupo Arquinova.");
        return t.toString();
    }

    static String html(Datos d) {
        StringBuilder h = new StringBuilder();
        h.append("<div style=\"font-family:Arial,Helvetica,sans-serif;background:#f4f6f6;padding:24px;\">")
         .append("<div style=\"max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px;color:#1e293b;\">")
         .append("<p style=\"margin:0 0 4px;font-size:13px;letter-spacing:1px;text-transform:uppercase;color:#2c5953;font-weight:700;\">Grupo Arquinova · Sitio web</p>")
         .append("<h1 style=\"margin:0 0 8px;font-size:22px;font-weight:600;\">Nueva solicitud de contacto</h1>")
         .append("<p style=\"margin:0 0 20px;font-size:14px;line-height:1.5;color:#475569;\">")
         .append("<strong>").append(esc(d.nombre().trim())).append("</strong> dejó sus datos en el sitio");
        if (d.ingles()) {
            h.append(" <span style=\"background:#e0e7ff;color:#3730a3;border-radius:999px;padding:2px 10px;font-size:12px;font-weight:700;\">Sitio en inglés</span>");
        }
        h.append(".</p>");

        StringBuilder filas = new StringBuilder();
        if (!vacio(d.proyecto())) filas.append(fila("Proyecto", esc(d.proyecto().trim())));
        if (!vacio(d.interes())) filas.append(fila("Preguntó por", "<strong>" + esc(d.interes().trim()) + "</strong>"));
        if (!vacio(d.servicio())) filas.append(fila("Servicio", esc(d.servicio().trim())));
        String llamada = telefonoParaLlamar(d.telefono());
        if (!vacio(d.telefono())) {
            String tel = esc(d.telefono().trim());
            filas.append(fila("Teléfono", llamada.isEmpty() ? tel : "<a href=\"tel:" + esc(llamada) + "\" style=\"color:#2c5953;\">" + tel + "</a>"));
        }
        if (!vacio(d.correo())) {
            String correo = esc(d.correo().trim());
            filas.append(fila("Correo", "<a href=\"mailto:" + correo + "\" style=\"color:#2c5953;\">" + correo + "</a>"));
        }
        h.append("<table style=\"width:100%;border-collapse:collapse;background:#f4f6f6;border-radius:8px;margin:0 0 20px;\">").append(filas).append("</table>");

        h.append("<p style=\"margin:0 0 8px;font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.5px;\">Mensaje</p>")
         .append("<div style=\"margin:0 0 24px;padding:14px 16px;border-left:4px solid #2c5953;background:#f4f6f6;border-radius:4px;font-size:14px;line-height:1.6;white-space:pre-wrap;\">")
         .append(vacio(d.mensaje()) ? "<em style=\"color:#94a3b8;\">(sin mensaje)</em>" : esc(d.mensaje().trim()))
         .append("</div>");

        StringBuilder botones = new StringBuilder();
        String wa = enlaceWhatsappCliente(d);
        if (!wa.isEmpty()) botones.append(boton(wa, "Escribirle por WhatsApp", true));
        if (!vacio(d.correo())) botones.append(boton("mailto:" + d.correo().trim(), "Responder por correo", wa.isEmpty()));
        if (!llamada.isEmpty()) botones.append(boton("tel:" + llamada, "Llamar", false));
        if (botones.length() > 0) h.append("<p style=\"margin:0 0 24px;text-align:center;\">").append(botones).append("</p>");

        h.append("<hr style=\"border:none;border-top:1px solid #e2e8f0;margin:0 0 16px;\">")
         .append("<p style=\"margin:0;font-size:12px;color:#94a3b8;\">Aviso automático del sistema de Grupo Arquinova S.A.S. Cada solicitud también queda en el panel de administración, en Solicitudes.</p>")
         .append("</div></div>");
        return h.toString();
    }

    private static String boton(String enlace, String etiqueta, boolean principal) {
        String estilo = principal
                ? "background:#2c5953;color:#ffffff;border:1.5px solid #2c5953;"
                : "background:#ffffff;color:#2c5953;border:1.5px solid #2c5953;";
        return "<a href=\"" + esc(enlace) + "\" style=\"display:inline-block;margin:4px;" + estilo
                + "text-decoration:none;font-weight:600;font-size:14px;padding:10px 22px;border-radius:999px;\">" + esc(etiqueta) + "</a>";
    }

    private static String fila(String etiqueta, String valorHtml) {
        return "<tr><td style=\"padding:10px 14px;font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.5px;vertical-align:top;width:30%;\">" + etiqueta
                + "</td><td style=\"padding:10px 14px;font-size:14px;color:#1e293b;\">" + valorHtml + "</td></tr>";
    }

    private static String esc(String texto) {
        return HtmlUtils.htmlEscape(texto, "UTF-8");
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
