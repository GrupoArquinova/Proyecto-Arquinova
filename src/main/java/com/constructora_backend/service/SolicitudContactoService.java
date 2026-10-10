package com.constructora_backend.service;

import com.constructora_backend.dto.SolicitudContactoAtencionDTO;
import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.entity.*;
import com.constructora_backend.mapper.SolicitudContactoMapper;
import com.constructora_backend.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SolicitudContactoService {

    /** Estado con el que entra toda solicitud nueva (se busca por nombre, no por número). */
    private static final String ESTADO_NUEVO = "NUEVO";

    @Autowired
    private SolicitudContactoRepository solicitudRepository;

    @Autowired
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudContactoMapper solicitudMapper;

    @Autowired
    private EmailService emailService; // Inyección de tu servicio de correo con RabbitMQ y Circuit Breaker

    @Transactional(readOnly = true)
    public List<SolicitudContactoResponseDTO> listarTodas() {
        return solicitudRepository.findAllByOrderByCreadoEnDesc()
                .stream()
                .map(solicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoResponseDTO> listarPorEstado(Integer estadoId){
        return solicitudRepository.findByEstadoIdOrderByCreadoEnDesc(estadoId)
                .stream()
                .map(solicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoResponseDTO> listarPorProyecto(Long proyectoId){
        return solicitudRepository.findByProyectoIdOrderByCreadoEnDesc(proyectoId)
                .stream()
                .map(solicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<SolicitudContactoResponseDTO> obtenerPorId(Long id) {
        return solicitudRepository.findById(id)
                .map(solicitudMapper::toDTO);
    }

    @Transactional
    public SolicitudContactoResponseDTO crearPublica(SolicitudContactoPublicDTO dto){
        EstadoSolicitud estadoInicial = estadoSolicitudRepository.findFirstByNombreIgnoreCase(ESTADO_NUEVO)
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Estado de solicitud '" + ESTADO_NUEVO + "' no encontrado"));

        Proyecto proyecto = null;
        if (dto.getProyectoId() != null){
            proyecto = proyectoRepository.findById(dto.getProyectoId())
                    .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Proyecto no encontrado con ID: " + dto.getProyectoId()));
        }

        Lote lote = null;
        if (dto.getLoteId() != null){
            lote = loteRepository.findById(dto.getLoteId())
                    .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Lote no encontrado con ID: "+ dto.getLoteId()));
        }

        // 1. Mapear y guardar la solicitud en la Base de Datos
        SolicitudContacto solicitud = solicitudMapper.toEntity(dto, estadoInicial, proyecto, lote);
        SolicitudContacto guardada = solicitudRepository.save(solicitud);

        // 2. Disparar el envío de correo electrónico a la empresa de forma segura
        try {
            String nombreProyectoStr = (proyecto != null) ? proyecto.getNombre() : "No especificado / General";
            emailService.enviarCorreoNuevaSolicitud(
                    guardada.getNombre(),
                    guardada.getTelefono(),
                    guardada.getCorreo(),
                    nombreProyectoStr,
                    guardada.getServicioInteres(),
                    dto.getMensaje(),
                    guardada.getIdioma()
            );
            log.info("Notificación de correo disparada exitosamente para la solicitud ID: {}", guardada.getId());
        } catch (Exception ex) {
            // Se captura la excepción para evitar que un fallo en el servidor SMTP o broker interrumpa el registro principal en BD
            log.error("La solicitud se guardó correctamente en BD, pero ocurrió un fallo al enviar la notificación por correo: {}", ex.getMessage());
        }

        // 3. Confirmación al cliente (solo si dejó correo); un fallo aquí tampoco afecta el registro
        if (guardada.getCorreo() != null && !guardada.getCorreo().isBlank()) {
            try {
                emailService.enviarConfirmacionSolicitud(guardada.getNombre(), guardada.getCorreo(),
                        proyecto != null ? proyecto.getNombre() : null, guardada.getIdioma());
            } catch (Exception ex) {
                log.error("La solicitud se guardó, pero no se pudo disparar la confirmación al cliente: {}", ex.getMessage());
            }
        }

        return solicitudMapper.toDTO(guardada);
    }

    @Transactional
    public SolicitudContactoResponseDTO atenderSolicitud(Long id, SolicitudContactoAtencionDTO dto) {
        SolicitudContacto existente = solicitudRepository.findById(id)
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Solicitud de contacto no encontrada con ID: " + id));

        EstadoSolicitud nuevoEstado = estadoSolicitudRepository.findById(dto.getEstadoId())
                .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Estado de solicitud no encontrado con ID: " + dto.getEstadoId()));

        existente.setEstado(nuevoEstado);
        existente.setObservacionesInternas(dto.getObservacionesInternas());

        // Quien atiende es la persona con la sesión iniciada; el ID del cuerpo solo se usa si no hay sesión
        // (así nadie puede registrar la atención a nombre de otro usuario).
        Usuario quienAtiende = usuarioAutenticado();
        if (quienAtiende == null && dto.getAtendidaPorId() != null) {
            quienAtiende = usuarioRepository.findById(dto.getAtendidaPorId())
                    .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getAtendidaPorId()));
        }
        if (quienAtiende != null) {
            existente.setAtendidaPor(quienAtiende);
        }

        // Queda registrada la última atención (quién y cuándo)
        existente.setAtendidaEn(LocalDateTime.now());

        SolicitudContacto actualizada = solicitudRepository.save(existente);
        return solicitudMapper.toDTO(actualizada);
    }

    /** Usuario con la sesión iniciada en esta petición, o null si no hay sesión o no se encuentra en la base de datos. */
    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return usuarioRepository.findByCorreo(auth.getName()).orElse(null);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!solicitudRepository.existsById(id)) {
            throw new com.constructora_backend.exception.ResourceNotFoundException("Solicitud de contacto no encontrada con ID: " + id);
        }
        solicitudRepository.deleteById(id);
    }
}