package com.constructora_backend.documento;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Documento PDF de Arquinova: franja de marca arriba, pie con fecha y "Página X de N", tablas que continúan en las hojas
 * siguientes con su encabezado repetido, párrafos que se ajustan al ancho y barras de proporción.
 * Usa las fuentes estándar del PDF, así que los caracteres que no tienen se reemplazan por "?" en vez de fallar.
 */
public final class PdfReporte implements AutoCloseable {

    public static final Color VERDE = new Color(0x2C, 0x59, 0x53);
    public static final Color VERDE_CLARO = new Color(0xE8, 0xF0, 0xEF);
    public static final Color GRIS = new Color(0x64, 0x74, 0x8B);
    public static final Color TEXTO = new Color(0x1E, 0x29, 0x3B);
    private static final Color ZEBRA = new Color(0xF8, 0xFA, 0xFB);
    private static final Color LINEA = new Color(0xE2, 0xE8, 0xF0);

    private static final float MARGEN = 40f;
    /** Cabecera de cada hoja: logo de Arquinova a la izquierda, nombre del documento a la derecha y una línea verde debajo. */
    private static final float ALTO_CABECERA = 58f;
    private static final float ALTO_LOGO = 38f;
    private static final String RECURSO_LOGO = "/documento/logo-arquinova.jpg";
    private static final float PIE = 40f;
    private static final DateTimeFormatter EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final CharsetEncoder WIN_ANSI = Charset.forName("windows-1252").newEncoder();

    private final PDDocument documento = new PDDocument();
    private final PDRectangle tamano;
    private final String tituloCorto;
    private final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private PDPage pagina;
    private PDPageContentStream flujo;
    private final PDImageXObject logo;
    private float y;
    private int paginas;

    public PdfReporte(boolean horizontal, String tituloCorto) throws IOException {
        this.tamano = horizontal ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()) : PDRectangle.A4;
        this.tituloCorto = limpiar(tituloCorto);
        this.logo = cargarLogo();
        nuevaPagina();
    }

    // ------------------------------------------------------------------ bloques

    /** Título grande al inicio del documento, con un subtítulo opcional. */
    public void portada(String titulo, String subtitulo) throws IOException {
        texto(titulo, MARGEN, y - 18, negrita, 20, VERDE);
        y -= 28;
        if (subtitulo != null && !subtitulo.isBlank()) {
            texto(subtitulo, MARGEN, y - 12, negrita, 13, TEXTO);
            y -= 22;
        }
        y -= 6;
    }

    /** Líneas pequeñas en gris (filtros usados, fecha de emisión). */
    public void notas(List<String> lineas) throws IOException {
        for (String linea : lineas) {
            for (String parte : envolver(linea, normal, 9, ancho())) {
                asegurar(13);
                texto(parte, MARGEN, y - 9, normal, 9, GRIS);
                y -= 13;
            }
        }
        y -= 4;
    }

    public void seccion(String titulo) throws IOException {
        asegurar(44);
        y -= 10;
        texto(titulo, MARGEN, y - 12, negrita, 12, VERDE);
        y -= 17;
        linea(MARGEN, y, tamano.getWidth() - MARGEN, y, VERDE, 1.2f);
        y -= 8;
    }

    public void clavesValor(Map<String, String> datos) throws IOException {
        float colClave = 120f;
        for (String clave : datos.keySet()) colClave = Math.max(colClave, ancho(clave, negrita, 10) + 16f);
        colClave = Math.min(colClave, ancho() * 0.55f);
        for (Map.Entry<String, String> e : datos.entrySet()) {
            if (e.getValue() == null || e.getValue().isBlank()) continue;
            List<String> partes = envolver(e.getValue(), normal, 10, ancho() - colClave);
            asegurar(15 * partes.size());
            texto(ajustar(limpiar(e.getKey()), negrita, 10, colClave - 8f), MARGEN, y - 10, negrita, 10, GRIS);
            for (String parte : partes) {
                texto(parte, MARGEN + colClave, y - 10, normal, 10, TEXTO);
                y -= 15;
            }
        }
        y -= 4;
    }

    public void parrafo(String texto, int tamanoLetra) throws IOException {
        for (String parte : envolver(texto, normal, tamanoLetra, ancho())) {
            asegurar(tamanoLetra + 5);
            texto(parte, MARGEN, y - tamanoLetra, normal, tamanoLetra, TEXTO);
            y -= tamanoLetra + 5;
        }
        y -= 4;
    }

    /** Una barra horizontal por estado: etiqueta, barra proporcional al total y cantidad con porcentaje. */
    public void barras(List<String> etiquetas, List<Long> cantidades, List<Color> colores) throws IOException {
        long total = cantidades.stream().mapToLong(Long::longValue).sum();
        float colEtiqueta = 110f;
        float colTexto = 120f;
        float largoMax = ancho() - colEtiqueta - colTexto;
        for (int i = 0; i < etiquetas.size(); i++) {
            asegurar(20);
            float proporcion = total == 0 ? 0f : (float) cantidades.get(i) / total;
            texto(etiquetas.get(i), MARGEN, y - 11, negrita, 10, TEXTO);
            relleno(MARGEN + colEtiqueta, y - 13, largoMax, 12, LINEA);
            if (proporcion > 0) relleno(MARGEN + colEtiqueta, y - 13, Math.max(2f, largoMax * proporcion), 12, colores.get(i));
            String detalle = cantidades.get(i) + (cantidades.get(i) == 1 ? " lote (" : " lotes (") + String.format(java.util.Locale.forLanguageTag("es-CO"), "%.1f", proporcion * 100) + " %)";
            texto(detalle, MARGEN + colEtiqueta + largoMax + 8, y - 11, normal, 10, TEXTO);
            y -= 20;
        }
        y -= 4;
    }

    /** Tabla con encabezado repetido en cada hoja, filas alternadas y, si hay columnas que suman, una fila de totales. */
    public <T> void tabla(List<Columna<T>> columnas, List<T> filas) throws IOException {
        float[] anchos = anchosColumnas(columnas, filas);
        final float altoFila = 16f;
        final float tamanoLetra = 8.5f;

        encabezadoTabla(columnas, anchos);
        if (filas.isEmpty()) {
            asegurar(altoFila);
            texto("Sin registros para los filtros elegidos.", MARGEN + 4, y - 11, normal, 9, GRIS);
            y -= altoFila;
            return;
        }

        int i = 0;
        for (T fila : filas) {
            if (y - altoFila < PIE + 8) {
                nuevaPagina();
                encabezadoTabla(columnas, anchos);
            }
            if (i++ % 2 == 1) relleno(MARGEN, y - altoFila, ancho(), altoFila, ZEBRA);
            float x = MARGEN;
            for (int c = 0; c < columnas.size(); c++) {
                celda(columnas.get(c).formatear(columnas.get(c).valor().apply(fila)), x, anchos[c], y, normal, tamanoLetra,
                        columnas.get(c).esNumerica(), TEXTO);
                x += anchos[c];
            }
            linea(MARGEN, y - altoFila, MARGEN + ancho(), y - altoFila, LINEA, 0.4f);
            y -= altoFila;
        }

        if (columnas.stream().anyMatch(Columna::sumar)) {
            if (y - altoFila < PIE + 8) {
                nuevaPagina();
                encabezadoTabla(columnas, anchos);
            }
            relleno(MARGEN, y - altoFila, ancho(), altoFila, VERDE_CLARO);
            float x = MARGEN;
            for (int c = 0; c < columnas.size(); c++) {
                Columna<T> col = columnas.get(c);
                String valor = c == 0 ? "TOTAL (" + filas.size() + ")" : "";
                if (col.sumar()) {
                    double suma = 0;
                    for (T fila : filas) {
                        if (col.valor().apply(fila) instanceof Number n) suma += n.doubleValue();
                    }
                    valor = col.formatear(col.tipo() == Columna.Tipo.ENTERO ? (Object) Math.round(suma) : (Object) suma);
                }
                celda(valor, x, anchos[c], y, negrita, tamanoLetra, col.esNumerica(), VERDE);
                x += anchos[c];
            }
            y -= altoFila;
        }
        y -= 6;
    }

    /** Cierra el documento: agrega el pie de cada hoja con "Página X de N" y devuelve los bytes del PDF. */
    public byte[] bytes() throws IOException {
        flujo.close();
        flujo = null;
        String emitido = "Arquinova · Grupo Empresarial  |  Generado el " + LocalDateTime.now().format(EMISION);
        int total = documento.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            try (PDPageContentStream pie = new PDPageContentStream(documento, documento.getPage(i), PDPageContentStream.AppendMode.APPEND, true, true)) {
                pie.setStrokingColor(LINEA);
                pie.setLineWidth(0.6f);
                pie.moveTo(MARGEN, 30);
                pie.lineTo(tamano.getWidth() - MARGEN, 30);
                pie.stroke();
                escribir(pie, emitido, MARGEN, 18, normal, 8, GRIS);
                String paginaTxt = "Página " + (i + 1) + " de " + total;
                escribir(pie, paginaTxt, tamano.getWidth() - MARGEN - ancho(paginaTxt, normal, 8), 18, normal, 8, GRIS);
            }
        }
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    @Override
    public void close() throws IOException {
        if (flujo != null) flujo.close();
        documento.close();
    }

    // ------------------------------------------------------------------ internos

    private float ancho() {
        return tamano.getWidth() - 2 * MARGEN;
    }

    private void nuevaPagina() throws IOException {
        if (flujo != null) flujo.close();
        pagina = new PDPage(tamano);
        documento.addPage(pagina);
        paginas++;
        flujo = new PDPageContentStream(documento, pagina);
        dibujarCabecera();
        y = tamano.getHeight() - ALTO_CABECERA - 14;
    }

    private void dibujarCabecera() throws IOException {
        float arriba = tamano.getHeight() - 10f;
        if (logo != null) {
            float anchoLogo = ALTO_LOGO * logo.getWidth() / logo.getHeight();
            flujo.drawImage(logo, MARGEN, arriba - ALTO_LOGO, anchoLogo, ALTO_LOGO);
        } else {
            texto("ARQUINOVA", MARGEN, arriba - 26, negrita, 18, VERDE);
        }
        float anchoTitulo = ancho(tituloCorto, negrita, 11);
        texto(tituloCorto, tamano.getWidth() - MARGEN - anchoTitulo, arriba - ALTO_LOGO / 2 - 4, negrita, 11, VERDE);
        relleno(0, tamano.getHeight() - ALTO_CABECERA, tamano.getWidth(), 3f, VERDE);
    }

    /** El logo viene con la aplicación (resources/documento); si no se encuentra, la cabecera usa el nombre en texto. */
    private PDImageXObject cargarLogo() {
        try (InputStream in = PdfReporte.class.getResourceAsStream(RECURSO_LOGO)) {
            if (in == null) return null;
            return PDImageXObject.createFromByteArray(documento, in.readAllBytes(), "logo-arquinova");
        } catch (IOException ex) {
            return null;
        }
    }

    private void asegurar(float alto) throws IOException {
        if (y - alto < PIE + 8) nuevaPagina();
    }

    /**
     * Ancho de cada columna según lo que contiene: las fechas, cifras y Sí/No se muestran completas; el espacio que queda se
     * reparte entre las de texto, que se recortan con "..." si no caben. Así una fecha nunca sale cortada.
     */
    private <T> float[] anchosColumnas(List<Columna<T>> columnas, List<T> filas) throws IOException {
        int n = columnas.size();
        float[] natural = new float[n];
        for (int i = 0; i < n; i++) {
            Columna<T> col = columnas.get(i);
            float max = ancho(col.titulo(), negrita, 9);
            for (int f = 0; f < Math.min(filas.size(), 300); f++) {
                max = Math.max(max, ancho(col.formatear(col.valor().apply(filas.get(f))), normal, 8.5f));
            }
            natural[i] = max + 12f;
        }

        float disponible = ancho();
        float fijos = 0;
        float textoNatural = 0;
        int columnasTexto = 0;
        for (int i = 0; i < n; i++) {
            if (columnas.get(i).tipo() == Columna.Tipo.TEXTO) {
                natural[i] = Math.min(natural[i], disponible * 0.32f);
                textoNatural += natural[i];
                columnasTexto++;
            } else {
                fijos += natural[i];
            }
        }

        float[] anchos = new float[n];
        if (columnasTexto == 0) {
            for (int i = 0; i < n; i++) anchos[i] = disponible * natural[i] / fijos;
            return anchos;
        }
        float paraTexto = Math.max(disponible - fijos, 36f * columnasTexto);
        if (textoNatural <= paraTexto) {
            // Todo cabe: el espacio sobrante se reparte entre las columnas de texto
            float factor = paraTexto / textoNatural;
            for (int i = 0; i < n; i++) anchos[i] = columnas.get(i).tipo() == Columna.Tipo.TEXTO ? natural[i] * factor : natural[i];
        } else {
            // No cabe: las columnas cortas (teléfono, estado, lote) se quedan completas y las largas (mensaje, correo) se recortan
            List<Integer> orden = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (columnas.get(i).tipo() == Columna.Tipo.TEXTO) orden.add(i);
                else anchos[i] = natural[i];
            }
            orden.sort((a, b) -> Float.compare(natural[a], natural[b]));
            float restante = paraTexto;
            for (int k = 0; k < orden.size(); k++) {
                int i = orden.get(k);
                float parte = restante / (orden.size() - k);
                anchos[i] = Math.max(36f, Math.min(natural[i], parte));
                restante -= anchos[i];
            }
        }
        float total = 0;
        for (float a : anchos) total += a;
        // Si aun así se pasa del ancho de la hoja (muchas columnas fijas), se reduce todo en la misma proporción
        if (total > disponible) {
            float escala = disponible / total;
            for (int i = 0; i < n; i++) anchos[i] *= escala;
        }
        return anchos;
    }

    private <T> void encabezadoTabla(List<Columna<T>> columnas, float[] anchos) throws IOException {
        float alto = 20f;
        asegurar(alto + 16);
        relleno(MARGEN, y - alto, ancho(), alto, VERDE);
        float x = MARGEN;
        for (int c = 0; c < columnas.size(); c++) {
            celda(columnas.get(c).titulo(), x, anchos[c], y + 2, negrita, 9, columnas.get(c).esNumerica(), Color.WHITE);
            x += anchos[c];
        }
        y -= alto;
    }

    /** Texto de una celda: se recorta con "..." si no cabe y se alinea a la derecha si es una cifra. */
    private void celda(String texto, float x, float anchoCol, float yFila, PDType1Font fuente, float tamanoLetra, boolean derecha, Color color) throws IOException {
        float disponible = anchoCol - 8;
        String ajustado = ajustar(limpiar(texto), fuente, tamanoLetra, disponible);
        float inicio = derecha ? x + anchoCol - 4 - ancho(ajustado, fuente, tamanoLetra) : x + 4;
        escribir(flujo, ajustado, inicio, yFila - 12, fuente, tamanoLetra, color);
    }

    private void texto(String texto, float x, float yBase, PDType1Font fuente, float tamanoLetra, Color color) throws IOException {
        escribir(flujo, limpiar(texto), x, yBase, fuente, tamanoLetra, color);
    }

    private void escribir(PDPageContentStream destino, String texto, float x, float yBase, PDType1Font fuente, float tamanoLetra, Color color) throws IOException {
        if (texto.isEmpty()) return;
        destino.beginText();
        destino.setFont(fuente, tamanoLetra);
        destino.setNonStrokingColor(color);
        destino.newLineAtOffset(x, yBase);
        destino.showText(limpiar(texto));
        destino.endText();
    }

    private void relleno(float x, float yInferior, float ancho, float alto, Color color) throws IOException {
        flujo.setNonStrokingColor(color);
        flujo.addRect(x, yInferior, ancho, alto);
        flujo.fill();
    }

    private void linea(float x1, float y1, float x2, float y2, Color color, float grosor) throws IOException {
        flujo.setStrokingColor(color);
        flujo.setLineWidth(grosor);
        flujo.moveTo(x1, y1);
        flujo.lineTo(x2, y2);
        flujo.stroke();
    }

    private float ancho(String texto, PDType1Font fuente, float tamanoLetra) throws IOException {
        return fuente.getStringWidth(limpiar(texto)) / 1000f * tamanoLetra;
    }

    private String ajustar(String texto, PDType1Font fuente, float tamanoLetra, float maximo) throws IOException {
        if (ancho(texto, fuente, tamanoLetra) <= maximo) return texto;
        String corto = texto;
        while (corto.length() > 1 && ancho(corto + "...", fuente, tamanoLetra) > maximo) {
            corto = corto.substring(0, corto.length() - 1);
        }
        return corto + "...";
    }

    private List<String> envolver(String texto, PDType1Font fuente, float tamanoLetra, float maximo) throws IOException {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : limpiar(texto).split(" ")) {
            if (palabra.isEmpty()) continue;
            String candidata = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (ancho(candidata, fuente, tamanoLetra) <= maximo) {
                actual = new StringBuilder(candidata);
                continue;
            }
            if (!actual.isEmpty()) lineas.add(actual.toString());
            actual = new StringBuilder(ajustar(palabra, fuente, tamanoLetra, maximo));
        }
        if (!actual.isEmpty()) lineas.add(actual.toString());
        if (lineas.isEmpty()) lineas.add("");
        return lineas;
    }

    /** Quita saltos de línea y reemplaza por "?" lo que las fuentes estándar del PDF no pueden dibujar (emojis, símbolos raros). */
    static String limpiar(String texto) {
        if (texto == null) return "";
        StringBuilder sb = new StringBuilder(texto.length());
        synchronized (WIN_ANSI) {
            for (int i = 0; i < texto.length(); i++) {
                char c = texto.charAt(i);
                if (c == '\n' || c == '\r' || c == '\t') sb.append(' ');
                else if (c < 0x20) continue;
                else if (WIN_ANSI.canEncode(c)) sb.append(c);
                else if (!Character.isLowSurrogate(c)) sb.append('?');
            }
        }
        return sb.toString();
    }

    int paginas() {
        return paginas;
    }
}
