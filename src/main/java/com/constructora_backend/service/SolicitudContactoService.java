package com.constructora_backend.service;

import com.constructora_backend.dto.SolicitudContactoAtencionDTO;
import com.constructora_backend.dto.SolicitudContactoPublicDTO;
import com.constructora_backend.dto.response.SolicitudContactoResponseDTO;
import com.constructora_backend.entity.*;
import com.constructora_backend.mapper.SolicitudContactoMapper;
import com.constructora_backend.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    /** Tope del user-agent y de la IP que se guardan (tamaño de las columnas). */
    static final int MAX_USER_AGENT = 500;
    static final int MAX_IP = 45;

    @Autowired
    private SolicitudContactoRepository solicitudRepository;

    @Autowired
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private CasaModeloRepository casaModeloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudContactoMapper solicitudMapper;

    @Autowired
    private EmailService emailService; // Inyección de tu servicio de correo con RabbitMQ y Circuit Breaker

    @Autowired
    private CaptchaService captchaService;

    /** Versión de la política de tratamiento de datos vigente; queda guardada con el consentimiento (APP_LEGAL_VERSION_POLITICA). */
    @Value("${app.legal.version-politica:2026-10}")
    private String versionPolitica = "2026-10";

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

    /** Alta pública sin datos de la conexión (pruebas y usos internos); el endpoint usa la variante con IP y user-agent. */
    @Transactional
    public SolicitudContactoResponseDTO crearPublica(SolicitudContactoPublicDTO dto){
        return crearPublica(dto, null, null);
    }

    /**
     * Alta desde el formulario público. La IP y el user-agent los pone el servidor (lo que traiga el cuerpo se ignora),
     * y queda guardada la versión de la política aceptada con la fecha del consentimiento.
     * Si el campo trampa (sitioWeb) viene con texto se responde igual que si todo saliera bien, sin guardar ni enviar nada.
     * La respuesta solo trae lo mínimo: es pública y no debe exponer datos de gestión interna.
     */
    @Transactional
    public SolicitudContactoResponseDTO crearPublica(SolicitudContactoPublicDTO dto, String ip, String userAgent){
        if (dto.esRobot()) {
            log.info("Solicitud pública descartada: el campo trampa vino con texto");
            return SolicitudContactoMapper.respuestaDescartada(dto);
        }

        // Captcha (Turnstile): antes de tocar la base o enviar correos. Desactivado si no hay TURNSTILE_SECRET.
        if (!captchaService.verificar(dto.getCaptchaToken(), ip)) {
            log.info("Solicitud pública rechazada: verificación de seguridad no superada");
            throw new com.constructora_backend.exception.BadRequestException("Verificación de seguridad no superada");
        }

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

        CasaModelo casaModelo = null;
        if (dto.getCasaModeloId() != null){
            casaModelo = casaModeloRepository.findById(dto.getCasaModeloId())
                    .orElseThrow(() -> new com.constructora_backend.exception.ResourceNotFoundException("Tipología no encontrada con ID: " + dto.getCasaModeloId()));
            if (proyecto != null && !casaModelo.getProyecto().getId().equals(proyecto.getId())) {
                throw new com.constructora_backend.exception.BadRequestException("La tipología no pertenece al proyecto indicado");
            }
        }

        // 1. Mapear y guardar la solicitud en la Base de Datos
        SolicitudContacto solicitud = solicitudMapper.toEntity(dto, estadoInicial, proyecto, lote, casaModelo);
        solicitud.setIp(recortar(ip, MAX_IP));
        solicitud.setUserAgent(recortar(userAgent, MAX_USER_AGENT));
        solicitud.setVersionPolitica(versionPolitica);
        solicitud.setFechaConsentimiento(LocalDateTime.now());
        SolicitudContacto guardada = solicitudRepository.save(solicitud);

        // 2. Disparar el envío de correo electrónico a la empresa de forma segura
        try {
            emailService.enviarCorreoNuevaSolicitud(
                    guardada.getNombre(),
                    guardada.getTelefono(),
                    guardada.getCorreo(),
                    proyecto != null ? proyecto.getNombre() : null,
                    describirInteres(lote, casaModelo, "es"), // el aviso es para el equipo: siempre en español
                    guardada.getServicioInteres(),
                    dto.getMensaje(),
                    guardada.getIdioma()
            );
            log.info("Notificación de correo disparada exitosamente para la solicitud ID: {}", guardada.getId());
        } catch (Exception ex) {
            // Se captura la excepción para evitar que un fallo en el servidor SMTP o broker interrumpa el registro principal en BD
            log.error("La solicitud se guardó correctamente en BD, pero ocurrió un fallo al enviar la notificación por correo: {}", ex.getMessage());
        }

        // 3. Confirmación al cliente (solo si dejó correo); un fallo aquí tampoco afecta el registro.
        // El correo no repite el mensaje del visitante: sale hacia una dirección que cualquiera pudo escribir.
        if (guardada.getCorreo() != null && !guardada.getCorreo().isBlank()) {
            try {
                emailService.enviarConfirmacionSolicitud(guardada.getNombre(), guardada.getCorreo(),
                        proyecto != null ? proyecto.getNombre() : null,
                        describirInteres(lote, casaModelo, guardada.getIdioma()),
                        null, guardada.getIdioma());
            } catch (Exception ex) {
                log.error("La solicitud se guardó, pero no se pudo disparar la confirmación al cliente: {}", ex.getMessage());
            }
        }

        return SolicitudContactoMapper.sinDatosInternos(solicitudMapper.toDTO(guardada));
    }

    /** Texto sin caracteres de control (una cabecera como el user-agent no debe llevar saltos de línea) y con tope de largo. */
    static String recortar(String texto, int max) {
        if (texto == null) return null;
        String limpio = texto.replaceAll("[\\r\\n\\p{Cntrl}]", " ").replaceAll(" {2,}", " ").trim();
        if (limpio.isEmpty()) return null;
        return limpio.length() > max ? limpio.substring(0, max) : limpio;
    }

    /** Lo que la persona consultó, para decírselo en la confirmación: "Villa Samán", "Lote LT-101" o ambos. */
    static String describirInteres(Lote lote, CasaModelo casaModelo, String idioma) {
        java.util.List<String> partes = new java.util.ArrayList<>();
        if (casaModelo != null && casaModelo.getNombre() != null) partes.add(casaModelo.getNombre());
        if (lote != null && lote.getCodigo() != null) partes.add(("en".equals(idioma) ? "Lot " : "Lote ") + lote.getCodigo());
        return partes.isEmpty() ? null : String.join(" · ", partes);
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

    /**
     * Atiende una solicitud del titular de los datos (Ley 1581): borra todas las solicitudes hechas con ese correo.
     * Devuelve cuántas se eliminaron.
     */
    @Transactional
    public int eliminarPorCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new com.constructora_backend.exception.BadRequestException("El correo es obligatorio");
        }
        return solicitudRepository.eliminarPorCorreo(correo.trim());
    }

    /** Borra las solicitudes anteriores a la fecha dada (retención de datos); devuelve cuántas eran. */
    @Transactional
    public int eliminarAnterioresA(LocalDateTime limite) {
        return solicitudRepository.eliminarAnterioresA(limite);
    }
}
