$ErrorActionPreference = "Stop"
$wf = @{
  id = "onboarding-contrato-001"
  name = "Onboarding de Contrato"
  steps = @(
    @{ id="validar-dados"; type="validation"; },
    @{ id="decidir-risco"; type="ai-decision"; },
    @{ id="criar-registro"; type="connector"; connector="crm-mock" }
  )
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/workflows -ContentType "application/json" -Body $wf | Out-Null
Write-Host "Workflow criado."
$resp = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/workflows/onboarding-contrato-001/execute
$resp | ConvertTo-Json -Depth 5
