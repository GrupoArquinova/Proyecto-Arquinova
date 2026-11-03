package com.constructora_backend.service;

import com.constructora_backend.dto.request.AuditoriaRequestDTO;
import com.constructora_backend.dto.response.AuditoriaFiltrosDTO;
import com.constructora_backend.dto.response.AuditoriaResponseDTO;
import com.constructora_backend.dto.response.PaginaDTO;
import com.constructora_backend.entity.Auditoria;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.exception.BadRequestException;
import com.constructora_backend.exception.ResourceNotFoundException;
import com.constructora_backend.mapper.AuditoriaMapper;
import com.constructora_backend.repository.AuditoriaRepository;
import com.constructora_backend.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditoriaService {

    /** Tope de filas por página del historial. */
    public static final int MAX_POR_PAGINA = 100;

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaMapper auditoriaMapper;

    @Autowired
    public AuditoriaService(AuditoriaRepository auditoriaRepository,
                            UsuarioRepository usuarioRepository,
                            AuditoriaMapper auditoriaMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaMapper = auditoriaMapper;
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponseDTO> listarTodas() {
        return auditoriaRepository.findAll()
                .stream()
                .map(auditoriaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<AuditoriaResponseDTO> obtenerPorId(Long id) {
        return auditoriaRepository.findById(id)
                .map(auditoriaMapper::toDTO);
    }

    public AuditoriaResponseDTO registrar(AuditoriaRequestDTO dto) {
        Usuario usuario = null;
        if (dto.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId()));
        }

        Auditoria auditoria = auditoriaMapper.toEntity(dto, usuario);
        Auditoria guardada = auditoriaRepository.save(auditoria);
        return auditoriaMapper.toDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponseDTO> obtenerPorEntidad(String entidad, Long entidadId) {
        return auditoriaRepository.findByEntidadAndEntidadId(entidad, entidadId)
                .stream()
                .map(auditoriaMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Historial paginado (lo más reciente primero) con filtros opcionales.
     *
     * @param usuarioId quien hizo el cambio
     * @param entidad   entidad afectada, p. ej. PROYECTOS (sin distinguir mayúsculas)
     * @param accion    acción exacta, p. ej. ACTUALIZAR_PROYECTO
     * @param texto     palabra que debe aparecer en la descripción
     * @param desde     primer día incluido
     * @param hasta     último día incluido
     * @param pagina    número de página (desde 0)
     * @param tamano    filas por página (entre 1 y {@link #MAX_POR_PAGINA})
     */
    @Transactional(readOnly = true)
    public PaginaDTO<AuditoriaResponseDTO> buscar(Long usuarioId, String entidad, String accion, String texto,
                                                  LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        int tamanoFinal = Math.min(Math.max(tamano, 1), MAX_POR_PAGINA);
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamanoFinal,
                Sort.by(Sort.Order.desc("creadoEn"), Sort.Order.desc("id")));
        Page<Auditoria> resultado = auditoriaRepository.findAll(filtro(usuarioId, entidad, accion, texto, desde, hasta), pageable);
        return PaginaDTO.de(resultado.map(auditoriaMapper::toDTO));
    }

    /** Entidades y acciones que existen en el historial, para llenar los filtros del panel. */
    @Transactional(readOnly = true)
    public AuditoriaFiltrosDTO filtrosDisponibles() {
        AuditoriaFiltrosDTO dto = new AuditoriaFiltrosDTO();
        dto.setEntidades(auditoriaRepository.entidadesRegistradas());
        dto.setAcciones(auditoriaRepository.accionesRegistradas());
        return dto;
    }

    /** Borra el historial anterior a la fecha (retención de datos); devuelve cuántos registros eran. */
    @Transactional
    public int eliminarAnterioresA(LocalDateTime limite) {
        return auditoriaRepository.eliminarAnterioresA(limite);
    }

    static Specification<Auditoria> filtro(Long usuarioId, String entidad, String accion, String texto,
                                           LocalDate desde, LocalDate hasta) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (usuarioId != null) {
                condiciones.add(cb.equal(raiz.get("usuario").get("id"), usuarioId));
            }
            if (entidad != null && !entidad.isBlank()) {
                condiciones.add(cb.equal(cb.upper(raiz.get("entidad")), entidad.trim().toUpperCase()));
            }
            if (accion != null && !accion.isBlank()) {
                condiciones.add(cb.equal(cb.upper(raiz.get("accion")), accion.trim().toUpperCase()));
            }
            if (texto != null && !texto.isBlank()) {
                // Los comodines que escriba la persona se toman como texto literal
                String literal = texto.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                condiciones.add(cb.like(cb.lower(raiz.get("descripcion")), "%" + literal + "%", '\\'));
            }
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(raiz.get("creadoEn"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThan(raiz.get("creadoEn"), hasta.plusDays(1).atStartOfDay()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    /** Texto sin saltos de línea ni caracteres de control (viene de cabeceras) y con el tope de la columna. */
    static String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.replaceAll("[\\r\\n\\p{Cntrl}]", " ").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        return limpio.length() > max ? limpio.substring(0, max) : limpio;
    }

    public AuditoriaResponseDTO registrarEvento(String accion, String entidad, Long entidadId, String descripcion, HttpServletRequest request) {
        Usuario usuario = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String correo = authentication.getName();
            usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        }

        String ip = null;
        String userAgent = null;
        if (request != null) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            } else {
                // Con varios proxies el encabezado trae una lista: la primera es el cliente
                ip = ip.split(",")[0].trim();
            }
            ip = recortar(ip, 45);
            userAgent = recortar(request.getHeader("User-Agent"), 500);
        }

        AuditoriaRequestDTO dto = new AuditoriaRequestDTO();
        dto.setAccion(accion);
        dto.setEntidad(entidad);
        dto.setEntidadId(entidadId);
        dto.setDescripcion(descripcion);
        dto.setIp(ip);
        dto.setUserAgent(userAgent);
        if (usuario != null) {
            dto.setUsuarioId(usuario.getId());
        }

        return registrar(dto);
    }
}
