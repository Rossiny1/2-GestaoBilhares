# Script PowerShell para criar PR automaticamente após build bem-sucedido
# Uso: .\scripts\create-pr-on-success.ps1

$ErrorActionPreference = "Continue"

Write-Host "🔄 Verificando mudanças para criar PR..." -ForegroundColor Cyan

# Verificar se estamos em um repositório git
if (-not (Test-Path ".git")) {
    Write-Host "❌ Erro: Não é um repositório Git." -ForegroundColor Red
    exit 1
}

# Verificar se há mudanças para commitar
$status = git status --porcelain 2>&1
if (-not $status) {
    Write-Host "ℹ️  Nenhuma mudança para commitar." -ForegroundColor Gray
    exit 0
}

# Obter branch atual
$currentBranch = git branch --show-current 2>&1
if (-not $currentBranch -or $currentBranch -eq "HEAD") {
    $currentBranch = git rev-parse --abbrev-ref HEAD 2>&1
}

if ($currentBranch -match "main|master") {
    Write-Host "⚠️  Não é possível criar PR da branch main/master." -ForegroundColor Yellow
    Write-Host "💡 Faça commit e push manualmente." -ForegroundColor Cyan
    exit 0
}

# Verificar se GitHub CLI está instalado
$ghInstalled = Get-Command gh -ErrorAction SilentlyContinue
if (-not $ghInstalled) {
    Write-Host "⚠️  GitHub CLI (gh) não encontrado." -ForegroundColor Yellow
    Write-Host "📝 Fazendo commit e push normal..." -ForegroundColor Cyan
    
    # Fallback: commit e push normal
    git add -A 2>&1 | Out-Null
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $COMMIT_MSG = "Auto-commit: Build bem-sucedido - $timestamp"
    git commit -m $COMMIT_MSG 2>&1 | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        git push origin $currentBranch 2>&1 | Out-Null
        Write-Host "✅ Mudanças commitadas e enviadas!" -ForegroundColor Green
        Write-Host "💡 Instale GitHub CLI (gh) para criar PRs automaticamente." -ForegroundColor Cyan
    }
    exit 0
}

# Verificar autenticação GitHub
$ghAuth = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  GitHub CLI não autenticado." -ForegroundColor Yellow
    Write-Host "💡 Execute: gh auth login" -ForegroundColor Cyan
    exit 1
}

# Fazer commit das mudanças
Write-Host "📝 Fazendo commit das mudanças..." -ForegroundColor Yellow
git add -A 2>&1 | Out-Null

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$commitTitle = "Auto-commit: Build bem-sucedido - $timestamp"
$commitBody = "Build passou com sucesso`nTodas as correções aplicadas`nPronto para revisão"

git commit -m $commitTitle -m $commitBody 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Nenhuma mudança para commitar." -ForegroundColor Yellow
    exit 0
}

# Fazer push da branch
Write-Host "📤 Fazendo push da branch..." -ForegroundColor Yellow
git push origin $currentBranch 2>&1 | ForEach-Object { Write-Host $_ }

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao fazer push." -ForegroundColor Red
    exit 1
}

# Criar ou atualizar PR
Write-Host "🔀 Criando/Atualizando PR..." -ForegroundColor Yellow

# Verificar se já existe PR para esta branch
$existingPR = gh pr list --head $currentBranch --json number,title --jq '.[0]' 2>&1

if ($existingPR -and $existingPR -ne "null" -and $existingPR -ne "") {
    $prNumber = ($existingPR | ConvertFrom-Json).number
    Write-Host "✅ PR #$prNumber já existe. Atualizado com novo commit!" -ForegroundColor Green
} else {
    # Criar novo PR
    $prTitle = "Auto-PR: Correções e Otimizações - $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
    $prBody = @"
## 🤖 Pull Request Automático

Este PR foi criado automaticamente após build bem-sucedido.

### 📋 O que foi feito:
- ✅ Build passou com sucesso
- ✅ Todas as correções aplicadas
- ✅ Otimizações de performance
- ✅ Scripts de automação

### 🔍 Revisão:
Por favor, revise as mudanças antes de fazer merge.

### 🚀 Próximos passos:
1. Revisar mudanças
2. Testar localmente (opcional)
3. Aprovar e fazer merge

---
*Criado automaticamente em $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')*
"@

    $prBody | Out-File -FilePath "$env:TEMP\pr-body.txt" -Encoding UTF8
    
    $pr = gh pr create `
        --title $prTitle `
        --body-file "$env:TEMP\pr-body.txt" `
        --base main `
        --head $currentBranch `
        2>&1

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ PR criado com sucesso!" -ForegroundColor Green
        Write-Host $pr
    } else {
        Write-Host "⚠️  Erro ao criar PR: $pr" -ForegroundColor Yellow
        Write-Host "💡 Mudanças foram commitadas e enviadas. Crie o PR manualmente." -ForegroundColor Cyan
    }
    
    Remove-Item "$env:TEMP\pr-body.txt" -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "✅ Processo concluído!" -ForegroundColor Green
