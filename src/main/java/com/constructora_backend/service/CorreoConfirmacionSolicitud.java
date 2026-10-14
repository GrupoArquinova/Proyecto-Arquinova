package com.constructora_backend.service;

import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Texto y HTML del correo que recibe quien deja una solicitud en el sitio: confirma lo que preguntó
 * (proyecto, villa o lote y su mensaje) y deja los medios para escribir a la empresa. Sale en español o en inglés.
 * Todo lo que escribe la persona se escapa antes de ir al HTML.
 */
final class CorreoConfirmacionSolicitud {

    /** Datos que se muestran en el correo; proyecto, interés y mensaje pueden venir vacíos. */
    record Datos(String idioma, String nombre, String proyecto, String interes, String mensaje,
                 String whatsapp, String correoEmpresa, String enlacePolitica) {

        boolean ingles() {
            return "en".equals(idioma);
        }
    }

    private static final int MAX_MENSAJE = 400;

    private CorreoConfirmacionSolicitud() {
    }

    static String asunto(Datos d) {
        String sobre = vacio(d.interes()) ? "" : (d.ingles() ? " about " : " sobre ") + d.interes().trim();
        return (d.ingles() ? "We received your request" : "Recibimos tu solicitud") + sobre + " - Grupo Arquinova";
    }

    /** Enlace que abre el chat de WhatsApp de la empresa con el mensaje ya escrito. */
    static String enlaceWhatsapp(Datos d) {
        String numero = d.whatsapp() == null ? "" : d.whatsapp().replaceAll("[^0-9]", "");
        if (numero.isEmpty()) return "";
        String tema = !vacio(d.interes()) ? d.interes().trim() : (!vacio(d.proyecto()) ? d.proyecto().trim() : "");
        String texto;
        if (d.ingles()) {
            texto = tema.isEmpty() ? "Hello, I left a request on your website." : "Hello, I left a request on your website about " + tema + ".";
        } else {
            texto = tema.isEmpty() ? "Hola, dejé una solicitud en su sitio web." : "Hola, dejé una solicitud en su sitio web sobre " + tema + ".";
        }
        return "https://wa.me/" + numero + "?text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String texto(Datos d) {
        boolean en = d.ingles();
        StringBuilder t = new StringBuilder();
        t.append(saludo(d)).append("\n\n");
        t.append(en ? "Thank you for writing to us. We received your request and we will get in touch with you soon.\n\n"
                    : "Gracias por escribirnos. Recibimos tu solicitud y pronto nos comunicaremos contigo.\n\n");

        StringBuilder resumen = new StringBuilder();
        if (!vacio(d.proyecto())) resumen.append("- ").append(en ? "Project: " : "Proyecto: ").append(d.proyecto().trim()).append("\n");
        if (!vacio(d.interes())) resumen.append("- ").append(en ? "You asked about: " : "Preguntaste por: ").append(d.interes().trim()).append("\n");
        if (!vacio(d.mensaje())) resumen.append("- ").append(en ? "Your message: " : "Tu mensaje: ").append(recortar(d.mensaje())).append("\n");
        if (resumen.length() > 0) {
            t.append(en ? "Summary of your request:\n" : "Resumen de tu solicitud:\n").append(resumen).append("\n");
        }

        t.append(en ? "If you would like a faster answer, you can write to us:\n" : "Si prefieres una respuesta más rápida, puedes escribirnos:\n");
        if (!vacio(d.whatsapp())) t.append("- WhatsApp: ").append(d.whatsapp().trim());
        String enlace = enlaceWhatsapp(d);
        if (!enlace.isEmpty()) t.append(" (").append(enlace).append(")");
        if (!vacio(d.whatsapp())) t.append("\n");
        if (!vacio(d.correoEmpresa())) t.append("- ").append(en ? "Email: " : "Correo: ").append(d.correoEmpresa().trim()).append("\n");
        t.append("\n");

        if (!vacio(d.enlacePolitica())) {
            t.append(en ? "This is how we handle your personal data: " : "Así tratamos tus datos personales: ").append(d.enlacePolitica()).append("\n\n");
        }
        t.append(en ? "If you did not leave this email address on our website, you can ignore this message.\n\n"
                    : "Si no fuiste tú quien dejó este correo en nuestro sitio web, puedes ignorar este mensaje.\n\n");
        t.append("Grupo Arquinova S.A.S.\nNIT 901.397.504-2");
        return t.toString();
    }

    static String html(Datos d) {
        boolean en = d.ingles();
        StringBuilder h = new StringBuilder();
        h.append("<div style=\"font-family:Arial,Helvetica,sans-serif;background:#f4f6f6;padding:24px;\">")
         .append("<div style=\"max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px;color:#1e293b;\">")
         .append("<p style=\"margin:0 0 4px;font-size:13px;letter-spacing:1px;text-transform:uppercase;color:#2c5953;font-weight:700;\">Grupo Arquinova</p>")
         .append("<h1 style=\"margin:0 0 20px;font-size:22px;font-weight:600;\">")
         .append(en ? "We received your request" : "Recibimos tu solicitud").append("</h1>")
         .append("<p style=\"margin:0 0 16px;font-size:15px;line-height:1.6;\">").append(esc(saludo(d))).append("</p>")
         .append("<p style=\"margin:0 0 24px;font-size:15px;line-height:1.6;\">")
         .append(en ? "Thank you for writing to us. We received your request and we will get in touch with you soon."
                    : "Gracias por escribirnos. Recibimos tu solicitud y pronto nos comunicaremos contigo.")
         .append("</p>");

        StringBuilder filas = new StringBuilder();
        if (!vacio(d.proyecto())) filas.append(fila(en ? "Project" : "Proyecto", d.proyecto()));
        if (!vacio(d.interes())) filas.append(fila(en ? "You asked about" : "Preguntaste por", d.interes()));
        if (!vacio(d.mensaje())) filas.append(fila(en ? "Your message" : "Tu mensaje", recortar(d.mensaje())));
        if (filas.length() > 0) {
            h.append("<table style=\"width:100%;border-collapse:collapse;background:#f4f6f6;border-radius:8px;margin:0 0 24px;\">").append(filas).append("</table>");
        }

        String enlace = enlaceWhatsapp(d);
        if (!enlace.isEmpty()) {
            h.append("<p style=\"margin:0 0 12px;font-size:14px;line-height:1.5;color:#475569;\">")
             .append(en ? "If you would like a faster answer, write to us on WhatsApp:" : "Si prefieres una respuesta más rápida, escríbenos por WhatsApp:")
             .append("</p>")
             .append("<p style=\"margin:0 0 24px;text-align:center;\"><a href=\"").append(esc(enlace))
             .append("\" style=\"display:inline-block;background:#2c5953;color:#ffffff;text-decoration:none;font-weight:600;font-size:15px;padding:12px 28px;border-radius:999px;\">")
             .append(en ? "Write on WhatsApp" : "Escribir por WhatsApp").append("</a></p>");
        }
        if (!vacio(d.whatsapp()) || !vacio(d.correoEmpresa())) {
            h.append("<p style=\"margin:0 0 24px;font-size:13px;line-height:1.6;color:#475569;\">");
            if (!vacio(d.whatsapp())) h.append("WhatsApp: <strong>").append(esc(d.whatsapp().trim())).append("</strong><br>");
            if (!vacio(d.correoEmpresa())) h.append(en ? "Email: " : "Correo: ").append("<strong>").append(esc(d.correoEmpresa().trim())).append("</strong>");
            h.append("</p>");
        }
        if (!vacio(d.enlacePolitica())) {
            h.append("<p style=\"margin:0 0 8px;font-size:12px;line-height:1.5;color:#64748b;\">")
             .append(en ? "This is how we handle your personal data: " : "Así tratamos tus datos personales: ")
             .append("<a href=\"").append(esc(d.enlacePolitica())).append("\" style=\"color:#2c5953;\">").append(esc(d.enlacePolitica())).append("</a></p>");
        }
        h.append("<p style=\"margin:0 0 24px;font-size:12px;line-height:1.5;color:#64748b;\">")
         .append(en ? "If you did not leave this email address on our website, you can ignore this message."
                    : "Si no fuiste tú quien dejó este correo en nuestro sitio web, puedes ignorar este mensaje.")
         .append("</p>")
         .append("<hr style=\"border:none;border-top:1px solid #e2e8f0;margin:0 0 16px;\">")
         .append("<p style=\"margin:0;font-size:12px;color:#94a3b8;\">Grupo Arquinova S.A.S. · NIT 901.397.504-2</p>")
         .append("</div></div>");
        return h.toString();
    }

    private static String fila(String etiqueta, String valor) {
        return "<tr><td style=\"padding:10px 14px;font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.5px;vertical-align:top;width:34%;\">" + esc(etiqueta)
                + "</td><td style=\"padding:10px 14px;font-size:14px;color:#1e293b;\">" + esc(valor.trim()) + "</td></tr>";
    }

    private static String saludo(Datos d) {
        boolean tieneNombre = !vacio(d.nombre());
        if (d.ingles()) return tieneNombre ? "Hello " + d.nombre().trim() + "," : "Hello,";
        return tieneNombre ? "Hola " + d.nombre().trim() + "," : "Hola,";
    }

    private static String recortar(String mensaje) {
        String limpio = mensaje.trim().replaceAll("\\s+", " ");
        return limpio.length() > MAX_MENSAJE ? limpio.substring(0, MAX_MENSAJE).trim() + "…" : limpio;
    }

    private static String esc(String texto) {
        return HtmlUtils.htmlEscape(texto, "UTF-8");
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
