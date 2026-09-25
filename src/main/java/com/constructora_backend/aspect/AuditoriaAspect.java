package com.constructora_backend.aspect;

import com.constructora_backend.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditoriaAspect {

    private final AuditoriaService auditoriaService;

    @Autowired
    public AuditoriaAspect(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Around("@annotation(auditable)")
    public Object auditarOperacion(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object resultado = joinPoint.proceed();

        try {
            HttpServletRequest request = null;
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
            }

            String descripcion = auditable.descripcion();
            if (descripcion == null || descripcion.isBlank()) {
                descripcion = "Operación " + auditable.accion() + " ejecutada en la entidad " + auditable.entidad();
            }

            Long entidadId = extraerEntidadId(resultado, joinPoint.getArgs());

            auditoriaService.registrarEvento(
                    auditable.accion(),
                    auditable.entidad(),
                    entidadId,
                    descripcion,
                    request
            );
        } catch (Exception e) {
            // Un fallo secundario de auditoría no bloquea la transacción principal del usuario
        }

        return resultado;
    }

    /**
     * Intenta obtener el ID de la entidad afectada:
     * 1) Del cuerpo del ResponseEntity devuelto (crear/actualizar), buscando un método getId().
     * 2) Si no se encontró, del primer argumento Long del método (típico en eliminar/actualizar,
     *    donde ese Long corresponde al @PathVariable id).
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
            if (id instanceof Long l) {
                return l;
            }
            if (id instanceof Integer i) {
                return i.longValue();
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
            if (arg instanceof Long l) {
                return l;
            }
        }
        return null;
    }
}