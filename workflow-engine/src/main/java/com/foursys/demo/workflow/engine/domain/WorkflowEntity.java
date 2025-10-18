// ===== WorkflowEntity.java (JPA) =====
package com.foursys.demo.workflow.engine.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflows")
public class WorkflowEntity {
    @Id
    private String id;
    
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String stepsJson; // Armazena steps como JSON
    
    @Enumerated(EnumType.STRING)
    private WorkflowStatus status; // DRAFT, ACTIVE, COMPLETED, FAILED
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters/Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String stepsJson) { this.stepsJson = stepsJson; }
    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

enum WorkflowStatus {
    DRAFT, ACTIVE, COMPLETED, FAILED
}

// ===== ExecutionEntity.java =====
package com.foursys.demo.workflow.engine.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "executions")
public class ExecutionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String workflowId;
    
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String logs;
    
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    
    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }
    
    // Getters/Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }
    public String getLogs() { return logs; }
    public void setLogs(String logs) { this.logs = logs; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}

enum ExecutionStatus {
    RUNNING, SUCCESS, FAILED
}

// ===== WorkflowRepository.java =====
package com.foursys.demo.workflow.engine.repository;

import com.foursys.demo.workflow.engine.domain.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowRepository extends JpaRepository<WorkflowEntity, String> {
    List<WorkflowEntity> findByName(String name);
}

public interface ExecutionRepository extends JpaRepository<ExecutionEntity, String> {
    List<ExecutionEntity> findByWorkflowId(String workflowId);
}

// ===== WorkflowService.java =====
package com.foursys.demo.workflow.engine.service;

import com.foursys.demo.common.dto.WorkflowDTO;
import com.foursys.demo.workflow.engine.domain.WorkflowEntity;
import com.foursys.demo.workflow.engine.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
public class WorkflowService {
    private final WorkflowRepository repository;
    private final ObjectMapper mapper;
    
    public WorkflowService(WorkflowRepository repository) {
        this.repository = repository;
        this.mapper = new ObjectMapper();
    }
    
    public WorkflowEntity createWorkflow(WorkflowDTO dto) {
        log.info("Criando workflow: {}", dto.id);
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(dto.id);
        entity.setName(dto.name);
        try {
            entity.setStepsJson(mapper.writeValueAsString(dto.steps));
        } catch (Exception e) {
            log.error("Erro ao serializar steps", e);
            throw new RuntimeException("Erro ao processar steps", e);
        }
        entity.setStatus(WorkflowStatus.ACTIVE);
        return repository.save(entity);
    }
    
    public Optional<WorkflowDTO> getWorkflow(String id) {
        log.info("Recuperando workflow: {}", id);
        return repository.findById(id).map(entity -> {
            WorkflowDTO dto = new WorkflowDTO();
            dto.id = entity.getId();
            dto.name = entity.getName();
            try {
                dto.steps = mapper.readValue(
                    entity.getStepsJson(),
                    mapper.getTypeFactory().constructCollectionType(
                        java.util.List.class,
                        WorkflowDTO.StepDTO.class
                    )
                );
            } catch (Exception e) {
                log.error("Erro ao desserializar steps", e);
            }
            return dto;
        });
    }
}

// ===== WorkflowController.java (ATUALIZADO) =====
package com.foursys.demo.workflow.engine.controller;

import com.foursys.demo.common.dto.WorkflowDTO;
import com.foursys.demo.workflow.engine.service.WorkflowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/workflows")
@Tag(name = "Workflow Engine", description = "Gerenciamento de workflows")
public class WorkflowController {
    private final WorkflowService service;
    
    public WorkflowController(WorkflowService service) {
        this.service = service;
    }
    
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody WorkflowDTO dto) {
        log.info("POST /workflows - Criando workflow: {}", dto.id);
        try {
            var entity = service.createWorkflow(dto);
            return ResponseEntity.ok(entity);
        } catch (Exception e) {
            log.error("Erro ao criar workflow", e);
            return ResponseEntity.status(500).body("Erro ao criar workflow");
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        log.info("GET /workflows/{} - Recuperando workflow", id);
        var wf = service.getWorkflow(id);
        return wf.map(ResponseEntity::ok)
                 .orElseGet(() -> ResponseEntity.notFound().build());
    }
}