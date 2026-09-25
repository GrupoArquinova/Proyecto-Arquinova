package com.constructora_backend.controller;

import com.constructora_backend.dto.FiltroReporteDTO;
import com.constructora_backend.dto.GenerarMinutaDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.ReporteDocumentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentoController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReporteDocumentoService documentoService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("GET /api/documentos/excel/lotes - Descarga archivo Excel de lotes")
    @WithMockUser(roles = "ADMINISTRADOR")
    void descargarLotesExcel_Exitoso() throws Exception {
        byte[] mockBytes = "EXCEL_CONTENT".getBytes();
        when(documentoService.generarLotesExcel(any(FiltroReporteDTO.class))).thenReturn(mockBytes);

        mockMvc.perform(get("/api/documentos/excel/lotes"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=reporte_lotes.xlsx"))
                .andExpect(content().bytes(mockBytes));
    }

    @Test
    @DisplayName("GET /api/documentos/excel/solicitudes - Descarga archivo Excel de solicitudes")
    @WithMockUser(roles = "ADMINISTRADOR")
    void descargarSolicitudesExcel_Exitoso() throws Exception {
        byte[] mockBytes = "EXCEL_SOLICITUDES".getBytes();
        when(documentoService.generarSolicitudesExcel(any(FiltroReporteDTO.class))).thenReturn(mockBytes);

        mockMvc.perform(get("/api/documentos/excel/solicitudes"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=reporte_solicitudes.xlsx"))
                .andExpect(content().bytes(mockBytes));
    }

    @Test
    @DisplayName("POST /api/documentos/word/promesa-compraventa - Genera minuta Word")
    @WithMockUser(roles = "ADMINISTRADOR")
    void descargarPromesaCompraventaWord_Exitoso() throws Exception {
        GenerarMinutaDTO dto = new GenerarMinutaDTO();
        dto.setLoteId(1L);
        dto.setCompradorNombre("Juan Pérez");
        dto.setCompradorCedula("12345678");

        byte[] mockBytes = "WORD_DOCUMENT".getBytes();
        when(documentoService.generarMinutaPromesaCompraventaWord(any(GenerarMinutaDTO.class))).thenReturn(mockBytes);

        mockMvc.perform(post("/api/documentos/word/promesa-compraventa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=minuta_promesa_compraventa.docx"))
                .andExpect(content().bytes(mockBytes));
    }

    @Test
    @DisplayName("GET /api/documentos/pdf/proyecto/{id}/ficha-tecnica - Genera ficha PDF")
    @WithMockUser(roles = "ADMINISTRADOR")
    void descargarFichaTecnicaPDF_Exitoso() throws Exception {
        byte[] mockBytes = "%PDF-1.4".getBytes();
        when(documentoService.generarFichaTecnicaProyectoPDF(eq(1L))).thenReturn(mockBytes);

        mockMvc.perform(get("/api/documentos/pdf/proyecto/1/ficha-tecnica"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=ficha_tecnica_proyecto_1.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(mockBytes));
    }
}
