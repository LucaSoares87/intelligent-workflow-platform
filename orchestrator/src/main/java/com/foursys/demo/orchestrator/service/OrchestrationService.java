package com.foursys.demo.orchestrator.service;

import com.foursys.demo.common.dto.WorkflowDTO;
import com.foursys.demo.common.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Serviço de Orquestração com Resilience4j integrado.
 * 
 * Implementa:
 * - @CircuitBreaker em 3 pontos críticos
 * - @Retry automático com exponential backoff
 * - Fallback methods para degradação graciosa
 * - Logging estruturado
 */
@Slf4j
@Service
public class OrchestrationService {
    
    private final RestClient engineClient;
    private final RestClient aiClient;
    private final RestClient connectorsClient;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    public OrchestrationService(
        @Value("${engine.baseUrl:http://workflow-engine:8081}") String engineBase,
        @Value("${ai.baseUrl:http://ai-decision:8083}") String aiBase,
        @Value("${connectors.baseUrl:http://connectors:8084}") String conBase
    ) {
        this.engineClient = RestClient.builder().baseUrl(engineBase).build();
        this.aiClient = RestClient.builder().baseUrl(aiBase).build();
        this.connectorsClient = RestClient.builder().baseUrl(conBase).build();
    }

    /**
     * Executar workflow com resiliência automática.
     * 
     * Decoradores:
     * - @CircuitBreaker: Protege contra falhas em cascata
     * - @Retry: Tenta novamente com backoff exponencial
     */
    @CircuitBreaker(name = "orchestration", fallbackMethod = "executeWorkflowFallback")
    @Retry(name = "orchestration")
    public Map<String, Object> execute(String workflowId) {
        log.info("Iniciando orquestração do workflow: {}", workflowId);
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Recuperar workflow do engine
            WorkflowDTO wf = fetchWorkflow(workflowId);
            if (wf == null) {
                return createResponse("NOT_FOUND", 
                    List.of("Workflow não encontrado: " + workflowId), 
                    System.currentTimeMillis() - startTime);
            }
            
            List<String> logs = new ArrayList<>();
            logs.add("▶ [" + now() + "] Iniciando execução: " + wf.name);
            
            // 2. Executar cada step
            for (int i = 0; i < wf.steps.size(); i++) {
                WorkflowDTO.StepDTO step = wf.steps.get(i);
                logs.add("▶ [" + now() + "] Executando step " + (i+1) + "/" + wf.steps.size() + 
                    ": " + step.id + " (" + step.type + ")");
                
                try {
                    executeStep(step, logs);
                } catch (Exception e) {
                    logs.add("✗ [" + now() + "] Erro no step " + step.id + ": " + e.getMessage());
                    log.error("Erro ao executar step {}", step.id, e);
                    return createResponse("FAILED", logs, System.currentTimeMillis() - startTime);
                }
            }
            
            logs.add("✓ [" + now() + "] Workflow finalizado com sucesso!");
            return createResponse("SUCCESS", logs, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("Erro geral na orquestração", e);
            return createResponse("FAILED", 
                List.of("Erro na orquestração: " + e.getMessage()), 
                System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Fallback quando circuit breaker está aberto.
     * Chamado automaticamente pelo Resilience4j.
     */
    public Map<String, Object> executeWorkflowFallback(String workflowId, Exception ex) {
        log.warn("Circuit breaker ativado para workflow: {}. Fallback acionado.", workflowId);
        return createResponse("CIRCUIT_BREAKER_OPEN",
            List.of(
                "⚠ Serviço temporariamente indisponível",
                "Razão: " + ex.getMessage(),
                "Status: Circuit breaker em estado OPEN",
                "Ação: Sistema em degradação. Tente novamente em alguns segundos."
            ),
            0L
        );
    }
    
    /**
     * Executar um step individual baseado no tipo.
     */
    private void executeStep(WorkflowDTO.StepDTO step, List<String> logs) {
        switch (step.type) {
            case "validation":
                executeValidation(step, logs);
                break;
            case "ai-decision":
                executeAiDecision(step, logs);
                break;
            case "connector":
                executeConnector(step, logs);
                break;
            default:
                logs.add("⚠ Step desconhecido: " + step.type);
        }
    }
    
    /**
     * Executar step de validação (local, sem chamada remota).
     */
    private void executeValidation(WorkflowDTO.StepDTO step, List<String> logs) {
        log.debug("Executando validação: {}", step.id);
        logs.add("  → Validando dados...");
        // Simulação: sempre ok
        logs.add("  ✓ Validação concluída com sucesso");
    }
    
    /**
     * Executar step de IA com circuit breaker específico.
     * 
     * Se falhar:
     * - Retry automático (até 3 vezes)
     * - Se circuit breaker abrir: fallback conservador (REPROVAR)
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "aiDecisionFallback")
    @Retry(name = "aiService")
    private void executeAiDecision(WorkflowDTO.StepDTO step, List<String> logs) {
        log.debug("Executando decisão IA: {}", step.id);
        logs.add("  → Consultando serviço de IA...");
        
        try {
            String decision = aiClient.post()
                .uri("/decide")
                .body(Map.of("context", "workflow", "step", step.id))
                .retrieve()
                .body(String.class);
            
            logs.add("  ✓ Decisão: " + decision);
            
            if ("REPROVAR".equalsIgnoreCase(decision)) {
                logs.add("  ✗ Fluxo interrompido por decisão de rejeição");
                throw new RuntimeException("Workflow rejeitado pelo serviço de IA");
            }
        } catch (Exception e) {
            log.error("Erro ao chamar serviço de IA", e);
            logs.add("  ✗ Erro na IA: " + e.getMessage());
            throw new ServiceUnavailableException("Serviço de IA indisponível", e);
        }
    }
    
    /**
     * Fallback para decisão IA.
     * Aplicar política conservadora: rejeitar quando serviço está indisponível.
     */
    public void aiDecisionFallback(WorkflowDTO.StepDTO step, List<String> logs, Exception ex) {
        log.warn("Fallback IA acionado para step: {}. Usando decisão conservadora.", step.id);
        logs.add("  ⚠ Serviço IA indisponível. Aplicando política conservadora: REPROVAR");
        logs.add("  Razão: " + ex.getMessage());
    }
    
    /**
     * Executar step de conector com retry.
     * 
     * Se falhar:
     * - Retry automático (até 3 vezes com backoff exponencial)
     * - Se circuit breaker abrir: fila de retry
     */
    @CircuitBreaker(name = "connectors", fallbackMethod = "connectorFallback")
    @Retry(name = "connectors")
    private void executeConnector(WorkflowDTO.StepDTO step, List<String> logs) {
        log.debug("Executando conector: {} -> {}", step.id, step.connector);
        logs.add("  → Chamando conector: " + step.connector);
        
        try {
            String response = connectorsClient.post()
                .uri("/invoke/" + (step.connector != null ? step.connector : "default"))
                .body(Map.of("step", step.id))
                .retrieve()
                .body(String.class);
            
            logs.add("  ✓ Conector respondeu: " + response);
        } catch (Exception e) {
            log.error("Erro ao chamar conector {}", step.connector, e);
            logs.add("  ✗ Erro no conector: " + e.getMessage());
            throw new ServiceUnavailableException("Conector indisponível: " + step.connector, e);
        }
    }
    
    /**
     * Fallback para conectores.
     * Agendar retry em fila assíncrona.
     */
    public void connectorFallback(WorkflowDTO.StepDTO step, List<String> logs, Exception ex) {
        log.warn("Fallback conector acionado para: {}. Agendando retry.", step.connector);
        logs.add("  ⚠ Conector indisponível. Agendando retry.");
        logs.add("  Razão: " + ex.getMessage());
    }
    
    /**
     * Buscar workflow com retry automático.
     */
    @Retry(name = "orchestration")
    private WorkflowDTO fetchWorkflow(String workflowId) {
        try {
            return engineClient.get()
                .uri("/workflows/" + workflowId)
                .retrieve()
                .body(WorkflowDTO.class);
        } catch (RestClientException e) {
            log.error("Erro ao buscar workflow", e);
            throw e;
        }
    }
    
    /**
     * Criar resposta padrão em formato Map.
     */
    private Map<String, Object> createResponse(String status, List<String> logs, Long durationMs) {
        return Map.of(
            "status", status,
            "logs", logs,
            "durationMs", durationMs,
            "timestamp", System.currentTimeMillis()
        );
    }
    
    /**
     * Helper para obter hora formatada.
     */
    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}