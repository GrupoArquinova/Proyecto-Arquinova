package com.constructora_backend.service;

import com.constructora_backend.dto.*;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.repository.LoteRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteDocumentoService {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private LoteRepository loteRepository;

    // =========================================================================
    // 1. EXCEL (.XLSX) - SIN COLUMNA DE PRECIOS
    // =========================================================================
    @Transactional(readOnly = true)
    public byte[] generarLotesExcel(FiltroReporteDTO filtro) throws IOException {
        List<ReporteLoteItemDTO> lotes = reporteService.consultarReporteLotes(filtro);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventario de Lotes");
            CellStyle headerCellStyle = crearEstiloEncabezado(workbook);

            String[] headers = {"ID", "Proyecto", "Etapa", "Código", "Nombre", "Área (m²)", "Estado Comercial", "Publicado", "Activo"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for (ReporteLoteItemDTO lote : lotes) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(lote.getId());
                row.createCell(1).setCellValue(lote.getProyectoNombre());
                row.createCell(2).setCellValue(lote.getEtapaNombre());
                row.createCell(3).setCellValue(lote.getCodigo());
                row.createCell(4).setCellValue(lote.getLoteNombre() != null ? lote.getLoteNombre() : "");
                row.createCell(5).setCellValue(lote.getAreaM2() != null ? lote.getAreaM2().doubleValue() : 0.0);
                row.createCell(6).setCellValue(lote.getEstadoComercial());
                row.createCell(7).setCellValue(Boolean.TRUE.equals(lote.getPublicado()) ? "SÍ" : "NO");
                row.createCell(8).setCellValue(Boolean.TRUE.equals(lote.getActivo()) ? "SÍ" : "NO");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarSolicitudesExcel(FiltroReporteDTO filtro) throws IOException {
        List<ReporteSolicitudItemDTO> solicitudes = reporteService.consultarReporteSolicitudes(filtro);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Solicitudes de Contacto");
            CellStyle headerCellStyle = crearEstiloEncabezado(workbook);

            String[] headers = {"ID", "Proyecto", "Código Lote", "Cliente", "Correo", "Teléfono", "Estado", "Atendida Por", "Fecha Registro"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for (ReporteSolicitudItemDTO sol : solicitudes) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(sol.getId());
                row.createCell(1).setCellValue(sol.getProyectoNombre());
                row.createCell(2).setCellValue(sol.getLoteCodigo());
                row.createCell(3).setCellValue(sol.getClienteNombre());
                row.createCell(4).setCellValue(sol.getClienteCorreo());
                row.createCell(5).setCellValue(sol.getClienteTelefono());
                row.createCell(6).setCellValue(sol.getEstadoNombre());
                row.createCell(7).setCellValue(sol.getAtendidaPorNombre());
                row.createCell(8).setCellValue(sol.getCreadoEn() != null ? sol.getCreadoEn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarProyectosExcel(FiltroReporteDTO filtro) throws IOException {
        List<ReporteProyectoItemDTO> proyectos = reporteService.consultarReporteProyectos(filtro);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte de Proyectos");
            CellStyle headerCellStyle = crearEstiloEncabezado(workbook);

            String[] headers = {"ID", "Empresa", "Nombre", "Estado", "Publicado", "Activo", "Fecha Lanzamiento", "Total Lotes", "Creado En"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for (ReporteProyectoItemDTO p : proyectos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getEmpresaNombre());
                row.createCell(2).setCellValue(p.getNombre());
                row.createCell(3).setCellValue(p.getEstadoProyecto());
                row.createCell(4).setCellValue(Boolean.TRUE.equals(p.getPublicado()) ? "SÍ" : "NO");
                row.createCell(5).setCellValue(Boolean.TRUE.equals(p.getActivo()) ? "SÍ" : "NO");
                row.createCell(6).setCellValue(p.getFechaLanzamiento() != null ? p.getFechaLanzamiento().toString() : "");
                row.createCell(7).setCellValue(p.getTotalLotes() != null ? p.getTotalLotes() : 0);
                row.createCell(8).setCellValue(p.getCreadoEn() != null ? p.getCreadoEn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarEstadosComercialesExcel(FiltroReporteDTO filtro) throws IOException {
        List<EstadisticaEstadoComercialDTO> estadisticas = reporteService.consultarEstadisticasEstadosComerciales(filtro);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Estados Comerciales");
            CellStyle headerCellStyle = crearEstiloEncabezado(workbook);

            String[] headers = {"Proyecto", "Estado Comercial", "Cantidad de Lotes", "Porcentaje (%)"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for (EstadisticaEstadoComercialDTO e : estadisticas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(e.getProyectoNombre());
                row.createCell(1).setCellValue(e.getEstadoNombre());
                row.createCell(2).setCellValue(e.getCantidadLotes());
                row.createCell(3).setCellValue(e.getPorcentaje() != null ? e.getPorcentaje().doubleValue() : 0.0);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarActividadExcel(FiltroReporteDTO filtro) throws IOException {
        List<ReporteActividadItemDTO> actividad = reporteService.consultarReporteActividad(filtro);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Actividad Administrativa");
            CellStyle headerCellStyle = crearEstiloEncabezado(workbook);

            String[] headers = {"ID", "Usuario", "Correo", "Acción", "Entidad", "ID Entidad", "Descripción", "IP", "Fecha"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for (ReporteActividadItemDTO a : actividad) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getId());
                row.createCell(1).setCellValue(a.getUsuarioNombre());
                row.createCell(2).setCellValue(a.getUsuarioCorreo());
                row.createCell(3).setCellValue(a.getAccion());
                row.createCell(4).setCellValue(a.getEntidad());
                row.createCell(5).setCellValue(a.getEntidadId() != null ? a.getEntidadId() : 0);
                row.createCell(6).setCellValue(a.getDescripcion() != null ? a.getDescripcion() : "");
                row.createCell(7).setCellValue(a.getIp() != null ? a.getIp() : "");
                row.createCell(8).setCellValue(a.getCreadoEn() != null ? a.getCreadoEn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // Método auxiliar para no repetir el estilo de encabezado en cada método
    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(headerFont);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    // =========================================================================
    // 2. WORD (.DOCX) - SIN MENCIONES MONETARIAS
    // =========================================================================
    @Transactional(readOnly = true)
    public byte[] generarMinutaPromesaCompraventaWord(GenerarMinutaDTO dto) throws IOException {
        Lote lote = loteRepository.findById(dto.getLoteId())
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + dto.getLoteId()));

        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("PROMESA DE COMPRAVENTA DE INMUEBLE");
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setFontFamily("Arial");
            titleRun.addBreak();

            XWPFParagraph p1 = document.createParagraph();
            p1.setAlignment(ParagraphAlignment.BOTH);
            XWPFRun r1 = p1.createRun();
            r1.setFontFamily("Arial");
            r1.setFontSize(11);
            r1.setText(String.format(
                    "Entre los suscritos a saber, CONSTRUCTORA ARQUINOVA S.A.S., y por otra parte el/la señor(a) %s, " +
                            "identificado(a) con C.C. No. %s, celebran la presente Promesa de Compraventa.",
                    dto.getCompradorNombre(), dto.getCompradorCedula()
            ));

            XWPFParagraph p2 = document.createParagraph();
            p2.setAlignment(ParagraphAlignment.BOTH);
            XWPFRun r2 = p2.createRun();
            r2.setFontFamily("Arial");
            r2.setFontSize(11);
            r2.setBold(true);
            r2.setText("PRIMERA. OBJETO Y ESPECIFICACIONES: ");
            XWPFRun r2Content = p2.createRun();
            r2Content.setFontFamily("Arial");
            r2Content.setFontSize(11);
            r2Content.setText(String.format(
                    "LA CONSTRUCTORA se compromete a adjudicar el lote identificado con código %s, " +
                            "ubicado en el Proyecto %s (%s), con un área de terreno de %.2f m².",
                    lote.getCodigo(),
                    lote.getEtapa().getProyecto().getNombre(),
                    lote.getEtapa().getNombre(),
                    lote.getAreaM2().doubleValue()
            ));

            XWPFParagraph pFirmas = document.createParagraph();
            XWPFRun rFirmas = pFirmas.createRun();
            rFirmas.addBreak();
            rFirmas.addBreak();
            rFirmas.setFontFamily("Arial");
            rFirmas.setFontSize(11);
            rFirmas.setText("_____________________________                  _____________________________");
            rFirmas.addBreak();
            rFirmas.setText("PROMETIENTE VENDEDOR                              PROMETIENTE COMPRADOR");

            document.write(out);
            return out.toByteArray();
        }
    }

    // =========================================================================
    // 3. PDF (.PDF) - APACHE PDFBOX
    // =========================================================================
    @Transactional(readOnly = true)
    public byte[] generarFichaTecnicaProyectoPDF(Long proyectoId) throws IOException {
        FiltroReporteDTO filtro = new FiltroReporteDTO();
        filtro.setProyectoId(proyectoId);

        List<ReporteProyectoItemDTO> proyectos = reporteService.consultarReporteProyectos(filtro);
        if (proyectos.isEmpty()) {
            throw new ResourceNotFoundException("Proyecto no encontrado con ID: " + proyectoId);
        }

        ReporteProyectoItemDTO p = proyectos.get(0);
        List<ReporteLoteItemDTO> lotes = reporteService.consultarReporteLotes(filtro);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                content.newLineAtOffset(50, 750);
                content.showText("FICHA TÉCNICA DEL PROYECTO");
                content.endText();

                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.newLineAtOffset(50, 735);
                content.showText("Emitido el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                content.endText();

                int y = 700;
                y = agregarLineaTexto(content, "Proyecto: " + p.getNombre(), 50, y, 12, true);
                y = agregarLineaTexto(content, "Empresa Desarrolladora: " + p.getEmpresaNombre(), 50, y, 11, false);
                y = agregarLineaTexto(content, "Estado del Proyecto: " + p.getEstadoProyecto(), 50, y, 11, false);
                y = agregarLineaTexto(content, "Total Lotes Asociados: " + p.getTotalLotes(), 50, y, 11, false);

                y -= 20;
                y = agregarLineaTexto(content, "RESUMEN DE LOTES:", 50, y, 12, true);

                y = agregarLineaTexto(content, String.format("%-15s | %-20s | %-12s | %-15s", "CÓDIGO", "ETAPA", "ÁREA (m²)", "ESTADO"), 50, y, 10, true);

                int contador = 0;
                for (ReporteLoteItemDTO l : lotes) {
                    if (contador++ >= 20) break;
                    String linea = String.format("%-15s | %-20s | %-12.2f | %-15s",
                            l.getCodigo(),
                            cortaTexto(l.getEtapaNombre(), 20),
                            l.getAreaM2() != null ? l.getAreaM2().doubleValue() : 0.0,
                            l.getEstadoComercial()
                    );
                    y = agregarLineaTexto(content, linea, 50, y, 9, false);
                }
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private int agregarLineaTexto(PDPageContentStream stream, String texto, int x, int y, int fontSize, boolean isBold) throws IOException {
        stream.beginText();
        if (isBold) {
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), fontSize);
        } else {
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
        }
        stream.newLineAtOffset(x, y);
        stream.showText(texto);
        stream.endText();
        return y - 18;
    }

    private String cortaTexto(String txt, int max) {
        if (txt == null) return "";
        return txt.length() <= max ? txt : txt.substring(0, max - 2) + "..";
    }
}