package com.constructora_backend.exception;

/**
 * Excepción lanzada cuando un cliente excede el límite de peticiones permitidas (Rate Limiting).
 * Se traduce a un HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
