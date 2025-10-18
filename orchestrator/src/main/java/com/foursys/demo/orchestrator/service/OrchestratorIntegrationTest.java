package com.foursys.demo.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foursys.demo.common.dto.WorkflowDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests para Orchestrator com Resilience4j.
 * 
 * Testa:
 * - Orquestração básica
 * - Circuit breaker e retry
 * - Logging estruturado
 * - Observabilidade
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Orchestrator Integration Tests")
class OrchestratorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EXECUTE_ENDPOINT = "/execute";

    @Nested
    @DisplayName("Orquestração Básica")
    class BasicOrchestration {

        @Test
        @DisplayName("Deve executar workflow simples com sucesso")
        void shouldExecuteSimpleWorkflow() throws Exception {
            // Act
            MvcResult result = mockMvc.perform(
                post(EXECUTE_ENDPOINT + "/simple-wf-001")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

            // Assert
            String content = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(content, Map.class);
            
            assertThat(response).containsKey("status");
            assertThat(response).containsKey("logs");
            assertThat(response).containsKey("durationMs");
            assertThat(response).containsKey("timestamp");
        }

        @Test
        @DisplayName("Deve retornar NOT_FOUND para workflow inexistente")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(
                post(EXECUTE_ENDPOINT + "/non-existent-workflow")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("Deve incluir logs estruturados na resposta")
        void shouldIncludeStructuredLogs() throws Exception {
            // Act
            MvcResult result = mockMvc.perform(
                post(EXECUTE_ENDPOINT + "/wf-logging")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

            // Assert
            String content = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(content, Map.class);
            
            @SuppressWarnings("unchecked")
            List<String> logs = (List<String>) response.get("logs");
            assertThat(logs).isNotEmpty();
            assertThat(logs.stream().anyMatch(log -> log.contains("Iniciando")))
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Circuit Breaker e Resiliência")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Deve incluir duração em ms na resposta")
        void shouldIncludeDuration() throws Exception {
            // Act
            MvcResult result = mockMvc.perform(
                post(EXECUTE_ENDPOINT + "/wf-duration")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

            // Assert
            String content = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(content, Map.class);
            
            assertThat(response).containsKey("durationMs");
            Long duration = ((Number) response.get("durationMs")).longValue();
            assertThat(duration).isNotNegative();
        }

        @Test
        @DisplayName("Deve incluir timestamp da execução")
        void shouldIncludeTimestamp() throws Exception {
            // Act
            MvcResult result = mockMvc.perform(
                post(EXECUTE_ENDPOINT + "/wf-timestamp")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

            // Assert
            String content = result.getResponse().getContent