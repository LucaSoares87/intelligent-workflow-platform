// ===== Gateway Controller com Swagger =====
package com.foursys.demo.api.gateway.controller;

import com.foursys.demo.common.dto.WorkflowDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Workflows", description = "Gerenciamento de workflows")
public class GatewayController {

    private final RestClient engineClient;
    private final RestClient orchestratorClient;

    public GatewayController(
        @Value("${engine.baseUrl:http://workflow-engine:8081}") String engineBase,
        @Value("${orchestrator.baseUrl:http://orchestrator:8082}") String orchBase
    ) {
        this.engineClient = RestClient.builder().baseUrl(engineBase).build();
        this.orchestratorClient = RestClient.builder().baseUrl(orchBase).build();
    }

    @PostMapping("/workflows")
    @Operation(
        summary = "Criar um novo workflow",
        description = "Cria um novo workflow com os steps especificados"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Workflow criado com sucesso",
            content = @Content(schema = @Schema(implementation = WorkflowDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    public ResponseEntity<?> createWorkflow(
        @Valid @RequestBody WorkflowDTO wf) {
        log.info("Criando workflow: {}", wf.id);
        try {
            return engineClient.post()
                .uri("/workflows")
                .body(wf)
                .retrieve()
                .toEntity(String.class);
        } catch (Exception e) {
            log.error("Erro ao criar workflow", e);
            return ResponseEntity.status(500)
                .body("Erro ao criar workflow: " + e.getMessage());
        }
    }

    @GetMapping("/workflows/{id}")
    @Operation(
        summary = "Recuperar workflow por ID",
        description = "Retorna os detalhes de um workflow específico"
    )
    @Parameter(name = "id", description = "ID único do workflow")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Workflow encontrado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Workflow não encontrado"
        )
    })
    public ResponseEntity<?> getWorkflow(@PathVariable String id) {
        log.info("Recuperando workflow: {}", id);
        try {
            return engineClient.get()
                .uri("/workflows/" + id)
                .retrieve()
                .toEntity(String.class);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/workflows/{id}/execute")
    @Operation(
        summary = "Executar workflow",
        description = "Inicia a execução de um workflow existente com retry automático"
    )
    @Parameter(name = "id", description = "ID do workflow a executar")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Execução iniciada com sucesso"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Workflow não encontrado"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Serviço indisponível (circuit breaker ativo)"
        )
    })
    public ResponseEntity<?> execute(@PathVariable String id) {
        log.info("Executando workflow: {}", id);
        try {
            return orchestratorClient.post()
                .uri("/execute/" + id)
                .retrieve()
                .toEntity(String.class);
        } catch (Exception e) {
            log.error("Erro ao executar workflow", e);
            return ResponseEntity.status(503)
                .body("Serviço indisponível: " + e.getMessage());
        }
    }
}