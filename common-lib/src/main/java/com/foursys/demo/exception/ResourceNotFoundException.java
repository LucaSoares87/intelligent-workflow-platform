// ===== ResourceNotFoundException.java =====
package com.foursys.demo.common.exception;

/**
 * Exceção lançada quando um recurso não é encontrado (HTTP 404)
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}