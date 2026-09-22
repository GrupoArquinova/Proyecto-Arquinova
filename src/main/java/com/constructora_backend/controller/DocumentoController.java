package com.constructora_backend.controller;

import com.constructora_backend.dto.FiltroReporteDTO;
import com.constructora_backend.dto.GenerarMinutaDTO;
import com.constructora_backend.service.ReporteDocumentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    @Autowired
    private ReporteDocumentoService documentoService;

    // =========================================================================
    // EXCEL (.XLSX) - APACHE POI
    // =========================================================================

    @GetMapping("/excel/lotes")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> descargarLotesExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarLotesExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_lotes.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/excel/solicitudes")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> descargarSolicitudesExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarSolicitudesExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_solicitudes.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // =========================================================================
    // WORD (.DOCX) - APACHE POI XWPF
    // =========================================================================

    @PostMapping("/word/promesa-compraventa")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> descargarFichaTecnicaPDF(@PathVariable Long proyectoId) throws IOException {
        byte[] data = documentoService.generarFichaTecnicaProyectoPDF(proyectoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ficha_tecnica_proyecto_" + proyectoId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/excel/proyectos")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> descargarProyectosExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarProyectosExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_proyectos.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/excel/estados-comerciales")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> descargarEstadosComercialesExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarEstadosComercialesExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_estados_comerciales.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/excel/actividad")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> descargarActividadExcel(@ModelAttribute FiltroReporteDTO filtro) throws IOException {
        byte[] data = documentoService.generarActividadExcel(filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_actividad.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}