// ===== ARQUIVO 2: workflow-engine/src/main/java/com/foursys/demo/workflow/engine/domain/ExecutionStatus.java =====
package com.foursys.demo.workflow.engine.domain;

/**
 * Estados possíveis de uma Execução de Workflow
 * PENDING -> RUNNING -> SUCCESS ou FAILED
 */
public enum ExecutionStatus {
    /** Execução aguardando início */
    PENDING("Aguardando"),
    
    /** Execução em andamento */
    RUNNING("Em Execução"),
    
    /** Execução concluída com sucesso */
    SUCCESS("Sucesso"),
    
    /** Execução falhou */
    FAILED("Falhou"),
    
    /** Execução foi cancelada */
    CANCELLED("Cancelada"),
    
    /** Execução foi pausada */
    PAUSED("Pausada");

    private final String label;

    ExecutionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return name() + "(" + label + ")";
    }
}