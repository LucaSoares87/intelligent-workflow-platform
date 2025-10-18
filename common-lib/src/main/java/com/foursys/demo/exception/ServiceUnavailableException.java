// ===== ServiceUnavailableException.java =====
package com.foursys.demo.common.exception;

/**
 * Exceção lançada quando um serviço está indisponível (HTTP 503)
 * Usado em circuit breaker e conexões falhadas
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}