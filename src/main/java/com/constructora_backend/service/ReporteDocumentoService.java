package com.constructora_backend.service;

import com.constructora_backend.documento.Columna;
import com.constructora_backend.documento.Etiquetas;
import com.constructora_backend.documento.ExcelReporte;
import com.constructora_backend.documento.PdfReporte;
import com.constructora_backend.dto.*;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.EstadoSolicitud;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.repository.EstadoLoteRepository;
import com.constructora_backend.repository.EstadoSolicitudRepository;
import com.constructora_backend.repository.LoteRepository;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.repository.UbicacionRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Exporta los reportes a Excel (.xlsx) y a PDF, y genera la minuta de promesa en Word (.docx).
 * Cada reporte se define una sola vez (columnas, filas, filtros usados y resumen) y de ahí salen el Excel y el PDF.
 * No incluye precios.
 */
@Service
public class ReporteDocumentoService {

    private static final Locale CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private EstadoLoteRepository estadoLoteRepository;

    @Autowired
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    /** Un reporte listo para volcarse a Excel o a PDF. */
    private record Reporte<T>(String titulo, String hoja, List<String> filtros, List<Columna<T>> columnas, List<T> filas,
                              Map<String, Object> resumen) {
    }

    private enum Campo { PROYECTO, ESTADO_LOTE, ESTADO_SOLICITUD, ESTADO_PROYECTO, FECHAS, INACTIVOS }

    // =========================================================================
    // 1. EXCEL (.XLSX)
    // =========================================================================
    @Transactional(readOnly = true)
    public byte[] generarLotesExcel(FiltroReporteDTO filtro) throws IOException {
        return excel(reporteLotes(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarSolicitudesExcel(FiltroReporteDTO filtro) throws IOException {
        return excel(reporteSolicitudes(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarProyectosExcel(FiltroReporteDTO filtro) throws IOException {
        return excel(reporteProyectos(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarEstadosComercialesExcel(FiltroReporteDTO filtro) throws IOException {
        return excel(reporteEstados(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarEtapasExcel(FiltroReporteDTO filtro) throws IOException {
        return excel(reporteEtapas(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarActividadExcel(FiltroReporteDTO filtro) throws IOException {
        return excel(reporteActividad(filtro));
    }

    // =========================================================================
    // 2. PDF DE LOS REPORTES (tabla con resumen, en horizontal)
    // =========================================================================
    @Transactional(readOnly = true)
    public byte[] generarLotesPdf(FiltroReporteDTO filtro) throws IOException {
        return pdf(reporteLotes(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarSolicitudesPdf(FiltroReporteDTO filtro) throws IOException {
        return pdf(reporteSolicitudes(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarProyectosPdf(FiltroReporteDTO filtro) throws IOException {
        return pdf(reporteProyectos(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarEstadosComercialesPdf(FiltroReporteDTO filtro) throws IOException {
        return pdf(reporteEstados(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarEtapasPdf(FiltroReporteDTO filtro) throws IOException {
        return pdf(reporteEtapas(filtro));
    }

    @Transactional(readOnly = true)
    public byte[] generarActividadPdf(FiltroReporteDTO filtro) throws IOException {
        return pdf(reporteActividad(filtro));
    }

    // =========================================================================
    // 3. DEFINICIÓN DE CADA REPORTE
    // =========================================================================
    private Reporte<ReporteLoteItemDTO> reporteLotes(FiltroReporteDTO filtro) {
        List<ReporteLoteItemDTO> filas = reporteService.consultarReporteLotes(filtro);
        List<Columna<ReporteLoteItemDTO>> columnas = List.of(
                Columna.texto("Proyecto", 32, ReporteLoteItemDTO::getProyectoNombre),
                Columna.texto("Etapa", 18, ReporteLoteItemDTO::getEtapaNombre),
                Columna.texto("Código", 12, ReporteLoteItemDTO::getCodigo),
                Columna.texto("Nombre", 20, ReporteLoteItemDTO::getLoteNombre),
                Columna.decimal("Área (m²)", 12, ReporteLoteItemDTO::getAreaM2, true),
                Columna.texto("Estado comercial", 16, l -> Etiquetas.legible(l.getEstadoComercial())),
                Columna.siNo("Publicado", 11, ReporteLoteItemDTO::getPublicado),
                Columna.siNo("Activo", 9, ReporteLoteItemDTO::getActivo),
                Columna.fecha("Creado", 12, l -> l.getCreadoEn() == null ? null : l.getCreadoEn().toLocalDate()));

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Total de lotes", (long) filas.size());
        contarPor(filas, l -> Etiquetas.legible(l.getEstadoComercial())).forEach((estado, n) -> resumen.put("Lotes en estado " + estado.toLowerCase(CO), n));
        resumen.put("Área total (m²)", filas.stream().map(ReporteLoteItemDTO::getAreaM2).filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        resumen.put("Lotes publicados en el sitio", filas.stream().filter(l -> Boolean.TRUE.equals(l.getPublicado())).count());
        if (filtro.incluyeInactivos()) {
            resumen.put("Lotes inactivos", filas.stream().filter(l -> !Boolean.TRUE.equals(l.getActivo())).count());
        }
        return new Reporte<>("Reporte de lotes", "Lotes", describir(filtro, Campo.PROYECTO, Campo.ESTADO_LOTE, Campo.FECHAS, Campo.INACTIVOS),
                columnas, filas, resumen);
    }

    private Reporte<ReporteProyectoItemDTO> reporteProyectos(FiltroReporteDTO filtro) {
        List<ReporteProyectoItemDTO> filas = reporteService.consultarReporteProyectos(filtro);
        List<Columna<ReporteProyectoItemDTO>> columnas = List.of(
                Columna.texto("Proyecto", 28, ReporteProyectoItemDTO::getNombre),
                Columna.texto("Empresa", 26, ReporteProyectoItemDTO::getEmpresaNombre),
                Columna.texto("Etapa del proyecto", 18, p -> Etiquetas.legible(p.getEstadoProyecto())),
                Columna.siNo("Publicado", 11, ReporteProyectoItemDTO::getPublicado),
                Columna.siNo("Activo", 9, ReporteProyectoItemDTO::getActivo),
                Columna.fecha("Lanzamiento", 13, ReporteProyectoItemDTO::getFechaLanzamiento),
                Columna.entero("Lotes", 9, ReporteProyectoItemDTO::getTotalLotes, true),
                Columna.fecha("Creado", 12, p -> p.getCreadoEn() == null ? null : p.getCreadoEn().toLocalDate()));

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Total de proyectos", (long) filas.size());
        resumen.put("Publicados en el sitio", filas.stream().filter(p -> Boolean.TRUE.equals(p.getPublicado())).count());
        contarPor(filas, p -> Etiquetas.legible(p.getEstadoProyecto())).forEach((estado, n) -> resumen.put("En etapa: " + estado.toLowerCase(CO), n));
        resumen.put("Lotes activos en total", filas.stream().mapToLong(p -> p.getTotalLotes() == null ? 0 : p.getTotalLotes()).sum());
        return new Reporte<>("Reporte de proyectos", "Proyectos", describir(filtro, Campo.PROYECTO, Campo.ESTADO_PROYECTO, Campo.FECHAS, Campo.INACTIVOS),
                columnas, filas, resumen);
    }

    private Reporte<ReporteSolicitudItemDTO> reporteSolicitudes(FiltroReporteDTO filtro) {
        List<ReporteSolicitudItemDTO> filas = reporteService.consultarReporteSolicitudes(filtro);
        List<Columna<ReporteSolicitudItemDTO>> columnas = List.of(
                Columna.fechaHora("Fecha", 16, ReporteSolicitudItemDTO::getCreadoEn),
                Columna.texto("Cliente", 22, ReporteSolicitudItemDTO::getClienteNombre),
                Columna.texto("Correo", 26, ReporteSolicitudItemDTO::getClienteCorreo),
                Columna.texto("Teléfono", 14, ReporteSolicitudItemDTO::getClienteTelefono),
                Columna.texto("Proyecto", 20, ReporteSolicitudItemDTO::getProyectoNombre),
                Columna.texto("Tipología", 16, ReporteSolicitudItemDTO::getTipologiaNombre),
                Columna.texto("Lote", 10, ReporteSolicitudItemDTO::getLoteCodigo),
                Columna.texto("Mensaje", 40, ReporteSolicitudItemDTO::getMensaje),
                Columna.texto("Estado", 12, s -> Etiquetas.legible(s.getEstadoNombre())),
                Columna.texto("Atendida por", 18, ReporteSolicitudItemDTO::getAtendidaPorNombre),
                Columna.fechaHora("Fecha de atención", 16, ReporteSolicitudItemDTO::getAtendidaEn));

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Total de solicitudes", (long) filas.size());
        contarPor(filas, s -> Etiquetas.legible(s.getEstadoNombre())).forEach((estado, n) -> resumen.put("Estado: " + estado.toLowerCase(CO), n));
        resumen.put("Sin atender", filas.stream().filter(s -> "Sin Atender".equals(s.getAtendidaPorNombre())).count());
        contarPor(filas, ReporteSolicitudItemDTO::getProyectoNombre).forEach((proyecto, n) -> resumen.put("Proyecto: " + proyecto, n));
        return new Reporte<>("Reporte de solicitudes de contacto", "Solicitudes", describir(filtro, Campo.PROYECTO, Campo.ESTADO_SOLICITUD, Campo.FECHAS),
                columnas, filas, resumen);
    }

    private Reporte<EstadisticaEstadoComercialDTO> reporteEstados(FiltroReporteDTO filtro) {
        List<EstadisticaEstadoComercialDTO> filas = reporteService.consultarEstadisticasEstadosComerciales(filtro);
        List<Columna<EstadisticaEstadoComercialDTO>> columnas = List.of(
                Columna.texto("Proyecto", 30, EstadisticaEstadoComercialDTO::getProyectoNombre),
                Columna.texto("Estado comercial", 20, e -> Etiquetas.legible(e.getEstadoNombre())),
                Columna.entero("Lotes", 10, EstadisticaEstadoComercialDTO::getCantidadLotes, true),
                Columna.porcentaje("Porcentaje del proyecto", 20, EstadisticaEstadoComercialDTO::getPorcentaje));

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Lotes en total", filas.stream().mapToLong(e -> e.getCantidadLotes() == null ? 0 : e.getCantidadLotes()).sum());
        Map<String, Long> porEstado = new TreeMap<>();
        for (EstadisticaEstadoComercialDTO e : filas) {
            porEstado.merge(Etiquetas.legible(e.getEstadoNombre()), e.getCantidadLotes() == null ? 0 : e.getCantidadLotes(), Long::sum);
        }
        porEstado.forEach((estado, n) -> resumen.put("Lotes en estado " + estado.toLowerCase(CO), n));
        return new Reporte<>("Estados comerciales de los lotes", "Estados comerciales", describir(filtro, Campo.PROYECTO, Campo.INACTIVOS),
                columnas, filas, resumen);
    }

    private Reporte<ReporteEtapaItemDTO> reporteEtapas(FiltroReporteDTO filtro) {
        List<ReporteEtapaItemDTO> filas = reporteService.consultarReporteEtapas(filtro);
        List<Columna<ReporteEtapaItemDTO>> columnas = List.of(
                Columna.texto("Proyecto", 26, ReporteEtapaItemDTO::getProyectoNombre),
                Columna.texto("Etapa", 22, ReporteEtapaItemDTO::getEtapaNombre),
                Columna.entero("Total de lotes", 14, ReporteEtapaItemDTO::getTotalLotes, true),
                Columna.entero("Disponibles", 13, ReporteEtapaItemDTO::getDisponibles, true),
                Columna.entero("Reservados", 12, ReporteEtapaItemDTO::getReservados, true),
                Columna.entero("Vendidos", 11, ReporteEtapaItemDTO::getVendidos, true),
                Columna.decimal("Área total (m²)", 16, ReporteEtapaItemDTO::getAreaTotalM2, true),
                Columna.porcentaje("% vendido", 12, this::porcentajeVendido));

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Etapas", (long) filas.size());
        resumen.put("Lotes en total", filas.stream().mapToLong(ReporteEtapaItemDTO::getTotalLotes).sum());
        resumen.put("Lotes disponibles", filas.stream().mapToLong(ReporteEtapaItemDTO::getDisponibles).sum());
        resumen.put("Lotes reservados", filas.stream().mapToLong(ReporteEtapaItemDTO::getReservados).sum());
        resumen.put("Lotes vendidos", filas.stream().mapToLong(ReporteEtapaItemDTO::getVendidos).sum());
        resumen.put("Área total (m²)", filas.stream().map(ReporteEtapaItemDTO::getAreaTotalM2).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new Reporte<>("Reporte de etapas", "Etapas", describir(filtro, Campo.PROYECTO, Campo.INACTIVOS), columnas, filas, resumen);
    }

    private BigDecimal porcentajeVendido(ReporteEtapaItemDTO e) {
        if (e.getTotalLotes() == null || e.getTotalLotes() == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(e.getVendidos()).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(e.getTotalLotes()), 2, RoundingMode.HALF_UP);
    }

    private Reporte<ReporteActividadItemDTO> reporteActividad(FiltroReporteDTO filtro) {
        List<ReporteActividadItemDTO> filas = reporteService.consultarReporteActividad(filtro);
        List<Columna<ReporteActividadItemDTO>> columnas = List.of(
                Columna.fechaHora("Fecha", 16, ReporteActividadItemDTO::getCreadoEn),
                Columna.texto("Usuario", 22, ReporteActividadItemDTO::getUsuarioNombre),
                Columna.texto("Correo", 26, ReporteActividadItemDTO::getUsuarioCorreo),
                Columna.texto("Acción", 28, ReporteActividadItemDTO::getAccion),
                Columna.texto("Entidad", 16, ReporteActividadItemDTO::getEntidad),
                Columna.entero("ID entidad", 11, ReporteActividadItemDTO::getEntidadId, false),
                Columna.texto("Descripción", 50, ReporteActividadItemDTO::getDescripcion),
                Columna.texto("IP", 14, ReporteActividadItemDTO::getIp));

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("Eventos registrados", (long) filas.size());
        contarPor(filas, ReporteActividadItemDTO::getUsuarioNombre).forEach((usuario, n) -> resumen.put("Usuario: " + usuario, n));
        return new Reporte<>("Actividad administrativa", "Actividad", describir(filtro, Campo.FECHAS), columnas, filas, resumen);
    }

    // =========================================================================
    // 4. SALIDAS: EXCEL Y PDF DE UN REPORTE
    // =========================================================================
    private <T> byte[] excel(Reporte<T> r) throws IOException {
        return ExcelReporte.generar(r.titulo(), r.hoja(), r.filtros(), r.columnas(), r.filas(), r.resumen());
    }

    private <T> byte[] pdf(Reporte<T> r) throws IOException {
        try (PdfReporte pdf = new PdfReporte(true, r.titulo())) {
            pdf.portada(r.titulo(), null);
            pdf.notas(r.filtros().isEmpty() ? List.of("Sin filtros: todos los registros activos") : r.filtros());
            if (!r.resumen().isEmpty()) {
                pdf.seccion("Resumen");
                Map<String, String> texto = new LinkedHashMap<>();
                r.resumen().forEach((clave, valor) -> texto.put(clave, formatear(valor)));
                pdf.clavesValor(texto);
            }
            pdf.seccion("Detalle (" + r.filas().size() + (r.filas().size() == 1 ? " registro)" : " registros)"));
            pdf.tabla(r.columnas(), r.filas());
            return pdf.bytes();
        }
    }

    private String formatear(Object valor) {
        if (valor instanceof BigDecimal d) return decimales(d);
        if (valor instanceof Number n) return NumberFormat.getIntegerInstance(CO).format(n.longValue());
        return String.valueOf(valor);
    }

    private String decimales(BigDecimal valor) {
        NumberFormat f = NumberFormat.getNumberInstance(CO);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f.format(valor.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    private <T> Map<String, Long> contarPor(List<T> filas, Function<T, String> clave) {
        Map<String, Long> conteo = new TreeMap<>();
        for (T fila : filas) {
            String k = clave.apply(fila);
            conteo.merge(k == null || k.isBlank() ? "(sin dato)" : k, 1L, Long::sum);
        }
        return conteo;
    }

    /** Los filtros usados, en palabras: así quien lea el archivo sabe de qué conjunto de datos se trata. */
    private List<String> describir(FiltroReporteDTO f, Campo... campos) {
        List<String> lineas = new ArrayList<>();
        for (Campo campo : campos) {
            switch (campo) {
                case PROYECTO -> {
                    if (f.getProyectoId() != null) {
                        lineas.add("Proyecto: " + proyectoRepository.findById(f.getProyectoId()).map(Proyecto::getNombre).orElse("#" + f.getProyectoId()));
                    }
                }
                case ESTADO_LOTE -> {
                    if (f.getEstadoLoteId() != null) {
                        lineas.add("Estado del lote: " + estadoLoteRepository.findById(f.getEstadoLoteId())
                                .map(EstadoLote::getNombre).map(Etiquetas::legible).orElse("#" + f.getEstadoLoteId()));
                    }
                }
                case ESTADO_SOLICITUD -> {
                    if (f.getEstadoSolicitudId() != null) {
                        lineas.add("Estado de la solicitud: " + estadoSolicitudRepository.findById(f.getEstadoSolicitudId())
                                .map(EstadoSolicitud::getNombre).map(Etiquetas::legible).orElse("#" + f.getEstadoSolicitudId()));
                    }
                }
                case ESTADO_PROYECTO -> {
                    if (f.getEstadoProyecto() != null && !f.getEstadoProyecto().isBlank()) {
                        lineas.add("Etapa del proyecto: " + Etiquetas.legible(f.getEstadoProyecto()));
                    }
                }
                case FECHAS -> {
                    if (f.getFechaInicio() != null) lineas.add("Desde: " + f.getFechaInicio().format(FECHA));
                    if (f.getFechaFin() != null) lineas.add("Hasta: " + f.getFechaFin().format(FECHA));
                }
                case INACTIVOS -> {
                    lineas.add(f.incluyeInactivos() ? "Incluye registros inactivos" : "Solo registros activos");
                }
            }
        }
        return lineas;
    }

    // =========================================================================
    // WORD (.DOCX) - SIN MENCIONES MONETARIAS
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
    // 5. PDF: FICHA TÉCNICA DE UN PROYECTO
    // =========================================================================
    @Transactional(readOnly = true)
    public byte[] generarFichaTecnicaProyectoPDF(Long proyectoId) throws IOException {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + proyectoId));

        // Los datos del proyecto salen aunque esté inactivo; el inventario cuenta solo lo activo
        FiltroReporteDTO deProyecto = new FiltroReporteDTO();
        deProyecto.setProyectoId(proyectoId);
        deProyecto.setIncluirInactivos(true);
        ReporteProyectoItemDTO resumenProyecto = reporteService.consultarReporteProyectos(deProyecto).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + proyectoId));

        FiltroReporteDTO deLotes = new FiltroReporteDTO();
        deLotes.setProyectoId(proyectoId);
        List<ReporteLoteItemDTO> lotes = reporteService.consultarReporteLotes(deLotes);
        List<EstadisticaEstadoComercialDTO> estados = reporteService.consultarEstadisticasEstadosComerciales(deLotes);
        List<ReporteEtapaItemDTO> etapas = reporteService.consultarReporteEtapas(deLotes);
        Ubicacion ubicacion = ubicacionRepository.findByProyectoId(proyectoId).orElse(null);

        try (PdfReporte pdf = new PdfReporte(false, "Ficha técnica del proyecto")) {
            pdf.portada("Ficha técnica del proyecto", proyecto.getNombre());

            Map<String, String> datos = new LinkedHashMap<>();
            datos.put("Empresa", resumenProyecto.getEmpresaNombre());
            datos.put("Etapa del proyecto", Etiquetas.legible(resumenProyecto.getEstadoProyecto()));
            datos.put("Tipo de registro", proyecto.getTipoRegistro() == null ? "" : Etiquetas.legible(proyecto.getTipoRegistro().name()));
            datos.put("Fecha de lanzamiento", resumenProyecto.getFechaLanzamiento() == null ? "" : resumenProyecto.getFechaLanzamiento().format(FECHA));
            datos.put("Publicado en el sitio", Boolean.TRUE.equals(resumenProyecto.getPublicado()) ? "Sí" : "No");
            pdf.seccion("Datos generales");
            pdf.clavesValor(datos);

            if (ubicacion != null) {
                Map<String, String> lugar = new LinkedHashMap<>();
                lugar.put("Dirección", ubicacion.getDireccion());
                String zona = String.join(", ", java.util.stream.Stream.of(ubicacion.getCiudad(), ubicacion.getDepartamento())
                        .filter(s -> s != null && !s.isBlank()).toList());
                lugar.put("Ciudad y departamento", zona);
                lugar.put("Referencias", ubicacion.getReferencias());
                if (lugar.values().stream().anyMatch(v -> v != null && !v.isBlank())) {
                    pdf.seccion("Ubicación");
                    pdf.clavesValor(lugar);
                }
            }

            if (proyecto.getDescripcion() != null && !proyecto.getDescripcion().isBlank()) {
                pdf.seccion("Descripción");
                String d = proyecto.getDescripcion().trim();
                pdf.parrafo(d.length() > 1500 ? d.substring(0, 1500) + "..." : d, 10);
            }

            pdf.seccion("Resumen comercial de lotes");
            BigDecimal areaTotal = lotes.stream().map(ReporteLoteItemDTO::getAreaM2).filter(a -> a != null).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, String> kpis = new LinkedHashMap<>();
            kpis.put("Lotes activos", NumberFormat.getIntegerInstance(CO).format(lotes.size()));
            kpis.put("Área total (m²)", formatear(areaTotal));
            if (!lotes.isEmpty()) {
                kpis.put("Área promedio por lote (m²)", formatear(areaTotal.divide(BigDecimal.valueOf(lotes.size()), 2, RoundingMode.HALF_UP)));
            }
            pdf.clavesValor(kpis);
            if (!estados.isEmpty()) {
                List<String> etiquetas = new ArrayList<>();
                List<Long> cantidades = new ArrayList<>();
                List<Color> colores = new ArrayList<>();
                for (EstadisticaEstadoComercialDTO e : estados) {
                    etiquetas.add(Etiquetas.legible(e.getEstadoNombre()));
                    cantidades.add(e.getCantidadLotes());
                    colores.add(colorDeEstado(e.getEstadoNombre()));
                }
                pdf.barras(etiquetas, cantidades, colores);
            }

            if (!etapas.isEmpty()) {
                pdf.seccion("Lotes por etapa");
                pdf.tabla(List.of(
                        Columna.<ReporteEtapaItemDTO>texto("Etapa", 24, ReporteEtapaItemDTO::getEtapaNombre),
                        Columna.<ReporteEtapaItemDTO>entero("Total", 9, ReporteEtapaItemDTO::getTotalLotes, true),
                        Columna.<ReporteEtapaItemDTO>entero("Disponibles", 12, ReporteEtapaItemDTO::getDisponibles, true),
                        Columna.<ReporteEtapaItemDTO>entero("Reservados", 12, ReporteEtapaItemDTO::getReservados, true),
                        Columna.<ReporteEtapaItemDTO>entero("Vendidos", 10, ReporteEtapaItemDTO::getVendidos, true),
                        Columna.<ReporteEtapaItemDTO>decimal("Área (m²)", 14, ReporteEtapaItemDTO::getAreaTotalM2, true)), etapas);
            }

            pdf.seccion("Inventario de lotes (" + lotes.size() + ")");
            pdf.tabla(List.of(
                    Columna.<ReporteLoteItemDTO>texto("Etapa", 22, ReporteLoteItemDTO::getEtapaNombre),
                    Columna.<ReporteLoteItemDTO>texto("Código", 14, ReporteLoteItemDTO::getCodigo),
                    Columna.<ReporteLoteItemDTO>texto("Nombre", 22, ReporteLoteItemDTO::getLoteNombre),
                    Columna.<ReporteLoteItemDTO>decimal("Área (m²)", 14, ReporteLoteItemDTO::getAreaM2, true),
                    Columna.<ReporteLoteItemDTO>texto("Estado", 14, l -> Etiquetas.legible(l.getEstadoComercial()))), lotes);
            return pdf.bytes();
        }
    }

    private Color colorDeEstado(String estado) {
        String e = estado == null ? "" : estado.toUpperCase();
        if (e.equals("DISPONIBLE")) return new Color(0x2F, 0x9E, 0x63);
        if (e.equals("RESERVADO")) return new Color(0xD9, 0x95, 0x1A);
        if (e.equals("VENDIDO")) return new Color(0xC9, 0x4A, 0x4A);
        return PdfReporte.GRIS;
    }
}
