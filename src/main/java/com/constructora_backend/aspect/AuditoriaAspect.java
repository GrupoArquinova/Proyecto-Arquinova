package com.constructora_backend.aspect;

import com.constructora_backend.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registra en el historial de cambios cada operación marcada con {@link Auditable}: quién, qué, sobre qué fila, desde
 * qué IP y cuándo. La descripción es una frase legible ("Editó proyecto 'El Encanto' (cambió: publicado: no → sí)") que arma
 * {@link AuditoriaDescriptor}; si no se puede armar se usa la descripción fija de la anotación. Un fallo al auditar nunca
 * bloquea ni deshace la operación del usuario.
 */
@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    private final AuditoriaService auditoriaService;
    private final AuditoriaDescriptor descriptor;

    @Autowired
    public AuditoriaAspect(AuditoriaService auditoriaService,
                           @Autowired(required = false) AuditoriaDescriptor descriptor) {
        this.auditoriaService = auditoriaService;
        this.descriptor = descriptor;
    }

    @Around("@annotation(auditable)")
    public Object auditarOperacion(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        // Cómo estaba la fila antes (para decir qué cambió o cómo se llamaba lo que se borró)
        AuditoriaDescriptor.Instantanea antes = null;
        Long idPrevio = extraerIdDeArgumentos(joinPoint.getArgs());
        if (descriptor != null && idPrevio != null && descriptor.conoce(auditable.entidad())) {
            antes = descriptor.capturar(auditable.entidad(), idPrevio);
        }

        Object resultado = joinPoint.proceed();

        try {
            HttpServletRequest request = null;
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
            }

            Long entidadId = extraerEntidadId(resultado, joinPoint.getArgs());

            String descripcion = auditable.descripcion();
            if (descripcion == null || descripcion.isBlank()) {
                descripcion = "Operación " + auditable.accion() + " ejecutada en la entidad " + auditable.entidad();
            }
            if (descriptor != null && descriptor.conoce(auditable.entidad())) {
                boolean baja = auditable.accion() != null && auditable.accion().startsWith("ELIMINAR_");
                AuditoriaDescriptor.Instantanea despues = baja ? null : descriptor.capturar(auditable.entidad(), entidadId);
                descripcion = descriptor.describir(auditable.accion(), auditable.entidad(), entidadId, antes, despues, descripcion);
            }

            auditoriaService.registrarEvento(
                    auditable.accion(),
                    auditable.entidad(),
                    entidadId,
                    descripcion,
                    request
            );
        } catch (Exception e) {
            // Un fallo secundario de auditoría no bloquea la transacción principal del usuario, pero se deja constancia
            log.warn("No se pudo registrar la auditoría de {}: {}", auditable.accion(), e.getMessage());
        }

        return resultado;
    }

    /**
     * Intenta obtener el ID de la entidad afectada:
     * 1) Del cuerpo del ResponseEntity devuelto (crear/actualizar), buscando un método getId().
     * 2) Si no se encontró, del primer argumento numérico del método (típico en eliminar/actualizar,
     *    donde ese número corresponde al @PathVariable id).
     */
    private Long extraerEntidadId(Object resultado, Object[] args) {
        Long id = extraerIdDelResultado(resultado);
        if (id != null) {
            return id;
        }
        return extraerIdDeArgumentos(args);
    }

    private Long extraerIdDelResultado(Object resultado) {
        Object body = resultado;
        if (resultado instanceof ResponseEntity<?> responseEntity) {
            body = responseEntity.getBody();
        }
        if (body == null) {
            return null;
        }
        try {
            Object id = body.getClass().getMethod("getId").invoke(body);
            if (id instanceof Number n) {
                return n.longValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // El DTO no tiene getId(); se intentará con los argumentos.
        }
        return null;
    }

    private Long extraerIdDeArgumentos(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            // Long (la mayoría) o Integer/Short (estados de lote y de solicitud): el primero es el id de la ruta
            if (arg instanceof Long || arg instanceof Integer || arg instanceof Short) {
                return ((Number) arg).longValue();
            }
        }
        return null;
    }
}
