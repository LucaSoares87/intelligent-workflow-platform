# 🔌 API Examples - Intelligent Workflow Platform

**Versão:** 0.1.0 | **Base URL:** `http://localhost:8080`

---

## 📋 Índice

1. [Criar Workflow Simples](#1-criar-workflow-simples)
2. [Recuperar Workflow](#2-recuperar-workflow)
3. [Executar Workflow](#3-executar-workflow)
4. [Workflow com Validação](#4-workflow-com-validação)
5. [Workflow com IA](#5-workflow-com-ia)
6. [Workflow com Conectores](#6-workflow-com-conectores)
7. [Workflow Complexo](#7-workflow-complexo-multi-step)
8. [Health Checks](#8-health-checks)
9. [Métricas Prometheus](#9-métricas-prometheus)
10. [Swagger/OpenAPI](#10-swaggeropenapi)
11. [Tratamento de Erros](#11-tratamento-de-erros)
12. [Circuit Breaker](#12-circuit-breaker-em-ação)
13. [Paginação](#13-busca-com-paginação)
14. [Batch Operations](#14-batch-operations)
15. [Simulação Completa](#15-simulação-end-to-end)

---

## 1. Criar Workflow Simples

Criar workflow básico com validação e decisão de IA.

### Request
```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "id": "onboarding-client-001",
    "name": "Onboarding de Cliente",
    "steps": [
      {"id": "validate-docs", "type": "validation"},
      {"id": "assess-risk", "type": "ai-decision"}
    ]
  }'
```

### Response (200 OK)
```json
{
  "id": "onboarding-client-001",
  "name": "Onboarding de Cliente",
  "status": "ACTIVE",
  "createdAt": "2024-10-17T14:30:00"
}
```

---

## 2. Recuperar Workflow

### Request
```bash
curl -X GET http://localhost:8080/api/workflows/onboarding-client-001
```

### Response (200 OK)
```json
{
  "id": "onboarding-client-001",
  "name": "Onboarding de Cliente",
  "steps": [
    {"id": "validate-docs", "type": "validation"},
    {"id": "assess-risk", "type": "ai-decision"}
  ]
}
```

---

## 3. Executar Workflow

Iniciar execução de um workflow.

### Request
```bash
curl -X POST http://localhost:8080/api/workflows/onboarding-client-001/execute
```

### Response (200 OK)
```json
{
  "status": "SUCCESS",
  "logs": [
    "▶ [14:35:22] Iniciando execução: Onboarding de Cliente",
    "▶ [14:35:22] Executando step 1/2: validate-docs (validation)",
    "  → Validando dados...",
    "  ✓ Validação concluída com sucesso",
    "▶ [14:35:23] Executando step 2/2: assess-risk (ai-decision)",
    "  → Consultando serviço de IA...",
    "  ✓ Decisão: APROVAR",
    "✓ [14:35:24] Workflow finalizado com sucesso!"
  ],
  "durationMs": 2150,
  "timestamp": 1697547000000
}
```

---

## 4. Workflow com Validação

Workflow focado em validação de dados.

### Request
```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "id": "document-validation-wf",
    "name": "Validação de Documentos",
    "steps": [
      {
        "id": "validate-cpf",
        "type": "validation"
      },
      {
        "id": "validate-identity",
        "type": "validation"
      },
      {
        "id": "check-black-list",
        "type": "ai-decision"
      }
    ]
  }'
```

### Executar
```bash
curl -X POST http://localhost:8080/api/workflows/document-validation-wf/execute
```

---

## 5. Workflow com IA

Workflow com múltiplas decisões inteligentes.

### Request
```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ai-risk-assessment-wf",
    "name": "Avaliação de Risco com IA",
    "steps": [
      {"id": "validate", "type": "validation"},
      {"id": "analyze-risk", "type": "ai-decision"},
      {"id": "assess-fraud", "type": "ai-decision"},
      {"id": "final-decision", "type": "ai-decision"}
    ]
  }'
```

---

## 6. Workflow com Conectores

Integração com serviços externos (CRM, Email, etc).

### Request
```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "id": "crm-integration-wf",
    "name": "Integração CRM",
    "steps": [
      {"id": "validate", "type": "validation"},
      {"id": "assess", "type": "ai-decision"},
      {
        "id": "sync-crm",
        "type": "connector",
        "connector": "salesforce-crm"
      },
      {
        "id": "send-email",
        "type": "connector",
        "connector": "email-service"
      },
      {
        "id": "store-data",
        "type": "connector",
        "connector": "s3-storage"
      }
    ]
  }'
```

### Executar
```bash
curl -X POST http://localhost:8080/api/workflows/crm-integration-wf/execute
```

### Response (200 OK)
```json
{
  "status": "SUCCESS",
  "logs": [
    "▶ [14:40:00] Iniciando execução: Integração CRM",
    "▶ [14:40:01] Executando step 1/5: validate (validation)",
    "  ✓ Validação concluída com sucesso",
    "▶ [14:40:02] Executando step 2/5: assess (ai-decision)",
    "  ✓ Decisão: APROVAR",
    "▶ [14:40:03] Executando step 3/5: sync-crm (connector)",
    "  → Chamando conector: salesforce-crm",
    "  ✓ Conector respondeu: OK:salesforce-crm",
    "▶ [14:40:04] Executando step 4/5: send-email (connector)",
    "  → Chamando conector: email-service",
    "  ✓ Conector respondeu: OK:email-service",
    "▶ [14:40:05] Executando step 5/5: store-data (connector)",
    "  → Chamando conector: s3-storage",
    "  ✓ Conector respondeu: OK:s3-storage",
    "✓ [14:40:06] Workflow finalizado com sucesso!"
  ],
  "durationMs": 6200,
  "timestamp": 1697547600000
}
```

---

## 7. Workflow Complexo Multi-Step

Workflow completo com validação, IA e conectores.

### Request
```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "id": "full-onboarding-enterprise",
    "name": "Onboarding Empresa - Fluxo Completo",
    "steps": [
      {"id": "validate-company", "type": "validation"},
      {"id": "validate-documents", "type": "validation"},
      {"id": "validate-contacts", "type": "validation"},
      {"id": "check-compliance", "type": "ai-decision"},
      {"id": "assess-risk", "type": "ai-decision"},
      {
        "id": "sync-erp",
        "type": "connector",
        "connector": "erp-system"
      },
      {
        "id": "sync-crm",
        "type": "connector",
        "connector": "salesforce-crm"
      },
      {
        "id": "notify-sales",
        "type": "connector",
        "connector": "email-service"
      },
      {
        "id": "store-documents",
        "type": "connector",
        "connector": "document-storage"
      }
    ]
  }'
```

---

## 8. Health Checks

Verificar saúde dos serviços.

### Liveness (está vivo?)
```bash
curl http://localhost:8080/actuator/health/liveness
```

Response:
```json
{"status": "UP"}
```

### Readiness (pronto para tráfego?)
```bash
curl http://localhost:8080/actuator/health/readiness
```

Response:
```json
{"status": "UP"}
```

### Métricas Gerais
```bash
curl http://localhost:8080/actuator/health
```

---

## 9. Métricas Prometheus

Coletar métricas em formato Prometheus.

### Endpoint
```bash
curl http://localhost:8080/actuator/prometheus
```

### Queries Úteis via Prometheus
```promql
# Taxa de requisições
rate(http_requests_total[5m])

# Workflows com sucesso
rate(workflow_executions_success[5m])

# Workflows com falha
rate(workflow_executions_failed[5m])

# Latência P95
histogram_quantile(0.95, http_request_duration_seconds)

# Circuit breaker ativo?
resilience4j_circuitbreaker_state{name="orchestration"}
```

---

## 10. Swagger/OpenAPI

Documentação interativa automática.

### Acessar
```
http://localhost:8080/swagger-ui.html
```

### JSON OpenAPI
```bash
curl http://localhost:8080/v3/api-docs
```

---

## 11. Tratamento de Erros

### Erro: Workflow não encontrado
```bash
curl -X GET http://localhost:8080/api/workflows/non-existent
```

Response (404):
```json
{
  "timestamp": "2024-10-17T14:45:00",
  "status": 404,
  "error": "Not Found",
  "message": "Workflow não encontrado: non-existent",
  "path": "/api/workflows/non-existent"
}
```

### Erro: Validação falhou
```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{"name": "Invalid"}'
```

Response (400):
```json
{
  "timestamp": "2024-10-17T14:45:00",
  "status": 400,
  "error": "Validation Error",
  "message": "Falha na validação dos campos",
  "details": {
    "id": "must not be blank",
    "steps": "must not be null"
  }
}
```

### Erro: Serviço indisponível
```bash
# Circuit breaker aberto
curl -X POST http://localhost:8080/api/workflows/any-wf/execute
```

Response (503):
```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Serviço temporariamente indisponível",
  "timestamp": "2024-10-17T14:45:00"
}
```

---

## 12. Circuit Breaker em Ação

Simular falha e recuperação.

### 1. Parar serviço IA
```powershell
docker pause intelligent-workflow-platform-ai-decision-1
```

### 2. Tentar executar workflow
```bash
curl -X POST http://localhost:8080/api/workflows/onboarding-client-001/execute
```

Response:
```json
{
  "status": "CIRCUIT_BREAKER_OPEN",
  "logs": [
    "⚠ Serviço temporariamente indisponível",
    "Status: Circuit breaker em estado OPEN",
    "Tente novamente em alguns segundos"
  ]
}
```

### 3. Retomar serviço
```powershell
docker unpause intelligent-workflow-platform-ai-decision-1
```

### 4. Tentar novamente
```bash
# Após alguns segundos, vai funcionar
curl -X POST http://localhost:8080/api/workflows/onboarding-client-001/execute
```

---

## 13. Busca com Paginação

### Listar workflows ativos
```bash
curl "http://localhost:8080/api/workflows/list?status=ACTIVE&page=0&size=10"
```

### Buscar por nome
```bash
curl "http://localhost:8080/api/workflows/search?name=onboarding&page=0&size=20"
```

Response:
```json
{
  "content": [
    {
      "id": "onboarding-client-001",
      "name": "Onboarding de Cliente",
      "status": "ACTIVE"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 14. Batch Operations

### Criar múltiplos workflows
```powershell
# Arquivo: batch-create-workflows.ps1
$workflows = @(
    @{id="wf-001"; name="Workflow 1"},
    @{id="wf-002"; name="Workflow 2"},
    @{id="wf-003"; name="Workflow 3"}
)

foreach ($wf in $workflows) {
    $body = @{
        id = $wf.id
        name = $wf.name
        steps = @(@{id="step1"; type="validation"})
    } | ConvertTo-Json
    
    curl -X POST http://localhost:8080/api/workflows `
      -H "Content-Type: application/json" `
      -d $body
}
```

### Executar batch de workflows
```powershell
$ids = @("wf-001", "wf-002", "wf-003")

foreach ($id in $ids) {
    curl -X POST "http://localhost:8080/api/workflows/$id/execute"
}
```

---

## 15. Simulação End-to-End

Script PowerShell completo:

```powershell
# scripts/full-simulation.ps1
Write-Host "🚀 Iniciando simulação completa..." -ForegroundColor Cyan

# 1. Criar workflow
$wf = @{
    id = "simulation-$(Get-Random)"
    name = "Simulation Workflow"
    steps = @(
        @{id="s1"; type="validation"},
        @{id="s2"; type="ai-decision"},
        @{id="s3"; type="connector"; connector="crm"}
    )
} | ConvertTo-Json -Depth 3

$response = curl -X POST http://localhost:8080/api/workflows `
  -H "Content-Type: application/json" `
  -d $wf | ConvertFrom-Json

Write-Host "✓ Workflow criado: $($response.id)" -ForegroundColor Green

# 2. Recuperar
$get = curl -X GET "http://localhost:8080/api/workflows/$($response.id)" | ConvertFrom-Json
Write-Host "✓ Workflow recuperado: $($get.name)" -ForegroundColor Green

# 3. Executar
$exec = curl -X POST "http://localhost:8080/api/workflows/$($response.id)/execute" | ConvertFrom-Json
Write-Host "✓ Execução: $($exec.status)" -ForegroundColor Green
Write-Host "  Duração: $($exec.durationMs)ms" -ForegroundColor Gray

# 4. Verificar métricas
$metrics = curl http://localhost:8080/actuator/prometheus | Select-String "workflow_executions"
Write-Host "✓ Métricas coletadas" -ForegroundColor Green

Write-Host ""
Write-Host "✅ Simulação concluída com sucesso!" -ForegroundColor Green
```

---

## 📊 Resumo de Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/workflows` | Criar workflow |
| GET | `/api/workflows/{id}` | Recuperar workflow |
| POST | `/api/workflows/{id}/execute` | Executar workflow |
| GET | `/api/workflows/list` | Listar workflows |
| GET | `/api/workflows/search` | Buscar workflows |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/health/liveness` | Liveness check |
| GET | `/actuator/prometheus` | Métricas Prometheus |
| GET | `/swagger-ui.html` | Documentação interativa |

---

## 🔐 Headers Recomendados

```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "User-Agent: workflow-client/1.0" \
  -H "X-Request-ID: $(uuidgen)" \
  -d '{...}'
```

---

## ⏱️ Timeouts e Limites

| Recurso | Limite |
|---------|--------|
| Execução máxima | 30 minutos |
| Steps máximos | 100 |
| Tamanho JSON | 10MB |
| Rate limit | 1000 req/min |

---

**Última atualização:** 17 de Outubro de 2024