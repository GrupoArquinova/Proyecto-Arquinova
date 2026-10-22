package com.constructora_backend.exception;

/** El asistente de IA no pudo responder (sin clave, Google caído, tiempo agotado o respuesta vacía). El detalle va solo al log. */
public class AIServicioNoDisponibleException extends RuntimeException {

    public AIServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public AIServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
