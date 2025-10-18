$ErrorActionPreference = "Stop"
Write-Host "==> Building Maven modules"
mvn -q -DskipTests package
Write-Host "==> Building Docker images"
docker compose build
