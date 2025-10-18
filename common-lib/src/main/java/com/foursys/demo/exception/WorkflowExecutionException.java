/ ===== WorkflowExecutionException.java =====
package com.foursys.demo.common.exception;

/**
 * Exceção lançada durante a execução de um workflow
 * Erro específico da lógica de negócio
 */
public class WorkflowExecutionException extends RuntimeException {
    public WorkflowExecutionException(String message) {
        super(message);
    }

    public WorkflowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
