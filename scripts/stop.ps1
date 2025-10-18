$ErrorActionPreference = "Stop"
Write-Host "==> Stopping stack"
docker compose down -v
