package com.constructora_backend.aspect;

import com.constructora_backend.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
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

            auditoriaService.registrarEvento(
                    auditable.accion(),
                    auditable.entidad(),
                    null,
                    descripcion,
                    request
            );
        } catch (Exception e) {
            // Un fallo secundario de auditoría no bloquea la transacción principal del usuario
        }

        return resultado;
    }
}
