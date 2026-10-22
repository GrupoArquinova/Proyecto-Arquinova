package com.constructora_backend.service;

import com.constructora_backend.dto.*;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.repository.EstadoLoteRepository;
import com.constructora_backend.repository.EstadoSolicitudRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.UbicacionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/** Abre los archivos que se generan y comprueba su contenido: hojas, formatos, totales, páginas y que no falte ningún lote. */
@ExtendWith(MockitoExtension.class)
class ReporteDocumentoServiceTest {

    @Mock
    private ReporteService reporteService;
    @Mock
    private LoteRepository loteRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private EstadoLoteRepository estadoLoteRepository;
    @Mock
    private EstadoSolicitudRepository estadoSolicitudRepository;
    @Mock
    private UbicacionRepository ubicacionRepository;

    @InjectMocks
    private ReporteDocumentoService servicio;

    private static ReporteLoteItemDTO lote(int n, String estado) {
        return new ReporteLoteItemDTO((long) n, "El Encanto", n % 2 == 0 ? "Etapa 1" : "Etapa 2", String.format("LT-%03d", n),
                "Lote " + n, new BigDecimal("800.50"), estado, true, true, LocalDateTime.of(2026, 10, 14, 9, 30));
    }

    private static List<ReporteLoteItemDTO> lotes(int cuantos) {
        List<ReporteLoteItemDTO> lista = new ArrayList<>();
        for (int i = 1; i <= cuantos; i++) lista.add(lote(i, i % 3 == 0 ? "VENDIDO" : i % 3 == 1 ? "DISPONIBLE" : "RESERVADO"));
        return lista;
    }

    private static String textoDe(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }

    private static int paginasDe(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return documento.getNumberOfPages();
        }
    }

    // ------------------------------------------------------------------ Excel

    @Test
    @DisplayName("Excel de lotes: título, filtros, encabezado fijo, fechas y cifras reales, totales y hoja Resumen")
    void excelDeLotes() throws IOException {
        lenient().when(reporteService.consultarReporteLotes(any())).thenReturn(lotes(3));
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("El Encanto");
        lenient().when(proyectoRepository.findById(5L)).thenReturn(Optional.of(proyecto));
        FiltroReporteDTO filtro = new FiltroReporteDTO();
        filtro.setProyectoId(5L);

        byte[] bytes = servicio.generarLotesExcel(filtro);

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(List.of("Lotes", "Resumen"), List.of(libro.getSheetName(0), libro.getSheetName(1)));
            XSSFSheet hoja = libro.getSheet("Lotes");

            assertEquals("Reporte de lotes", hoja.getRow(0).getCell(0).getStringCellValue());
            assertTrue(hoja.getRow(1).getCell(0).getStringCellValue().startsWith("Emitido el "));
            String filtros = hoja.getRow(2).getCell(0).getStringCellValue();
            assertTrue(filtros.contains("Proyecto: El Encanto"), filtros);
            assertTrue(filtros.contains("Solo registros activos"), filtros);

            Row encabezado = hoja.getRow(4);
            assertEquals("Proyecto", encabezado.getCell(0).getStringCellValue());
            assertEquals("Área (m²)", encabezado.getCell(4).getStringCellValue());
            assertNotNull(hoja.getPaneInformation(), "el encabezado queda fijo al desplazarse");
            assertEquals(5, hoja.getPaneInformation().getHorizontalSplitTopRow());
            assertTrue(hoja.getCTWorksheet().isSetAutoFilter(), "filtros de columna");

            Row primera = hoja.getRow(5);
            assertEquals("LT-001", primera.getCell(2).getStringCellValue());
            assertEquals(CellType.NUMERIC, primera.getCell(4).getCellType(), "el área es un número, no texto");
            assertEquals(800.50, primera.getCell(4).getNumericCellValue(), 0.001);
            assertEquals("Disponible", primera.getCell(5).getStringCellValue());
            assertEquals("Sí", primera.getCell(6).getStringCellValue());
            Cell fecha = primera.getCell(8);
            assertEquals(CellType.NUMERIC, fecha.getCellType(), "la fecha es una fecha real, no texto");
            assertTrue(DateUtil.isCellDateFormatted(fecha));

            Row total = hoja.getRow(8);
            assertEquals("TOTAL (3 registros)", total.getCell(0).getStringCellValue());
            assertEquals(CellType.FORMULA, total.getCell(4).getCellType(), "SUBTOTAL: se ajusta al filtrar la tabla");
            assertEquals(2401.50, total.getCell(4).getNumericCellValue(), 0.001, "el valor ya viene calculado");

            Sheet resumen = libro.getSheet("Resumen");
            assertEquals("Resumen · Reporte de lotes", resumen.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Total de lotes", resumen.getRow(2).getCell(0).getStringCellValue());
            assertEquals(3, resumen.getRow(2).getCell(1).getNumericCellValue(), 0.0);
        }
    }

    @Test
    @DisplayName("Excel de solicitudes: incluye tipología, mensaje y fecha de atención")
    void excelDeSolicitudes() throws IOException {
        ReporteSolicitudItemDTO s = new ReporteSolicitudItemDTO(1L, "El Encanto", "N/A", "Ana Ruiz", "ana@correo.com", "3001234567",
                "NUEVO", "Sin Atender", LocalDateTime.of(2026, 10, 14, 10, 0), "Villa Samán", "Quiero conocer la villa", null);
        lenient().when(reporteService.consultarReporteSolicitudes(any())).thenReturn(List.of(s));

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(servicio.generarSolicitudesExcel(new FiltroReporteDTO())))) {
            Row encabezado = libro.getSheet("Solicitudes").getRow(4);
            assertEquals("Tipología", encabezado.getCell(5).getStringCellValue());
            assertEquals("Mensaje", encabezado.getCell(7).getStringCellValue());
            Row fila = libro.getSheet("Solicitudes").getRow(5);
            assertEquals("Villa Samán", fila.getCell(5).getStringCellValue());
            assertEquals("Quiero conocer la villa", fila.getCell(7).getStringCellValue());
            assertEquals("Nuevo", fila.getCell(8).getStringCellValue());
            assertEquals(CellType.BLANK, fila.getCell(10).getCellType(), "sin atender: la fecha de atención queda vacía");
        }
    }

    @Test
    @DisplayName("Excel de etapas: lotes por estado y porcentaje vendido")
    void excelDeEtapas() throws IOException {
        lenient().when(reporteService.consultarReporteEtapas(any())).thenReturn(List.of(
                new ReporteEtapaItemDTO(1L, "El Encanto", 10L, "Etapa 1", 20L, 10L, 5L, 5L, new BigDecimal("16000.00")),
                new ReporteEtapaItemDTO(1L, "El Encanto", 11L, "Etapa 2", 0L, 0L, 0L, 0L, BigDecimal.ZERO)));

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(servicio.generarEtapasExcel(new FiltroReporteDTO())))) {
            Sheet hoja = libro.getSheet("Etapas");
            assertEquals(25.0, hoja.getRow(5).getCell(7).getNumericCellValue(), 0.001, "5 vendidos de 20 = 25 %");
            assertEquals(0.0, hoja.getRow(6).getCell(7).getNumericCellValue(), 0.001, "etapa sin lotes: 0 %");
            assertEquals(20.0, hoja.getRow(7).getCell(2).getNumericCellValue(), 0.001, "total de lotes de todas las etapas");
        }
    }

    // ------------------------------------------------------------------ PDF

    @Test
    @DisplayName("PDF de lotes: no corta la lista, continúa en más hojas y numera las páginas")
    void pdfDeLotesCompleto() throws IOException {
        lenient().when(reporteService.consultarReporteLotes(any())).thenReturn(lotes(120));

        byte[] pdf = servicio.generarLotesPdf(new FiltroReporteDTO());

        int paginas = paginasDe(pdf);
        assertTrue(paginas > 1, "120 lotes no caben en una hoja");
        String texto = textoDe(pdf);
        assertTrue(texto.contains("LT-001") && texto.contains("LT-060") && texto.contains("LT-120"), "aparecen todos los lotes, no solo 20");
        assertTrue(texto.contains("Página 1 de " + paginas));
        assertTrue(texto.contains("Página " + paginas + " de " + paginas));
        assertTrue(texto.contains("Reporte de lotes"));
        assertTrue(texto.contains("Resumen"));
        assertTrue(texto.contains("TOTAL (120)"));
    }

    @Test
    @DisplayName("Todos los PDF llevan el logo de Arquinova en cada hoja")
    void todosLosPdfLlevanElLogoEnCadaHoja() throws IOException {
        lenient().when(reporteService.consultarReporteLotes(any())).thenReturn(lotes(120));
        lenient().when(reporteService.consultarReporteSolicitudes(any())).thenReturn(List.of());
        lenient().when(reporteService.consultarReporteProyectos(any())).thenReturn(List.of());
        lenient().when(reporteService.consultarReporteEtapas(any())).thenReturn(List.of());
        lenient().when(reporteService.consultarEstadisticasEstadosComerciales(any())).thenReturn(List.of());
        lenient().when(reporteService.consultarReporteActividad(any())).thenReturn(List.of());
        FiltroReporteDTO filtro = new FiltroReporteDTO();

        List<byte[]> pdfs = List.of(servicio.generarLotesPdf(filtro), servicio.generarSolicitudesPdf(filtro), servicio.generarProyectosPdf(filtro),
                servicio.generarEtapasPdf(filtro), servicio.generarEstadosComercialesPdf(filtro), servicio.generarActividadPdf(filtro));

        for (byte[] pdf : pdfs) {
            try (PDDocument documento = Loader.loadPDF(pdf)) {
                for (int i = 0; i < documento.getNumberOfPages(); i++) {
                    boolean conImagen = false;
                    for (var nombre : documento.getPage(i).getResources().getXObjectNames()) {
                        conImagen |= documento.getPage(i).getResources().isImageXObject(nombre);
                    }
                    assertTrue(conImagen, "la hoja " + (i + 1) + " no tiene el logo");
                }
            }
        }
        assertTrue(paginasDe(pdfs.get(0)) > 1, "el logo se repite en las hojas siguientes");
    }

    @Test
    @DisplayName("PDF: los caracteres que la fuente no tiene (emojis) no rompen la generación")
    void pdfConCaracteresRaros() throws IOException {
        ReporteSolicitudItemDTO s = new ReporteSolicitudItemDTO(1L, "El Encanto", "N/A", "Ana 😀 Ruiz", "ana@correo.com", "300",
                "NUEVO", "Sin Atender", LocalDateTime.now(), "", "Hola 🌎 quiero información → ya\nsegunda línea", null);
        lenient().when(reporteService.consultarReporteSolicitudes(any())).thenReturn(List.of(s));

        byte[] pdf = assertDoesNotThrow(() -> servicio.generarSolicitudesPdf(new FiltroReporteDTO()));

        String texto = textoDe(pdf);
        assertTrue(texto.contains("quiero información"), "las tildes se conservan");
    }

    @Test
    @DisplayName("Ficha técnica: datos del proyecto, resumen por estado y el inventario completo en varias hojas")
    void fichaTecnica() throws IOException {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Condominio El Encanto");
        proyecto.setDescripcion("Proyecto campestre con lotes y villas rodeado de naturaleza.");
        lenient().when(proyectoRepository.findById(5L)).thenReturn(Optional.of(proyecto));
        lenient().when(reporteService.consultarReporteProyectos(any())).thenReturn(List.of(new ReporteProyectoItemDTO(
                5L, "Grupo Arquinova S.A.S.", "Condominio El Encanto", "EN_CONSTRUCCION", true, true, LocalDate.of(2026, 3, 1), 70L, LocalDateTime.now())));
        lenient().when(reporteService.consultarReporteLotes(any())).thenReturn(lotes(70));
        lenient().when(reporteService.consultarEstadisticasEstadosComerciales(any())).thenReturn(List.of(
                new EstadisticaEstadoComercialDTO(5L, "Condominio El Encanto", "DISPONIBLE", 24L, new BigDecimal("34.29")),
                new EstadisticaEstadoComercialDTO(5L, "Condominio El Encanto", "RESERVADO", 23L, new BigDecimal("32.86")),
                new EstadisticaEstadoComercialDTO(5L, "Condominio El Encanto", "VENDIDO", 23L, new BigDecimal("32.86"))));
        lenient().when(reporteService.consultarReporteEtapas(any())).thenReturn(List.of(
                new ReporteEtapaItemDTO(5L, "Condominio El Encanto", 1L, "Etapa 1", 35L, 12L, 12L, 11L, new BigDecimal("28000.00"))));
        lenient().when(ubicacionRepository.findByProyectoId(5L)).thenReturn(Optional.empty());

        byte[] pdf = servicio.generarFichaTecnicaProyectoPDF(5L);

        String texto = textoDe(pdf);
        assertTrue(paginasDe(pdf) >= 2, "70 lotes ocupan más de una hoja");
        assertTrue(texto.contains("Ficha técnica del proyecto"));
        assertTrue(texto.contains("Condominio El Encanto"));
        assertTrue(texto.contains("Grupo Arquinova S.A.S."));
        assertTrue(texto.contains("En construcción"), "la etapa sale en palabras, no como EN_CONSTRUCCION");
        assertTrue(texto.contains("Disponible") && texto.contains("Reservado") && texto.contains("Vendido"));
        assertTrue(texto.contains("LT-001") && texto.contains("LT-070"), "todos los lotes, sin cortar");
        assertTrue(texto.contains("Lotes por etapa"));
    }

    @Test
    @DisplayName("Ficha técnica de un proyecto que no existe: error 404")
    void fichaDeProyectoInexistente() {
        lenient().when(proyectoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.generarFichaTecnicaProyectoPDF(99L));
    }
}
