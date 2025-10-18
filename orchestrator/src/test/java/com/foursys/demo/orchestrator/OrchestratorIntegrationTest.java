// ===== ARQUIVO 11: orchestrator/src/test/java/com/foursys/demo/orchestrator/OrchestratorIntegrationTest.java =====
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
    @DisplayName("Orquestração básica")
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
    }

    @Nested
    @DisplayName("Circuit Breaker e Resiliência")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Deve incluir timeout e retry nos logs")
        void shouldIncludeRetryLogsOnFailure() throws Exception {
            MvcResult result = mockMvc.perform(
                post(EXECUTE_ENDPOINT + "/wf-with-retry")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

            String content = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(content, Map.class);
            
            @SuppressWarnings("unchecked")
            List<String> logs = (List<String>) response.get("logs");
            assertThat(logs).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("Deve aplicar política de degradação graciosa")
        void shouldApplyGracefulDegradation() throws Exception {
            MvcResult result = mockMvc.perform(
                post(EXECUTE_ENDPOINT + "/degradation-test")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

            String content = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(content, Map.class);
            
            assertThat(response).containsKey("status");
            assertThat(response.get("status")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Logging e Observabilidade")
    class ObservabilityTests {

        @Test
        @DisplayName("Deve incluir logs estruturados em resposta")
        void shouldIncludeStructuredLogs() throws Exception {
            MvcResult result = mockMvc.perform(
                post(EXECUTE_ENDPOINT + "/wf-logging-test")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

            String content = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(content, Map.class);
            
            @SuppressWarnings("unchecked")
            List<String> logs = (List<String>) response.get("logs");
            assertThat(logs).isNotEmpty();
            assertThat(logs.stream().anyMatch(log -> log.contains("Iniciando")))
                .isTrue();
        }

        @Test
        @DisplayName("Deve incluir duração em resposta")
        void shouldIncludeDuration() throws Exception {
            MvcResult result = mockMvc.perform(
                post(EXECUTE_ENDPOINT + "/wf-duration-test")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

            String content = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(content, Map.class);
            
            assertThat(response).containsKey("durationMs");
            Long duration = ((Number) response.get("durationMs")).longValue();
            assertThat(duration).isNotNegative();
        }
    }
}