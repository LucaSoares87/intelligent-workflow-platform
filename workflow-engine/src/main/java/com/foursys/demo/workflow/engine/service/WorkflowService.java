// ===== ARQUIVO 7: workflow-engine/src/main/java/com/foursys/demo/workflow/engine/service/WorkflowService.java =====
package com.foursys.demo.workflow.engine.service;

import com.foursys.demo.common.dto.WorkflowDTO;
import com.foursys.demo.common.exception.ResourceNotFoundException;
import com.foursys.demo.workflow.engine.domain.WorkflowEntity;
import com.foursys.demo.workflow.engine.domain.WorkflowStatus;
import com.foursys.demo.workflow.engine.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de domínio para Workflows
 * Responsável pela lógica de negócio e persistência
 */
@Slf4j
@Service
@Transactional
public class WorkflowService {
    
    private final WorkflowRepository repository;
    private final ObjectMapper objectMapper;
    
    public WorkflowService(WorkflowRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Criar novo workflow
     */
    public WorkflowEntity createWorkflow(WorkflowDTO dto, String createdBy) {
        log.info("Criando workflow: id={}, name={}", dto.id, dto.name);
        
        if (repository.existsById(dto.id)) {
            log.warn("Workflow com ID {} já existe", dto.id);
            throw new IllegalArgumentException("Workflow com ID " + dto.id + " já existe");
        }
        
        try {
            WorkflowEntity entity = WorkflowEntity.builder()
                .id(dto.id)
                .name(dto.name)
                .description(dto.description)
                .stepsJson(objectMapper.writeValueAsString(dto.steps))
                .status(WorkflowStatus.ACTIVE)
                .version(0)
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();
            
            WorkflowEntity saved = repository.save(entity);
            log.info("Workflow criado com sucesso: id={}", saved.getId());
            return saved;
            
        } catch (Exception e) {
            log.error("Erro ao serializar steps para workflow {}", dto.id, e);
            throw new RuntimeException("Erro ao processar steps do workflow", e);
        }
    }
    
    /**
     * Recuperar workflow por ID
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowDTO> getWorkflow(String id) {
        log.debug("Recuperando workflow: id={}", id);
        return repository.findById(id).map(this::entityToDTO);
    }
    
    /**
     * Recuperar workflow com validação (lança exceção se não encontrado)
     */
    @Transactional(readOnly = true)
    public WorkflowEntity getWorkflowOrThrow(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow não encontrado: " + id));
    }
    
    /**
     * Listar todos os workflows ativos
     */
    @Transactional(readOnly = true)
    public List<WorkflowEntity> listActive() {
        return repository.findByStatus(WorkflowStatus.ACTIVE);
    }
    
    /**
     * Listar workflows com paginação e filtros
     */
    @Transactional(readOnly = true)
    public Page<WorkflowEntity> searchWorkflows(
            WorkflowStatus status,
            String nameFilter,
            Pageable pageable) {
        return repository.searchWorkflows(status, nameFilter, pageable);
    }
    
    /**
     * Atualizar status do workflow
     */
    public WorkflowEntity updateStatus(String id, WorkflowStatus newStatus, String updatedBy) {
        log.info("Atualizando status do workflow: id={}, newStatus={}", id, newStatus);
        
        WorkflowEntity entity = getWorkflowOrThrow(id);
        entity.setStatus(newStatus);
        entity.setUpdatedBy(updatedBy);
        
        return repository.save(entity);
    }
    
    /**
     * Contar workflows por status
     */
    @Transactional(readOnly = true)
    public long countByStatus(WorkflowStatus status) {
        return repository.countByStatus(status);
    }
    
    /**
     * Buscar workflows criados em um período
     */
    @Transactional(readOnly = true)
    public List<WorkflowEntity> findCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return repository.findByCreatedBetween(start, end);
    }
    
    /**
     * Converter Entity para DTO
     */
    private WorkflowDTO entityToDTO(WorkflowEntity entity) {
        try {
            WorkflowDTO dto = new WorkflowDTO();
            dto.id = entity.getId();
            dto.name = entity.getName();
            dto.description = entity.getDescription();
            dto.steps = objectMapper.readValue(
                entity.getStepsJson(),
                objectMapper.getTypeFactory().constructCollectionType(
                    java.util.List.class,
                    WorkflowDTO.StepDTO.class
                )
            );
            return dto;
        } catch (Exception e) {
            log.error("Erro ao desserializar steps do workflow {}", entity.getId(), e);
            throw new RuntimeException("Erro ao processar steps", e);
        }
    }
}