# 🏗️ Architecture Guide - Intelligent Workflow Platform

**Versão:** 0.1.0 | **Status:** Production Ready

---

## 📐 Visão Geral da Arquitetura

```
┌──────────────────────────────────────────────────────────┐
│                    CLIENTES / SISTEMAS EXTERNOS          │
└────────────────────────┬─────────────────────────────────┘
                         │
                    ┌────▼────────────────────┐
                    │   API GATEWAY (8080)    │
                    │  • Roteamento          │
                    │  • Validação           │
                    │  • OpenAPI/Swagger     │
                    └────┬─────────┬─────┬───┘
         ┌──────────────┘ │       │     └──────────────┐
         │                │       │                     │
    ┌────▼────┐  ┌────────▼───┐ ┌───▼────┐  ┌────────▼────┐
    │ WORKFLOW │  │ORCHESTRATOR│ │  AI    │  │ CONNECTORS  │
    │ ENGINE   │  │  (8082)    │ │DECISION│  │  (8084)     │
    │ (8081)   │  │            │ │(8083)  │  │             │
    └────┬─────┘  └────────────┘ └────────┘  └─────────────┘
         │              │
    ┌────▼──────────────▼─────┐
    │  DATABASE LAYER         │
    │  ┌───────────────────┐  │
    │  │  Workflows Table  │  │
    │  │  Executions Table │  │
    │  │  Audit Logs       │  │
    │  └───────────────────┘  │
    └──────────────────────────┘
         │
    ┌────▼──────────────┐
    │  OBSERVABILITY    │
    │  • Prometheus     │
    │  • Grafana        │
    │  • ELK Stack      │
    └───────────────────┘
```

---

## 🔄 Fluxo de Execução

### Fase 1: Criação
```
POST /api/workflows
    ↓
API Gateway valida DTO
    ↓
Workflow Engine persiste em BD
    ↓
Status: ACTIVE
```

### Fase 2: Execução
```
POST /api/workflows/{id}/execute
    ↓
API Gateway roteia para Orchestrator
    ↓
Orchestrator busca workflow no Engine
    ↓
Para cada Step:
    ├─ type=validation → executa inline
    ├─ type=ai-decision → chama AI Decision com @CircuitBreaker
    └─ type=connector → chama Connectors com @Retry
    ↓
Execution Entity registra resultado
    ↓
Logs e Métricas coletadas por Prometheus
    ↓
Response com status, logs e duração
```

---

## 📦 Módulos Detalhados

### 1. API Gateway (8080)

**Responsabilidades:**
- Ponto de entrada único
- Roteamento inteligente
- Validação Jakarta Bean Validation
- OpenAPI/Swagger automático
- Error handling global

**Componentes:**
```
api-gateway/
├── controller/
│   └── GatewayController.java
│       └── POST /api/workflows
│       └── GET /api/workflows/{id}
│       └── POST /api/workflows/{id}/execute
├── config/
│   └── OpenApiConfiguration.java
└── application.yml
```

**Validação:**
```java
@PostMapping("/workflows")
public ResponseEntity<?> createWorkflow(
    @Valid @RequestBody WorkflowDTO wf) {
    // @Valid ativa validação automática
    // Se falhar → GlobalExceptionHandler captura
}
```

### 2. Workflow Engine (8081)

**Responsabilidades:**
- CRUD de workflows
- Persistência via JPA/Hibernate
- Histórico de execuções
- Queries por status/data

**Arquitetura em Camadas:**

```
┌─ Controller (WorkflowController)
│  └─ REST endpoints
│
├─ Service (WorkflowService)
│  └─ Lógica de negócio
│
├─ Repository (WorkflowRepository, ExecutionRepository)
│  └─ JpaRepository com queries customizadas
│
└─ Entity (WorkflowEntity, ExecutionEntity)
   └─ Mapeamento ORM
```

**Entidades JPA:**

```java
@Entity
@Table(name = "workflows")
public class WorkflowEntity {
    @Id private String id;
    @Column private String name;
    @Column(columnDefinition = "TEXT") private String stepsJson;
    @Enumerated(EnumType.STRING) private WorkflowStatus status;
    @Version private Long optimisticLock;
    @PrePersist protected void onCreate() { ... }
}

@Entity
@Table(name = "executions")
public class ExecutionEntity {
    @Id @GeneratedValue private String id;
    @Column private String workflowId;
    @Enumerated(EnumType.STRING) private ExecutionStatus status;
    @Column(columnDefinition = "TEXT") private String logs;
    @Column private LocalDateTime startedAt;
    @Column private LocalDateTime finishedAt;
    @Column private Long durationMs;
}
```

**Queries Customizadas:**

```java
// Buscar por status com paginação
Page<WorkflowEntity> findByStatus(WorkflowStatus status, Pageable pageable);

// Query JPQL customizada
@Query("""
    SELECT e FROM ExecutionEntity e 
    WHERE e.workflowId = :workflowId 
    AND e.status = 'FAILED'
    ORDER BY e.startedAt DESC
""")
List<ExecutionEntity> findFailedExecutions(@Param("workflowId") String id);
```

### 3. Orchestrator (8082)

**Responsabilidades:**
- Orquestrar execução de steps
- Aplicar Resilience4j (Circuit Breaker + Retry)
- Chamar serviços por step type
- Montar resposta final

**Fluxo de Orquestração:**

```java
@Service
public class OrchestrationService {
    
    @CircuitBreaker(name = "orchestration")
    @Retry(name = "orchestration")
    public Map<String, Object> execute(String workflowId) {
        // 1. Buscar workflow
        WorkflowDTO wf = fetchWorkflow(workflowId);
        
        // 2. Para cada step
        for (StepDTO step : wf.steps) {
            switch (step.type) {
                case "validation":
                    executeValidation(step);
                    break;
                case "ai-decision":
                    executeAiDecision(step);  // com @CircuitBreaker
                    break;
                case "connector":
                    executeConnector(step);   // com @Retry
            }
        }
        
        // 3. Retornar resultado
        return createResponse("SUCCESS", logs, durationMs);
    }
    
    public Map<String, Object> executeWorkflowFallback(
        String workflowId, Exception ex) {
        // Chamado se circuit breaker abrir
        return createResponse("CIRCUIT_BREAKER_OPEN", 
            List.of("Serviço indisponível"), 0L);
    }
}
```

**Resilience4j Config:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      orchestration:
        slidingWindowSize: 10        # últimas 10 calls
        failureRateThreshold: 50     # 50% falhas = OPEN
        slowCallRateThreshold: 100   # 100% lentas = OPEN
        waitDurationInOpenState: 5000 # esperar 5s antes HALF_OPEN
        
      aiService:
        failureRateThreshold: 30     # mais sensível
        
  retry:
    instances:
      orchestration:
        maxAttempts: 3
        waitDuration: 1000
        intervalFunction: exponential
        exponentialRandomizationMultiplier: 2
```

### 4. AI Decision Service (8083)

**Responsabilidades:**
- Tomar decisões inteligentes
- Avaliar risco
- Integração com modelos de ML

**Mock Current:**
```java
@PostMapping("/decide")
public String decide(@RequestBody Map<String,Object> payload) {
    // 10% rejeita, 90% aprova
    return Math.random() < 0.1 ? "REPROVAR" : "APROVAR";
}
```

**Production Ready:**
```java
@Service
public class AiDecisionService {
    private final ModelLoader modelLoader;
    
    public String decide(RiskAssessmentRequest req) {
        // Integrar com modelo real (TensorFlow, PyTorch, etc)
        ModelPrediction prediction = modelLoader
            .getModel("risk-assessment-v2")
            .predict(req.toFeatureVector());
        
        return prediction.getScore() > 0.7 
            ? "APROVAR" 
            : "REPROVAR";
    }
}
```

### 5. Connectors Service (8084)

**Responsabilidades:**
- Integração com sistemas externos
- Factory pattern para conectores
- Suportar múltiplos provedores

**Arquitetura:**

```java
public interface Connector {
    String execute(Map<String, Object> payload);
}

public class SalesforceConnector implements Connector {
    @Override
    public String execute(Map<String, Object> payload) {
        SalesforceClient client = new SalesforceClient();
        return client.createLead(payload);
    }
}

public class EmailConnector implements Connector {
    @Override
    public String execute(Map<String, Object> payload) {
        SendGridClient client = new SendGridClient();
        return client.sendEmail(payload);
    }
}

@Service
public class ConnectorFactory {
    private final Map<String, Connector> connectors;
    
    public Connector getConnector(String name) {
        return connectors.getOrDefault(name, 
            new NoOpConnector());
    }
}
```

**Controller:**

```java
@PostMapping("/invoke/{name}")
public ResponseEntity<?> invoke(
    @PathVariable String name,
    @RequestBody Map<String,Object> payload) {
    
    Connector connector = factory.getConnector(name);
    String result = connector.execute(payload);
    return ResponseEntity.ok("OK:" + result);
}
```

### 6. Common Library

**Compartilhado por todos os serviços:**

```
common-lib/
├── dto/
│   ├── WorkflowDTO.java
│   ├── ExecutionResult.java
│   └── StepDTO.java
│
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── ServiceUnavailableException.java
│   ├── WorkflowExecutionException.java
│   └── GlobalExceptionHandler.java
│
└── config/
    └── ObjectMapperConfig.java
```

---

## 🛡️ Resiliência

### Circuit Breaker Pattern

**Estados:**

```
CLOSED (Normal)
  ↑ (sucesso)  ↓ (50% falhas)
HALF_OPEN ← OPEN (aguarda 5s)
  ↑            ↓
  └─ sucesso ─ testa
```

**Implementação:**

```java
@CircuitBreaker(
    name = "orchestration",
    fallbackMethod = "executeWorkflowFallback"
)
@Retry(name = "orchestration")
public Map<String, Object> execute(String workflowId) {
    // Se falhar 50% das vezes → OPEN
    // Próximas chamadas → fallback imediato
    // Após 5s → tenta 1 chamada (HALF_OPEN)
    // Se OK → volta a CLOSED
}
```

### Retry Policy

**Exponential Backoff:**

```
Tentativa 1: 0ms (imediato)
    ↓ FALHA
Tentativa 2: 1000ms + jitter aleatório
    ↓ FALHA
Tentativa 3: 2000ms + jitter aleatório
    ↓ FALHA
Falha final (depois de 3 tentativas)
```

**Configuração:**

```yaml
waitDuration: 1000                    # 1 segundo
intervalFunction: exponential         # 1s → 2s → 4s
exponentialRandomizationMultiplier: 2 # dobra a cada tentativa
maxAttempts: 3                        # máximo 3 vezes
```

---

## 📊 Persistência

### Estratégia de Banco

**Desenvolvimento (H2):**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:workflow
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
```

**Produção (PostgreSQL):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/workflow
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # Não criar automaticamente
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

### Transações

```java
// Write operation - Full transaction
@Transactional
public WorkflowEntity createWorkflow(WorkflowDTO dto) {
    // Atomicity: tudo ou nada
    // Consistency: constraints garantidos
    // Isolation: sem dirty reads
    // Durability: persistido em disco
}

// Read operation - Read-only optimization
@Transactional(readOnly = true)
public Optional<WorkflowDTO> getWorkflow(String id) {
    // Sem locks de escrita
    // Hibernate pode otimizar
}
```

### Otimistic Locking

```java
@Version
private Long optimisticLock;

// Se outro processo atualizar entre read/write:
// javax.persistence.OptimisticLockException
// → transação abortada, cliente tenta novamente
```

---

## 🔍 Observabilidade

### Prometheus Metrics

**Coletados automaticamente:**

```
# HTTP
http_requests_total{method, status, uri}
http_request_duration_seconds{method, status}

# JVM
jvm_memory_usage_bytes{area, id}
jvm_threads_peak
process_cpu_usage

# Database (HikariCP)
hikari_connections{pool, state}
hikari_connections_pending

# Spring Data
spring_data_repository_invocations_total

# Resilience4j
resilience4j_circuitbreaker_state{name}
resilience4j_circuitbreaker_calls_total{name}
resilience4j_retry_calls_total{name}
```

### Logging Estruturado

```java
@Slf4j
public class WorkflowService {
    
    public WorkflowEntity createWorkflow(WorkflowDTO dto) {
        log.info("Criando workflow: id={}, name={}", 
            dto.id, dto.name);
        
        try {
            WorkflowEntity saved = repository.save(entity);
            log.info("Workflow criado com sucesso: id={}", 
                saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Erro ao criar workflow: id={}, error={}", 
                dto.id, e.getMessage(), e);
            throw new RuntimeException("Erro ao criar", e);
        }
    }
}
```

**Configuração:**

```yaml
logging:
  level:
    root: INFO
    com.foursys.demo: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d %p %c{1.} [%t] %m%n"
  
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30
```

---

## 🧪 Testes

### Strategy

**Unit Tests (40%):**
- Serviço sem dependências
- Repository mocked
- Lógica pura

**Integration Tests (60%):**
- Com TestContainers (PostgreSQL real)
- API endpoints
- Database persistence
- Circuit breaker behavior

### TestContainers

```java
@Testcontainers
@SpringBootTest
public class WorkflowIntegrationTest extends AbstractIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("workflow_test")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired private MockMvc mockMvc;
    @Autowired private WorkflowRepository repository;
    
    @Test
    void shouldCreateAndRetrieveWorkflow() throws Exception {
        // Arrange
        WorkflowDTO dto = createTestWorkflow();
        
        // Act
        mockMvc.perform(post("/api/workflows")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(dto.id));
        
        // Assert
        assertThat(repository.findById(dto.id))
            .isPresent();
    }
}
```

---

## 🚀 Deployment

### Local Development
```powershell
# H2 em memória
.\scripts\build.ps1 -Clean
.\scripts\run.ps1
curl http://localhost:8080/actuator/health
```

### Staging (Blue/Green)
```powershell
# BLUE rodando (versão atual)
docker compose up -d

# GREEN em porta 9080 (nova versão)
docker compose -f docker-compose.green.yml up -d

# Testes em GREEN
.\scripts\simulate.ps1 -BaseUrl http://localhost:9080

# Trocar tráfego (via nginx/proxy)
# Se falha: docker compose down -v && docker compose resume
```

### Production
```powershell
# Build e push
.\scripts\build.ps1 -Profile prod
docker tag api-gateway:latest myregistry/api-gateway:0.1.0
docker push myregistry/api-gateway:0.1.0

# Deploy
docker compose -f docker-compose.prod.yml up -d

# Health checks
curl http://production-api.example.com/actuator/health/liveness
```

---

## 📈 Performance Tuning

### JVM Settings
```bash
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
```

### Database Pool
```yaml
hikari:
  maximum-pool-size: 30
  minimum-idle: 10
  connection-timeout: 30000
  idle-timeout: 600000
```

### HTTP Settings
```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    connection-timeout: 20000
```

---

## 📚 Referências

- Spring Boot 3.3.4
- Spring Data JPA
- Resilience4j 2.1.0
- TestContainers 1.19.3
- Prometheus + Grafana
- PostgreSQL 15

---

**Última atualização:** 17 de Outubro de 2024  
**Status:** Production Ready ✅