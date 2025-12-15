# Script para finalizar commits - apenas arquivos importantes
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "FINALIZACAO DE COMMITS" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Adicionar apenas arquivos de codigo fonte dos modulos (excluindo build/)
Write-Host "Adicionando modulos (excluindo build/)..." -ForegroundColor Yellow

# Adicionar modulos, mas excluir build/
if (Test-Path "core/src") {
    git add core/src/ 2>$null
    git add core/build.gradle.kts 2>$null
    git add core/src/main/AndroidManifest.xml 2>$null
    Write-Host "  - Modulo 'core' adicionado" -ForegroundColor Green
}

if (Test-Path "data/src") {
    git add data/src/ 2>$null
    git add data/build.gradle.kts 2>$null
    git add data/src/main/AndroidManifest.xml 2>$null
    Write-Host "  - Modulo 'data' adicionado" -ForegroundColor Green
}

if (Test-Path "sync/src") {
    git add sync/src/ 2>$null
    git add sync/build.gradle.kts 2>$null
    git add sync/src/main/AndroidManifest.xml 2>$null
    Write-Host "  - Modulo 'sync' adicionado" -ForegroundColor Green
}

if (Test-Path "ui/src") {
    git add ui/src/ 2>$null
    git add ui/build.gradle.kts 2>$null
    git add ui/src/main/AndroidManifest.xml 2>$null
    Write-Host "  - Modulo 'ui' adicionado" -ForegroundColor Green
}

# Adicionar documentacao
if (Test-Path "MIGRATION_PLAN.md") {
    git add MIGRATION_PLAN.md 2>$null
}
if (Test-Path "COMPOSE-MIGRATION-SUMMARY.md") {
    git add COMPOSE-MIGRATION-SUMMARY.md 2>$null
}

# Verificar se ha mudancas
$staged = git diff --cached --name-only
if ($staged) {
    Write-Host ""
    Write-Host "Realizando commit..." -ForegroundColor Yellow
    git commit -m "feat: Adiciona modulos core, data, sync, ui (codigo fonte)" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  - Commit realizado!" -ForegroundColor Green
    }
}

# Criar tag de backup
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
git tag -a "backup-modularizacao-$timestamp" -m "Backup apos modularizacao - $timestamp" 2>$null

Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "FINALIZADO!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
git log --oneline -3

