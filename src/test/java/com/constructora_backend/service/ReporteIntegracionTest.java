package com.constructora_backend.service;

import com.constructora_backend.dto.FiltroReporteDTO;
import com.constructora_backend.dto.ReporteEtapaItemDTO;
import com.constructora_backend.dto.ReporteLoteItemDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Con la base de datos real: las consultas de los reportes (JPQL) y la generación de todos los archivos. Con repositorios
 * simulados un error en una consulta no se ve; esta prueba lo atrapa.
 */
@SpringBootTest
class ReporteIntegracionTest {

    @Autowired
    private ReporteService reportes;

    @Autowired
    private ReporteDocumentoService documentos;

    @Test
    void lasConsultasDeLosReportesFuncionanConLaBaseReal() {
        FiltroReporteDTO activos = new FiltroReporteDTO();
        FiltroReporteDTO todos = new FiltroReporteDTO();
        todos.setIncluirInactivos(true);

        List<ReporteLoteItemDTO> lotesActivos = assertDoesNotThrow(() -> reportes.consultarReporteLotes(activos));
        List<ReporteLoteItemDTO> lotesTodos = assertDoesNotThrow(() -> reportes.consultarReporteLotes(todos));
        assertTrue(lotesTodos.size() >= lotesActivos.size());
        assertTrue(lotesActivos.stream().allMatch(l -> Boolean.TRUE.equals(l.getActivo())), "por defecto solo lotes activos");

        assertDoesNotThrow(() -> reportes.consultarReporteProyectos(activos));
        assertDoesNotThrow(() -> reportes.consultarReporteSolicitudes(activos));
        assertDoesNotThrow(() -> reportes.consultarEstadisticasEstadosComerciales(activos));
        assertDoesNotThrow(() -> reportes.consultarReporteActividad(activos));

        List<ReporteEtapaItemDTO> etapas = assertDoesNotThrow(() -> reportes.consultarReporteEtapas(activos));
        for (ReporteEtapaItemDTO e : etapas) {
            assertTrue(e.getDisponibles() + e.getReservados() + e.getVendidos() <= e.getTotalLotes(), "las cuentas cuadran");
        }
        long lotesEnEtapas = etapas.stream().mapToLong(ReporteEtapaItemDTO::getTotalLotes).sum();
        assertEquals(lotesActivos.size(), lotesEnEtapas, "los lotes por etapa suman los lotes activos del reporte de lotes");
    }

    @Test
    void generaTodosLosExcelYPdfConLosDatosReales() {
        FiltroReporteDTO filtro = new FiltroReporteDTO();

        assertTrue(assertDoesNotThrow(() -> documentos.generarLotesExcel(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarProyectosExcel(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarSolicitudesExcel(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarEstadosComercialesExcel(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarEtapasExcel(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarActividadExcel(filtro)).length > 0);

        assertTrue(assertDoesNotThrow(() -> documentos.generarLotesPdf(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarProyectosPdf(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarSolicitudesPdf(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarEstadosComercialesPdf(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarEtapasPdf(filtro)).length > 0);
        assertTrue(assertDoesNotThrow(() -> documentos.generarActividadPdf(filtro)).length > 0);
    }

    @Test
    void generaLaFichaTecnicaDeCadaProyectoReal() {
        FiltroReporteDTO todos = new FiltroReporteDTO();
        todos.setIncluirInactivos(true);
        reportes.consultarReporteProyectos(todos).forEach(p ->
                assertTrue(assertDoesNotThrow(() -> documentos.generarFichaTecnicaProyectoPDF(p.getId())).length > 0,
                        "ficha del proyecto " + p.getNombre()));
    }
}
