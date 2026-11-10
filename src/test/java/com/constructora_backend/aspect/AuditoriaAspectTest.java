package com.constructora_backend.aspect;

import com.constructora_backend.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaAspectTest {

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Auditable auditable;

    @InjectMocks
    private AuditoriaAspect auditoriaAspect;

    @Test
    @DisplayName("auditarOperacion debe ejecutar el método objetivo y llamar a auditoriaService.registrarEvento")
    void auditarOperacion_exito() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ResultadoSimulado");
        when(auditable.accion()).thenReturn("CREAR_PROYECTO");
        when(auditable.entidad()).thenReturn("PROYECTOS");
        when(auditable.descripcion()).thenReturn("Creación de prueba");

        Object resultado = auditoriaAspect.auditarOperacion(joinPoint, auditable);

        assertEquals("ResultadoSimulado", resultado);
        verify(auditoriaService, times(1)).registrarEvento(
                eq("CREAR_PROYECTO"),
                eq("PROYECTOS"),
                isNull(),
                eq("Creación de prueba"),
                any()
        );
    }
}
