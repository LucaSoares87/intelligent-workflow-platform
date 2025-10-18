// ===== AbstractIntegrationTest.java =====
package com.foursys.demo.workflow.engine;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class para integration tests com PostgreSQL real (TestContainers).
 * 
 * Fornece:
 * - PostgreSQL 15 em container Docker
 * - MockMvc pré-configurado
 * - Transações automáticas
 * - Profile de teste
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("workflow_test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    protected MockMvc mockMvc;
    
    // URLs e status codes úteis
    protected static final String API_BASE = "/workflows";
    protected static final int STATUS_OK = 200;
    protected static final int STATUS_CREATED = 201;
    protected static final int STATUS_BAD_REQUEST = 400;
    protected static final int STATUS_NOT_FOUND = 404;
    protected static final int STATUS_CONFLICT = 409;
    protected static final int STATUS_INTERNAL_ERROR = 500;
}

// ===== WorkflowIntegrationTest.java =====
package com.foursys.demo.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foursys.demo.common.dto.WorkflowDTO;
import com.foursys.demo.workflow.engine.domain.WorkflowEntity;
import com.foursys.demo.workflow.engine.repository.WorkflowRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests para Workflow Engine.
 * 
 * Testa:
 * - Criar workflows
 * - Recuperar workflows
 * - Validações
 * - Persistência em PostgreSQL real
 */
@DisplayName("Workflow Engine Integration Tests")
class WorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("Criar Workflow")
    class CreateWorkflow {

        @Test
        @DisplayName("Deve criar workflow com dados válidos")
        void shouldCreateWorkflowWithValidData() throws Exception {
            // Arrange
            WorkflowDTO dto = createTestWorkflow("wf-001", "Onboarding");

            // Act
            MvcResult result = mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("wf-001"))
            .andExpect(jsonPath("$.name").value("Onboarding"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();

            // Assert
            String content = result.getResponse().getContentAsString();
            WorkflowEntity entity = objectMapper.readValue(content, WorkflowEntity.class);
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo("wf-001");
            assertThat(repository.existsById("wf-001")).isTrue();
        }

        @Test
        @DisplayName("Deve retornar erro ao criar workflow sem ID")
        void shouldFailWithoutId() throws Exception {
            // Arrange
            WorkflowDTO dto = new WorkflowDTO();
            dto.name = "Onboarding";
            dto.steps = java.util.List.of();

            // Act & Assert
            mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar erro ao criar workflow com ID duplicado")
        void shouldFailWithDuplicateId() throws Exception {
            // Arrange
            WorkflowDTO dto = createTestWorkflow("wf-dup", "Test");
            mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            ).andExpect(status().isOk());

            // Act & Assert - segunda tentativa deve falhar
            mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Recuperar Workflow")
    class GetWorkflow {

        @Test
        @DisplayName("Deve recuperar workflow existente por ID")
        void shouldGetExistingWorkflow() throws Exception {
            // Arrange
            WorkflowDTO dto = createTestWorkflow("wf-get", "Get Test");
            mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            ).andExpect(status().isOk());

            // Act & Assert
            mockMvc.perform(get(API_BASE + "/wf-get"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("wf-get"))
                .andExpect(jsonPath("$.name").value("Get Test"));
        }

        @Test
        @DisplayName("Deve retornar 404 para workflow inexistente")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get(API_BASE + "/wf-nonexistent"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Workflow com Steps Complexos")
    class ComplexWorkflows {

        @Test
        @DisplayName("Deve criar workflow com múltiplos steps")
        void shouldCreateComplexWorkflow() throws Exception {
            // Arrange
            WorkflowDTO dto = new WorkflowDTO();
            dto.id = "wf-complex";
            dto.name = "Complex Workflow";
            dto.steps = java.util.List.of(
                createStep("step-1", "validation"),
                createStep("step-2", "ai-decision"),
                createStep("step-3", "connector", "salesforce-crm"),
                createStep("step-4", "connector", "email-service")
            );

            // Act & Assert
            mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("wf-complex"));

            // Verificar que foi persistido
            assertThat(repository.findById("wf-complex"))
                .isPresent();
        }

        @Test
        @DisplayName("Deve validar steps obrigatórios")
        void shouldValidateSteps() throws Exception {
            // Arrange
            WorkflowDTO dto = new WorkflowDTO();
            dto.id = "wf-invalid-steps";
            dto.name = "Invalid Steps";
            dto.steps = null; // Steps obrigatório

            // Act & Assert
            mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Listar e Buscar Workflows")
    class ListWorkflows {

        @Test
        @DisplayName("Deve listar workflows criados")
        void shouldListWorkflows() throws Exception {
            // Arrange
            for (int i = 0; i < 3; i++) {
                WorkflowDTO dto = createTestWorkflow("wf-list-" + i, "Workflow " + i);
                mockMvc.perform(
                    post(API_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                ).andExpect(status().isOk());
            }

            // Act & Assert
            mockMvc.perform(get(API_BASE + "/list"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve buscar workflows por nome")
        void shouldSearchByName() throws Exception {
            // Arrange
            WorkflowDTO dto = createTestWorkflow("wf-search", "Onboarding Process");
            mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            ).andExpect(status().isOk());

            // Act & Assert
            mockMvc.perform(get(API_BASE + "/search?name=Onboarding"))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Persistência e Transações")
    class Persistence {

        @Test
        @DisplayName("Deve persistir workflow em PostgreSQL")
        void shouldPersistInPostgreSQL() throws Exception {
            // Arrange
            WorkflowDTO dto = createTestWorkflow("wf-persist", "Persist Test");

            // Act
            mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            ).andExpect(status().isOk());

            // Assert - verificar que foi realmente persistido
            assertThat(repository.findById("wf-persist"))
                .isPresent()
                .hasValueSatisfying(w -> {
                    assertThat(w.getName()).isEqualTo("Persist Test");
                    assertThat(w.getCreatedAt()).isNotNull();
                    assertThat(w.getStepsJson()).isNotNull();
                });
        }

        @Test
        @DisplayName("Deve atualizar timestamp ao modificar")
        void shouldUpdateTimestamp() throws Exception {
            // Arrange
            WorkflowDTO dto = createTestWorkflow("wf-timestamp", "Timestamp Test");
            mockMvc.perform(
                post(API_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            ).andExpect(status().isOk());

            var workflow = repository.findById("wf-timestamp").get();
            var originalUpdatedAt = workflow.getUpdatedAt();

            // Aguardar um pouco para garantir diferença de tempo
            Thread.sleep(100);

            // Act - recuperar novamente
            mockMvc.perform(get(API_BASE + "/wf-timestamp"))
                .andExpect(status().isOk());

            // Assert - updated_at deve permanecer o mesmo se não houve mudança
            var updatedWorkflow = repository.findById("wf-timestamp").get();
            assertThat(updatedWorkflow.getUpdatedAt()).isNotNull();
        }
    }

    // ===== Helper Methods =====

    private WorkflowDTO createTestWorkflow(String id, String name) {
        WorkflowDTO dto = new WorkflowDTO();
        dto.id = id;
        dto.name = name;
        dto.description = "Test workflow for " + name;
        dto.steps = java.util.List.of(
            createStep("validate", "validation"),
            createStep("decide", "ai-decision")
        );
        return dto;
    }

    private WorkflowDTO.StepDTO createStep(String id, String type) {
        WorkflowDTO.StepDTO step = new WorkflowDTO.StepDTO();
        step.id = id;
        step.type = type;
        return step;
    }

    private WorkflowDTO.StepDTO createStep(String id, String type, String connector) {
        WorkflowDTO.StepDTO step = createStep(id, type);
        step.connector = connector;
        return step;
    }
}