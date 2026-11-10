package com.constructora_backend.documento;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Un texto escrito por una persona que empieza con = + - @ nunca debe poder leerse como fórmula en el Excel de un reporte. */
class ExcelReporteSeguridadTest {

    private record Fila(String nombre, String mensaje) {
    }

    private static Cell celdaConTexto(Sheet hoja, String texto) {
        for (Row fila : hoja) {
            for (Cell celda : fila) {
                if (celda.getCellType() == CellType.STRING && celda.getStringCellValue().equals(texto)) return celda;
            }
        }
        throw new AssertionError("No hay una celda con el texto: " + texto);
    }

    @Test
    @DisplayName("Las celdas de texto que parecen fórmula quedan como texto forzado; las normales no cambian")
    void textoQueParecefFormula() throws Exception {
        byte[] excel = ExcelReporte.generar("Solicitudes", "Solicitudes", List.of(),
                List.of(Columna.<Fila>texto("Nombre", 20, Fila::nombre), Columna.<Fila>texto("Mensaje", 40, Fila::mensaje)),
                List.of(new Fila("=HYPERLINK(\"http://evil.test\",\"clic\")", "+57 316 865 3715"),
                        new Fila("@SUM(1+1)", "-2+3"),
                        new Fila("Ana Gómez", "Hola, quiero información")),
                Map.of());

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet hoja = libro.getSheetAt(0);
            for (String peligroso : new String[]{"=HYPERLINK(\"http://evil.test\",\"clic\")", "+57 316 865 3715", "@SUM(1+1)", "-2+3"}) {
                Cell celda = celdaConTexto(hoja, peligroso);
                assertThat(celda.getCellType()).as(peligroso).isEqualTo(CellType.STRING);
                assertThat(((XSSFCellStyle) celda.getCellStyle()).getQuotePrefixed()).as(peligroso).isTrue();
            }
            for (String normal : new String[]{"Ana Gómez", "Hola, quiero información"}) {
                assertThat(((XSSFCellStyle) celdaConTexto(hoja, normal).getCellStyle()).getQuotePrefixed()).as(normal).isFalse();
            }
        }
    }
}
