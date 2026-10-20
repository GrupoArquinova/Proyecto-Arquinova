package com.constructora_backend.documento;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Arma el libro de Excel de un reporte: hoja con título, filtros usados y fecha de emisión; tabla con encabezado fijo, filtros de
 * columna, fechas y cifras con su formato real, fila de totales, e impresión en horizontal; y una hoja "Resumen" con los totales.
 */
public final class ExcelReporte {

    /** Fila (0-based) donde va el encabezado de la tabla; encima están título, emisión y filtros. */
    static final int FILA_ENCABEZADO = 4;
    private static final int MAX_TEXTO = 32000;
    private static final DateTimeFormatter EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final byte[] VERDE = {(byte) 0x2C, (byte) 0x59, (byte) 0x53};
    private static final byte[] VERDE_CLARO = {(byte) 0xE8, (byte) 0xF0, (byte) 0xEF};

    private ExcelReporte() {
    }

    public static <T> byte[] generar(String titulo, String nombreHoja, List<String> filtros, List<Columna<T>> columnas,
                                     List<T> filas, Map<String, Object> resumen) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(libro);
            Sheet hoja = libro.createSheet(nombreHoja);
            int n = columnas.size();

            // Título, emisión y filtros
            fila(hoja, 0, titulo, estilos.titulo, n);
            fila(hoja, 1, "Emitido el " + LocalDateTime.now().format(EMISION), estilos.nota, n);
            fila(hoja, 2, filtros.isEmpty() ? "Sin filtros: todos los registros activos" : String.join("  ·  ", filtros), estilos.nota, n);
            hoja.getRow(0).setHeightInPoints(26);

            // Encabezado
            Row encabezado = hoja.createRow(FILA_ENCABEZADO);
            encabezado.setHeightInPoints(22);
            for (int c = 0; c < n; c++) {
                Cell celda = encabezado.createCell(c);
                celda.setCellValue(columnas.get(c).titulo());
                celda.setCellStyle(estilos.encabezado);
                hoja.setColumnWidth(c, Math.min(255, columnas.get(c).ancho() + 2) * 256);
            }

            // Datos
            int primera = FILA_ENCABEZADO + 1;
            for (int i = 0; i < filas.size(); i++) {
                Row row = hoja.createRow(primera + i);
                for (int c = 0; c < n; c++) {
                    Columna<T> col = columnas.get(c);
                    escribir(row.createCell(c), col, col.valor().apply(filas.get(i)), estilos);
                }
            }

            // Totales: SUBTOTAL se ajusta cuando se filtra la tabla
            int ultima = primera + filas.size() - 1;
            if (!filas.isEmpty()) {
                escribirTotales(hoja, columnas, filas, estilos, primera, ultima);
            }

            hoja.setAutoFilter(new CellRangeAddress(FILA_ENCABEZADO, Math.max(FILA_ENCABEZADO, ultima), 0, Math.max(0, n - 1)));
            hoja.createFreezePane(0, FILA_ENCABEZADO + 1);
            hoja.getPrintSetup().setLandscape(true);
            hoja.setFitToPage(true);
            hoja.getPrintSetup().setFitWidth((short) 1);
            hoja.getPrintSetup().setFitHeight((short) 0);
            hoja.setRepeatingRows(new CellRangeAddress(FILA_ENCABEZADO, FILA_ENCABEZADO, 0, Math.max(0, n - 1)));

            if (resumen != null && !resumen.isEmpty()) {
                hojaResumen(libro, estilos, titulo, resumen);
            }

            libro.write(salida);
            return salida.toByteArray();
        }
    }

    // ------------------------------------------------------------------ piezas

    private static void fila(Sheet hoja, int indice, String texto, CellStyle estilo, int columnas) {
        Row row = hoja.createRow(indice);
        Cell celda = row.createCell(0);
        celda.setCellValue(limitar(texto));
        celda.setCellStyle(estilo);
        if (columnas > 1) hoja.addMergedRegion(new CellRangeAddress(indice, indice, 0, columnas - 1));
    }

    private static <T> void escribir(Cell celda, Columna<T> col, Object valor, Estilos estilos) {
        celda.setCellStyle(estilos.dato(col.tipo()));
        if (valor == null) return;
        switch (col.tipo()) {
            case ENTERO, DECIMAL, PORCENTAJE -> {
                if (valor instanceof Number num) celda.setCellValue(num.doubleValue());
                else celda.setCellValue(limitar(valor.toString()));
            }
            case FECHA -> {
                if (valor instanceof LocalDate d) celda.setCellValue(d);
                else celda.setCellValue(limitar(valor.toString()));
            }
            case FECHA_HORA -> {
                if (valor instanceof LocalDateTime d) celda.setCellValue(d);
                else celda.setCellValue(limitar(valor.toString()));
            }
            case SI_NO -> celda.setCellValue(Boolean.TRUE.equals(valor) ? "Sí" : "No");
            case TEXTO -> celda.setCellValue(limitar(valor.toString()));
        }
    }

    private static <T> void escribirTotales(Sheet hoja, List<Columna<T>> columnas, List<T> filas, Estilos estilos, int primera, int ultima) {
        Row total = hoja.createRow(ultima + 1);
        for (int c = 0; c < columnas.size(); c++) {
            Cell celda = total.createCell(c);
            celda.setCellStyle(estilos.total(columnas.get(c).tipo()));
        }
        total.getCell(0).setCellValue("TOTAL (" + filas.size() + (filas.size() == 1 ? " registro)" : " registros)"));

        boolean evaluable = true;
        for (int c = 0; c < columnas.size(); c++) {
            Columna<T> col = columnas.get(c);
            if (!col.sumar()) continue;
            String letra = CellReference.convertNumToColString(c);
            Cell celda = total.getCell(c);
            celda.setCellFormula("SUBTOTAL(109," + letra + (primera + 1) + ":" + letra + (ultima + 1) + ")");
        }
        try {
            hoja.getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateAll();
        } catch (RuntimeException ex) {
            evaluable = false;
        }
        if (!evaluable) {
            // Si el evaluador no puede calcularlo, se deja la suma fija para que siempre haya un número
            for (int c = 0; c < columnas.size(); c++) {
                Columna<T> col = columnas.get(c);
                if (!col.sumar()) continue;
                double suma = 0;
                for (T fila : filas) {
                    if (col.valor().apply(fila) instanceof Number num) suma += num.doubleValue();
                }
                total.getCell(c).setCellValue(suma);
            }
        }
    }

    private static void hojaResumen(XSSFWorkbook libro, Estilos estilos, String titulo, Map<String, Object> resumen) {
        Sheet hoja = libro.createSheet("Resumen");
        fila(hoja, 0, "Resumen · " + titulo, estilos.titulo, 2);
        hoja.getRow(0).setHeightInPoints(26);
        int i = 2;
        for (Map.Entry<String, Object> e : resumen.entrySet()) {
            Row row = hoja.createRow(i++);
            Cell etiqueta = row.createCell(0);
            etiqueta.setCellValue(limitar(e.getKey()));
            etiqueta.setCellStyle(estilos.etiquetaResumen);
            Cell valor = row.createCell(1);
            if (e.getValue() instanceof Number num) {
                valor.setCellValue(num.doubleValue());
                boolean entero = num instanceof Long || num instanceof Integer || num instanceof Short;
                valor.setCellStyle(entero ? estilos.dato(Columna.Tipo.ENTERO) : estilos.dato(Columna.Tipo.DECIMAL));
            } else {
                valor.setCellValue(limitar(String.valueOf(e.getValue())));
                valor.setCellStyle(estilos.dato(Columna.Tipo.TEXTO));
            }
        }
        hoja.setColumnWidth(0, 40 * 256);
        hoja.setColumnWidth(1, 22 * 256);
    }

    private static String limitar(String texto) {
        if (texto == null) return "";
        return texto.length() > MAX_TEXTO ? texto.substring(0, MAX_TEXTO) : texto;
    }

    // ------------------------------------------------------------------ estilos

    private static final class Estilos {
        final XSSFCellStyle titulo;
        final XSSFCellStyle nota;
        final XSSFCellStyle encabezado;
        final XSSFCellStyle etiquetaResumen;
        private final XSSFWorkbook libro;
        private final DataFormat formato;
        private final Map<Columna.Tipo, CellStyle> datos = new EnumMap<>(Columna.Tipo.class);
        private final Map<Columna.Tipo, CellStyle> totales = new EnumMap<>(Columna.Tipo.class);

        Estilos(XSSFWorkbook libro) {
            this.libro = libro;
            this.formato = libro.createDataFormat();

            XSSFFont fuenteTitulo = fuente(true, 15, VERDE);
            titulo = libro.createCellStyle();
            titulo.setFont(fuenteTitulo);
            titulo.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFFont fuenteNota = fuente(false, 10, new byte[]{0x64, 0x74, (byte) 0x8B});
            nota = libro.createCellStyle();
            nota.setFont(fuenteNota);

            XSSFFont fuenteEncabezado = fuente(true, 11, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
            encabezado = libro.createCellStyle();
            encabezado.setFont(fuenteEncabezado);
            encabezado.setFillForegroundColor(new XSSFColor(VERDE, null));
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            encabezado.setVerticalAlignment(VerticalAlignment.CENTER);
            encabezado.setWrapText(true);

            etiquetaResumen = libro.createCellStyle();
            etiquetaResumen.setFont(fuente(true, 11, VERDE));
            etiquetaResumen.setBorderBottom(BorderStyle.HAIR);
        }

        private XSSFFont fuente(boolean negrita, int tamano, byte[] color) {
            XSSFFont f = libro.createFont();
            f.setBold(negrita);
            f.setFontHeightInPoints((short) tamano);
            f.setColor(new XSSFColor(color, null));
            return f;
        }

        CellStyle dato(Columna.Tipo tipo) {
            return datos.computeIfAbsent(tipo, t -> construir(t, false));
        }

        CellStyle total(Columna.Tipo tipo) {
            return totales.computeIfAbsent(tipo, t -> construir(t, true));
        }

        private CellStyle construir(Columna.Tipo tipo, boolean total) {
            XSSFCellStyle e = libro.createCellStyle();
            e.setVerticalAlignment(VerticalAlignment.CENTER);
            e.setIndention((short) 1);
            e.setBorderBottom(total ? BorderStyle.MEDIUM : BorderStyle.HAIR);
            if (total) {
                e.setBorderTop(BorderStyle.THIN);
                e.setFont(fuente(true, 11, VERDE));
                e.setFillForegroundColor(new XSSFColor(VERDE_CLARO, null));
                e.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            switch (tipo) {
                case ENTERO -> {
                    e.setDataFormat(formato.getFormat("#,##0"));
                    e.setAlignment(HorizontalAlignment.RIGHT);
                }
                case DECIMAL -> {
                    e.setDataFormat(formato.getFormat("#,##0.00"));
                    e.setAlignment(HorizontalAlignment.RIGHT);
                }
                case PORCENTAJE -> {
                    e.setDataFormat(formato.getFormat("0.00\"%\""));
                    e.setAlignment(HorizontalAlignment.RIGHT);
                }
                case FECHA -> {
                    e.setDataFormat(formato.getFormat("dd/mm/yyyy"));
                    e.setAlignment(HorizontalAlignment.CENTER);
                }
                case FECHA_HORA -> {
                    e.setDataFormat(formato.getFormat("dd/mm/yyyy hh:mm"));
                    e.setAlignment(HorizontalAlignment.CENTER);
                }
                case SI_NO -> e.setAlignment(HorizontalAlignment.CENTER);
                case TEXTO -> e.setAlignment(HorizontalAlignment.LEFT);
            }
            return e;
        }
    }
}
