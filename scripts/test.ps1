# ===== ARQUIVO 12: scripts/test.ps1 =====
# Script para executar testes (unit + integration)
# Uso: .\scripts\test.ps1 -TestType all|unit|integration -SkipBuild

param(
    [ValidateSet("all", "unit", "integration")]
    [string]$TestType = "all",
    [switch]$SkipBuild,
    [switch]$Coverage,
    [switch]$Verbose
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         🧪 Intelligent Workflow Platform - Test Suite         ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Build
if (-not $SkipBuild) {
    Write-Host "📦 Compilando projeto..." -ForegroundColor Yellow
    mvn -q clean compile
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Erro na compilação" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Compilação concluída" -ForegroundColor Green
}

# Unit Tests
if ($TestType -eq "unit" -or $TestType -eq "all") {
    Write-Host ""
    Write-Host "🔬 Executando testes unitários..." -ForegroundColor Yellow
    $params = @("test", "-Dgroups=unit")
    if ($Coverage) { $params += "-Dargline=`"-javaagent:target/jacoco-agent.jar`"" }
    
    mvn @params
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Testes unitários falharam" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Testes unitários passaram" -ForegroundColor Green
}

# Integration Tests
if ($TestType -eq "integration" -or $TestType -eq "all") {
    Write-Host ""
    Write-Host "🔗 Executando testes de integração..." -ForegroundColor Yellow
    Write-Host "   (Isso pode levar 2-3 minutos)" -ForegroundColor Gray
    
    $params = @("verify", "-Dgroups=integration")
    if ($Verbose) { $params += "-X" }
    
    mvn @params
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Testes de integração falharam" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Testes de integração passaram" -ForegroundColor Green
}

# Coverage Report
if ($Coverage) {
    Write-Host ""
    Write-Host "📊 Relatório de cobertura:" -ForegroundColor Yellow
    Write-Host "   $projectRoot\target\site\jacoco\index.html" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║                    ✓ TODOS OS TESTES PASSARAM!                ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Green