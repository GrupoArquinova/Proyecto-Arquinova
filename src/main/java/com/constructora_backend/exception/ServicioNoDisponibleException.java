package com.constructora_backend.exception;

/** Un servicio externo del que depende la petición no respondió; se contesta 503 para que el visitante reintente. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
