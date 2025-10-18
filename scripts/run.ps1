$ErrorActionPreference = "Stop"
.\scripts\build.ps1
Write-Host "==> Starting stack"
docker compose up -d
Write-Host "==> Services"
docker compose ps
