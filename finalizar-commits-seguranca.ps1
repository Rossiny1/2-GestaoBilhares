# Script para finalizar commits e garantir seguranca do projeto
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "FINALIZACAO DE COMMITS E SEGURANCA" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se estamos em um repositorio Git
if (-not (Test-Path .git)) {
    Write-Host "ERRO: Nao esta em um repositorio Git!" -ForegroundColor Red
    exit 1
}

# Verificar branch atual
$currentBranch = git branch --show-current
Write-Host "Branch atual: $currentBranch" -ForegroundColor Yellow
Write-Host ""

# 1. Adicionar os modulos importantes (core, data, sync, ui)
Write-Host "Passo 1: Adicionando modulos ao Git..." -ForegroundColor Yellow
if (Test-Path "core") {
    git add core/ 2>$null
    Write-Host "  - Modulo 'core' adicionado" -ForegroundColor Green
}
if (Test-Path "data") {
    git add data/ 2>$null
    Write-Host "  - Modulo 'data' adicionado" -ForegroundColor Green
}
if (Test-Path "sync") {
    git add sync/ 2>$null
    Write-Host "  - Modulo 'sync' adicionado" -ForegroundColor Green
}
if (Test-Path "ui") {
    git add ui/ 2>$null
    Write-Host "  - Modulo 'ui' adicionado" -ForegroundColor Green
}

# 2. Adicionar arquivos de documentacao importantes
Write-Host ""
Write-Host "Passo 2: Adicionando documentacao..." -ForegroundColor Yellow
if (Test-Path "MIGRATION_PLAN.md") {
    git add MIGRATION_PLAN.md 2>$null
    Write-Host "  - MIGRATION_PLAN.md adicionado" -ForegroundColor Green
}
if (Test-Path "COMPOSE-MIGRATION-SUMMARY.md") {
    git add COMPOSE-MIGRATION-SUMMARY.md 2>$null
    Write-Host "  - COMPOSE-MIGRATION-SUMMARY.md adicionado" -ForegroundColor Green
}

# 3. Adicionar arquivos .idea (configuracao do IDE)
Write-Host ""
Write-Host "Passo 3: Adicionando configuracao do IDE..." -ForegroundColor Yellow
if (Test-Path ".idea") {
    git add .idea/ 2>$null
    Write-Host "  - Configuracao .idea adicionada" -ForegroundColor Green
}

# 4. Verificar se ha mudancas para commitar
Write-Host ""
Write-Host "Passo 4: Verificando mudancas..." -ForegroundColor Yellow
$status = git status --porcelain
$staged = $status | Where-Object { $_ -match "^\s*[AM]" }

if ($staged) {
    Write-Host "  - Encontradas mudancas para commitar" -ForegroundColor Green
    
    # Fazer commit
    Write-Host ""
    Write-Host "Passo 5: Realizando commit..." -ForegroundColor Yellow
    $commitMessage = @"
feat: Adiciona modulos core, data, sync, ui e documentacao

- Adicionados modulos da modularizacao (core, data, sync, ui)
- Adicionada documentacao de migracao
- Adicionada configuracao do IDE
- Projeto modularizado completo
"@
    
    git commit -m $commitMessage
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  - Commit realizado com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "  - ERRO: Falha ao realizar commit!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "  - Nenhuma mudanca para commitar" -ForegroundColor Yellow
}

# 5. Verificar e corrigir branch master
Write-Host ""
Write-Host "Passo 6: Verificando branch master..." -ForegroundColor Yellow
$masterExists = git show-ref --verify --quiet refs/heads/master 2>$null

if (-not $masterExists) {
    Write-Host "  - Branch master nao existe ou esta corrompida" -ForegroundColor Yellow
    Write-Host "  - Criando branch master a partir de $currentBranch..." -ForegroundColor Yellow
    
    # Criar branch master a partir da branch atual
    git branch master $currentBranch 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  - Branch master criada com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "  - AVISO: Nao foi possivel criar branch master (pode ja existir)" -ForegroundColor Yellow
    }
} else {
    Write-Host "  - Branch master existe" -ForegroundColor Green
}

# 6. Criar tag de backup para seguranca
Write-Host ""
Write-Host "Passo 7: Criando tag de backup..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$tagName = "backup-modularizacao-$timestamp"
git tag -a $tagName -m "Backup apos modularizacao completa - $timestamp" 2>$null

if ($LASTEXITCODE -eq 0) {
    Write-Host "  - Tag de backup criada: $tagName" -ForegroundColor Green
} else {
    Write-Host "  - AVISO: Nao foi possivel criar tag (pode ja existir)" -ForegroundColor Yellow
}

# 7. Mostrar resumo final
Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "RESUMO FINAL" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""
Write-Host "Branch atual: $currentBranch" -ForegroundColor Cyan
Write-Host "Ultimos commits:" -ForegroundColor Cyan
git log --oneline -3
Write-Host ""
Write-Host "Tags de backup disponiveis:" -ForegroundColor Cyan
git tag -l "backup-*" | Select-Object -Last 5
Write-Host ""
Write-Host "Status do repositorio:" -ForegroundColor Cyan
git status --short
Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "PROJETO SEGURO E COMMITADO!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""
Write-Host "Para restaurar um backup, use:" -ForegroundColor Yellow
Write-Host "  git checkout <nome-da-tag>" -ForegroundColor White
Write-Host ""
Write-Host "Para voltar para a branch master:" -ForegroundColor Yellow
Write-Host "  git checkout master" -ForegroundColor White
Write-Host ""

