/ ===== ARQUIVO 1: workflow-engine/src/main/java/com/foursys/demo/workflow/engine/domain/WorkflowStatus.java =====
package com.foursys.demo.workflow.engine.domain;

/**
 * Estados possíveis de um Workflow
 * DRAFT -> ACTIVE -> COMPLETED ou FAILED
 */
public enum WorkflowStatus {
    /** Workflow em rascunho, não ativo */
    DRAFT("Rascunho"),
    
    /** Workflow ativo e pronto para execução */
    ACTIVE("Ativo"),
    
    /** Workflow concluído com sucesso */
    COMPLETED("Concluído"),
    
    /** Workflow falhou durante a execução */
    FAILED("Falhou"),
    
    /** Workflow suspenso/pausado */
    SUSPENDED("Suspenso");

    private final String label;

    WorkflowStatus(String label) {
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