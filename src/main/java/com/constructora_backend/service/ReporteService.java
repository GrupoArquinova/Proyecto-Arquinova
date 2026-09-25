package com.constructora_backend.service;

import com.constructora_backend.dto.*;
import com.constructora_backend.dto.response.DashboardResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ReporteService {

    @PersistenceContext
    private EntityManager entityManager;

    // ============================================================
    // RF22 - DASHBOARD DE INDICADORES
    // ============================================================
    @Transactional(readOnly = true)
    public DashboardResponseDTO obtenerDashboard() {
        long totalProyectos = ((Number) entityManager.createQuery("SELECT COUNT(p) FROM Proyecto p WHERE p.activo = true").getSingleResult()).longValue();
        long proyectosPublicados = ((Number) entityManager.createQuery("SELECT COUNT(p) FROM Proyecto p WHERE p.activo = true AND p.publicado = true").getSingleResult()).longValue();

        long totalLotes = ((Number) entityManager.createQuery("SELECT COUNT(l) FROM Lote l WHERE l.activo = true").getSingleResult()).longValue();
        long lotesDisponibles = ((Number) entityManager.createQuery("SELECT COUNT(l) FROM Lote l WHERE l.activo = true AND UPPER(l.estado.nombre) = 'DISPONIBLE'").getSingleResult()).longValue();
        long lotesReservados = ((Number) entityManager.createQuery("SELECT COUNT(l) FROM Lote l WHERE l.activo = true AND UPPER(l.estado.nombre) = 'RESERVADO'").getSingleResult()).longValue();
        long lotesVendidos = ((Number) entityManager.createQuery("SELECT COUNT(l) FROM Lote l WHERE l.activo = true AND UPPER(l.estado.nombre) = 'VENDIDO'").getSingleResult()).longValue();

        long totalSolicitudes = ((Number) entityManager.createQuery("SELECT COUNT(s) FROM SolicitudContacto s").getSingleResult()).longValue();
        long solicitudesNuevas = ((Number) entityManager.createQuery("SELECT COUNT(s) FROM SolicitudContacto s WHERE UPPER(s.estado.nombre) = 'NUEVA'").getSingleResult()).longValue();

        @SuppressWarnings("unchecked")
        List<Object[]> rawLotesEstados = entityManager.createQuery(
                "SELECT l.estado.nombre, COUNT(l) FROM Lote l WHERE l.activo = true GROUP BY l.estado.nombre").getResultList();
        Map<String, Long> distribucionLotes = new HashMap<>();
        for (Object[] row : rawLotesEstados) {
            distribucionLotes.put((String) row[0], ((Number) row[1]).longValue());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rawSolicitudesEstados = entityManager.createQuery(
                "SELECT s.estado.nombre, COUNT(s) FROM SolicitudContacto s GROUP BY s.estado.nombre").getResultList();
        Map<String, Long> distribucionSolicitudes = new HashMap<>();
        for (Object[] row : rawSolicitudesEstados) {
            distribucionSolicitudes.put((String) row[0], ((Number) row[1]).longValue());
        }

        return DashboardResponseDTO.builder()
                .totalProyectos(totalProyectos)
                .proyectosPublicados(proyectosPublicados)
                .totalLotes(totalLotes)
                .lotesDisponibles(lotesDisponibles)
                .lotesReservados(lotesReservados)
                .lotesVendidos(lotesVendidos)
                .totalSolicitudes(totalSolicitudes)
                .solicitudesNuevas(solicitudesNuevas)
                .distribucionLotesPorEstado(distribucionLotes)
                .distribucionSolicitudesPorEstado(distribucionSolicitudes)
                .build();
    }

    // ============================================================
    // RF23 - REPORTE DE PROYECTOS
    // ============================================================
    @Transactional(readOnly = true)
    public List<ReporteProyectoItemDTO> consultarReporteProyectos(FiltroReporteDTO filtro) {
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.constructora_backend.dto.ReporteProyectoItemDTO(" +
                        "p.id, e.nombre, p.nombre, CAST(p.estadoProyecto AS string), p.publicado, p.activo, p.fechaLanzamiento, " +
                        "(SELECT COUNT(l) FROM Lote l WHERE l.etapa.proyecto.id = p.id AND l.activo = true), p.creadoEn) " +
                        "FROM Proyecto p JOIN p.empresa e WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        if (filtro.getProyectoId() != null) {
            jpql.append("AND p.id = :proyectoId ");
            params.put("proyectoId", filtro.getProyectoId());
        }
        if (filtro.getEstadoProyecto() != null && !filtro.getEstadoProyecto().isBlank()) {
            jpql.append("AND CAST(p.estadoProyecto AS string) = :estadoProyecto ");
            params.put("estadoProyecto", filtro.getEstadoProyecto().trim());
        }
        if (filtro.getFechaInicio() != null) {
            jpql.append("AND p.creadoEn >= :fechaInicio ");
            params.put("fechaInicio", filtro.getFechaInicio().atStartOfDay());
        }
        if (filtro.getFechaFin() != null) {
            jpql.append("AND p.creadoEn <= :fechaFin ");
            params.put("fechaFin", filtro.getFechaFin().atTime(23, 59, 59));
        }

        jpql.append("ORDER BY p.creadoEn DESC");

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<ReporteProyectoItemDTO> resultado = query.getResultList();
        return resultado;
    }

    // ============================================================
    // RF24 - REPORTE DE LOTES
    // ============================================================
    @Transactional(readOnly = true)
    public List<ReporteLoteItemDTO> consultarReporteLotes(FiltroReporteDTO filtro) {
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.constructora_backend.dto.ReporteLoteItemDTO(" +
                        "l.id, p.nombre, et.nombre, l.codigo, l.nombre, l.areaM2, est.nombre, l.publicado, l.activo, l.creadoEn) " +
                        "FROM Lote l " +
                        "JOIN l.etapa et " +
                        "JOIN et.proyecto p " +
                        "JOIN l.estado est " +
                        "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        if (filtro.getProyectoId() != null) {
            jpql.append("AND p.id = :proyectoId ");
            params.put("proyectoId", filtro.getProyectoId());
        }
        if (filtro.getEstadoLoteId() != null) {
            jpql.append("AND est.id = :estadoLoteId ");
            params.put("estadoLoteId", filtro.getEstadoLoteId());
        }
        if (filtro.getFechaInicio() != null) {
            jpql.append("AND l.creadoEn >= :fechaInicio ");
            params.put("fechaInicio", filtro.getFechaInicio().atStartOfDay());
        }
        if (filtro.getFechaFin() != null) {
            jpql.append("AND l.creadoEn <= :fechaFin ");
            params.put("fechaFin", filtro.getFechaFin().atTime(23, 59, 59));
        }

        jpql.append("ORDER BY p.nombre ASC, et.orden ASC, l.codigo ASC");

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<ReporteLoteItemDTO> resultado = query.getResultList();
        return resultado;
    }

    // ============================================================
    // RF25 - REPORTE DE ESTADOS COMERCIALES DE LOTES
    // ============================================================
    @Transactional(readOnly = true)
    public List<EstadisticaEstadoComercialDTO> consultarEstadisticasEstadosComerciales(FiltroReporteDTO filtro) {
        StringBuilder jpql = new StringBuilder(
                "SELECT p.id, p.nombre, est.nombre, COUNT(l) " +
                        "FROM Lote l " +
                        "JOIN l.etapa et " +
                        "JOIN et.proyecto p " +
                        "JOIN l.estado est " +
                        "WHERE l.activo = true "
        );

        Map<String, Object> params = new HashMap<>();

        if (filtro.getProyectoId() != null) {
            jpql.append("AND p.id = :proyectoId ");
            params.put("proyectoId", filtro.getProyectoId());
        }

        jpql.append("GROUP BY p.id, p.nombre, est.nombre ORDER BY p.nombre ASC, est.nombre ASC");

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<Long, Long> totalLotesPorProyecto = new HashMap<>();
        for (Object[] row : rows) {
            Long pId = (Long) row[0];
            Long cnt = ((Number) row[3]).longValue();
            totalLotesPorProyecto.put(pId, totalLotesPorProyecto.getOrDefault(pId, 0L) + cnt);
        }

        List<EstadisticaEstadoComercialDTO> estadisticas = new ArrayList<>();
        for (Object[] row : rows) {
            Long pId = (Long) row[0];
            String pNombre = (String) row[1];
            String eNombre = (String) row[2];
            Long cantidad = ((Number) row[3]).longValue();

            Long totalProyecto = totalLotesPorProyecto.getOrDefault(pId, 1L);
            BigDecimal porcentaje = BigDecimal.valueOf(cantidad)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalProyecto), 2, RoundingMode.HALF_UP);

            estadisticas.add(new EstadisticaEstadoComercialDTO(pId, pNombre, eNombre, cantidad, porcentaje));
        }

        return estadisticas;
    }

    // ============================================================
    // RF26 - REPORTE DE SOLICITUDES DE CONTACTO
    // ============================================================
    @Transactional(readOnly = true)
    public List<ReporteSolicitudItemDTO> consultarReporteSolicitudes(FiltroReporteDTO filtro) {
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.constructora_backend.dto.ReporteSolicitudItemDTO(" +
                        "s.id, COALESCE(p.nombre, 'General'), COALESCE(l.codigo, 'N/A'), s.nombre, s.correo, s.telefono, " +
                        "est.nombre, COALESCE(u.nombreCompleto, 'Sin Atender'), s.creadoEn) " +
                        "FROM SolicitudContacto s " +
                        "LEFT JOIN s.proyecto p " +
                        "LEFT JOIN s.lote l " +
                        "JOIN s.estado est " +
                        "LEFT JOIN s.atendidaPor u " +
                        "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        if (filtro.getProyectoId() != null) {
            jpql.append("AND p.id = :proyectoId ");
            params.put("proyectoId", filtro.getProyectoId());
        }
        if (filtro.getEstadoSolicitudId() != null) {
            jpql.append("AND est.id = :estadoSolicitudId ");
            params.put("estadoSolicitudId", filtro.getEstadoSolicitudId());
        }
        if (filtro.getFechaInicio() != null) {
            jpql.append("AND s.creadoEn >= :fechaInicio ");
            params.put("fechaInicio", filtro.getFechaInicio().atStartOfDay());
        }
        if (filtro.getFechaFin() != null) {
            jpql.append("AND s.creadoEn <= :fechaFin ");
            params.put("fechaFin", filtro.getFechaFin().atTime(23, 59, 59));
        }

        jpql.append("ORDER BY s.creadoEn DESC");

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<ReporteSolicitudItemDTO> resultado = query.getResultList();
        return resultado;
    }

    // ============================================================
    // RF29 - REPORTE DE ACTIVIDAD ADMINISTRATIVA
    // ============================================================
    @Transactional(readOnly = true)
    public List<ReporteActividadItemDTO> consultarReporteActividad(FiltroReporteDTO filtro) {
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.constructora_backend.dto.ReporteActividadItemDTO(" +
                        "a.id, COALESCE(u.nombreCompleto, 'Sistema'), COALESCE(u.correo, 'N/A'), a.accion, a.entidad, a.entidadId, " +
                        "a.descripcion, a.ip, a.creadoEn) " +
                        "FROM Auditoria a " +
                        "LEFT JOIN a.usuario u " +
                        "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        if (filtro.getFechaInicio() != null) {
            jpql.append("AND a.creadoEn >= :fechaInicio ");
            params.put("fechaInicio", filtro.getFechaInicio().atStartOfDay());
        }
        if (filtro.getFechaFin() != null) {
            jpql.append("AND a.creadoEn <= :fechaFin ");
            params.put("fechaFin", filtro.getFechaFin().atTime(23, 59, 59));
        }

        jpql.append("ORDER BY a.creadoEn DESC");

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<ReporteActividadItemDTO> resultado = query.getResultList();
        return resultado;
    }
}