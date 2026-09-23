package com.constructora_backend.controller;

import com.constructora_backend.aspect.Auditable;
import com.constructora_backend.dto.FiltroReporteDTO;
import com.constructora_backend.dto.GenerarMinutaDTO;
import com.constructora_backend.service.ReporteDocumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/documentos")
@Tag(name = "Documentos y Minutas", description = "Endpoints para la exportación de reportes a Excel (.xlsx), generación de minutas contractuales en Word (.docx) y fichas técnicas en PDF (.pdf)")
@SecurityRequirement(name = "bearerAuth")
public class DocumentoController {

    private final ReporteDocumentoService documentoService;

    @Autowired
    public DocumentoController(ReporteDocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    // =========================================================================
    // EXCEL (.XLSX) - APACHE POI
    // =========================================================================

    @GetMapping("/excel/lotes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "EXPORTAR_EXCEL_LOTES", entidad = "DOCUMENTOS", descripcion = "Exportación de reporte de lotes a archivo Excel")
    @Operation(summary = "Descargar reporte de lotes en Excel", description = "Genera y descarga un archivo Excel (.xlsx) con el listado filtrado de lotes y sus estados comerciales.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo Excel generado con éxito",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<byte[]> descargarLotesExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarLotesExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_lotes.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/excel/solicitudes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "EXPORTAR_EXCEL_SOLICITUDES", entidad = "DOCUMENTOS", descripcion = "Exportación de solicitudes de contacto a archivo Excel")
    @Operation(summary = "Descargar reporte de solicitudes en Excel", description = "Genera y descarga un archivo Excel (.xlsx) con las solicitudes de contacto y sus detalles de atención.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo Excel generado con éxito",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<byte[]> descargarSolicitudesExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarSolicitudesExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_solicitudes.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/excel/proyectos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "EXPORTAR_EXCEL_PROYECTOS", entidad = "DOCUMENTOS", descripcion = "Exportación de proyectos a archivo Excel")
    @Operation(summary = "Descargar reporte de proyectos en Excel", description = "Genera y descarga un archivo Excel (.xlsx) con la información consolidada de proyectos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo Excel generado con éxito",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<byte[]> descargarProyectosExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarProyectosExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_proyectos.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/excel/estados-comerciales")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "EXPORTAR_EXCEL_ESTADOS", entidad = "DOCUMENTOS", descripcion = "Exportación de estados comerciales a archivo Excel")
    @Operation(summary = "Descargar estadísticas de estados comerciales en Excel", description = "Genera y descarga un archivo Excel (.xlsx) con la distribución porcentual de lotes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo Excel generado con éxito",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<byte[]> descargarEstadosComercialesExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarEstadosComercialesExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_estados_comerciales.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/excel/actividad")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "EXPORTAR_EXCEL_ACTIVIDAD", entidad = "DOCUMENTOS", descripcion = "Exportación de registro de auditoría a archivo Excel")
    @Operation(summary = "Descargar registro de actividad en Excel", description = "Genera y descarga un archivo Excel (.xlsx) con los registros de auditoría administrativa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo Excel generado con éxito",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<byte[]> descargarActividadExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarActividadExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_actividad.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // =========================================================================
    // WORD (.DOCX) - APACHE POI XWPF
    // =========================================================================

    @PostMapping("/word/promesa-compraventa")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "GENERAR_WORD_PROMESA", entidad = "DOCUMENTOS", descripcion = "Generación de minuta Word de promesa de compraventa")
    @Operation(summary = "Generar minuta de promesa de compraventa en Word", description = "Genera un documento Word (.docx) formal de promesa de compraventa para un lote y comprador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Minuta Word generada con éxito",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Lote no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<byte[]> descargarPromesaCompraventaWord(@Valid @RequestBody GenerarMinutaDTO dto) throws IOException {
        byte[] data = documentoService.generarMinutaPromesaCompraventaWord(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=minuta_promesa_compraventa.docx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(data);
    }

    // =========================================================================
    // PDF (.PDF) - APACHE PDFBOX
    // =========================================================================

    @GetMapping("/pdf/proyecto/{proyectoId}/ficha-tecnica")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Auditable(accion = "GENERAR_PDF_FICHA_TECNICA", entidad = "DOCUMENTOS", descripcion = "Generación de ficha técnica PDF del proyecto")
    @Operation(summary = "Descargar ficha técnica de proyecto en PDF", description = "Genera un documento PDF formal con los datos del proyecto y el resumen de lotes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ficha técnica PDF generada con éxito",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Requiere rol ADMINISTRADOR")
    })
    public ResponseEntity<byte[]> descargarFichaTecnicaPDF(
            @Parameter(description = "ID del proyecto para generar ficha técnica", example = "1") @PathVariable Long proyectoId) throws IOException {
        byte[] data = documentoService.generarFichaTecnicaProyectoPDF(proyectoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ficha_tecnica_proyecto_" + proyectoId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}