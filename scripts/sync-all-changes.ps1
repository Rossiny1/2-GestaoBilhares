# Script PowerShell para sincronizar todas as mudanças (GitHub e local)
# Uso: .\scripts\sync-all-changes.ps1

$ErrorActionPreference = "Continue"

Write-Host "🔄 Sincronizando todas as mudanças..." -ForegroundColor Cyan
Write-Host ""

# Verificar se estamos em um repositório git
if (-not (Test-Path ".git")) {
    Write-Host "❌ Erro: Não é um repositório Git. Execute este script na raiz do projeto." -ForegroundColor Red
    exit 1
}

# 1. Verificar status atual
Write-Host "📊 Verificando status do repositório..." -ForegroundColor Yellow
try {
    $status = git status --short 2>&1
    if ($status) {
        Write-Host $status
    }
} catch {
    Write-Host "⚠️  Erro ao verificar status: $_" -ForegroundColor Yellow
}

# 2. Adicionar todas as mudanças locais (se houver)
Write-Host ""
Write-Host "📝 Verificando mudanças locais..." -ForegroundColor Yellow
try {
    git add -A 2>&1 | Out-Null
    
    $staged = git diff --cached --quiet 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "📝 Mudanças locais detectadas. Fazendo commit..." -ForegroundColor Yellow
        
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        $COMMIT_MSG = "Auto-sync: Salvando mudanças locais - $timestamp"
        
        git commit -m $COMMIT_MSG 2>&1 | Out-Null
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Mudanças locais commitadas!" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Nenhuma mudança para commitar ou commit falhou." -ForegroundColor Yellow
        }
    } else {
        Write-Host "ℹ️  Nenhuma mudança local para commitar." -ForegroundColor Gray
    }
} catch {
    Write-Host "⚠️  Erro ao processar mudanças locais: $_" -ForegroundColor Yellow
}

# 3. Buscar mudanças remotas
Write-Host ""
Write-Host "📥 Buscando mudanças do GitHub..." -ForegroundColor Yellow
try {
    git fetch origin 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "⚠️  Erro ao fazer fetch. Verifique sua conexão." -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Erro ao fazer fetch: $_" -ForegroundColor Yellow
}

# 4. Verificar branch atual
try {
    $currentBranch = git branch --show-current 2>&1
    if (-not $currentBranch) {
        $currentBranch = git rev-parse --abbrev-ref HEAD 2>&1
    }
    Write-Host "📍 Branch atual: $currentBranch" -ForegroundColor Cyan
} catch {
    Write-Host "⚠️  Erro ao detectar branch: $_" -ForegroundColor Yellow
    $currentBranch = "HEAD"
}

# 5. Fazer pull
Write-Host ""
Write-Host "📥 Fazendo pull do GitHub..." -ForegroundColor Yellow
try {
    if ($currentBranch -and $currentBranch -ne "HEAD") {
        git pull origin $currentBranch 2>&1 | ForEach-Object { Write-Host $_ }
    } else {
        git pull origin 2>&1 | ForEach-Object { Write-Host $_ }
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Mudanças do GitHub baixadas!" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Erro ao fazer pull ou já está atualizado." -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Erro ao fazer pull: $_" -ForegroundColor Yellow
}

# 6. Fazer push de qualquer commit local pendente
Write-Host ""
Write-Host "📤 Verificando commits locais não enviados..." -ForegroundColor Yellow
try {
    if ($currentBranch -and $currentBranch -ne "HEAD") {
        $localCommits = git log origin/$currentBranch..HEAD --oneline 2>&1
    } else {
        $localCommits = git log @{u}..@ --oneline 2>&1
    }
    
    if ($localCommits -and $localCommits.Count -gt 0) {
        Write-Host "Encontrados commits locais não enviados:" -ForegroundColor Yellow
        $localCommits | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }
        Write-Host ""
        Write-Host "📤 Fazendo push para o GitHub..." -ForegroundColor Yellow
        
        if ($currentBranch -and $currentBranch -ne "HEAD") {
            git push origin $currentBranch 2>&1 | ForEach-Object { Write-Host $_ }
        } else {
            git push origin HEAD 2>&1 | ForEach-Object { Write-Host $_ }
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Todos os commits foram enviados para o GitHub!" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Erro ao fazer push ou não há commits para enviar." -ForegroundColor Yellow
        }
    } else {
        Write-Host "ℹ️  Nenhum commit local para enviar." -ForegroundColor Gray
    }
} catch {
    Write-Host "⚠️  Erro ao verificar/push commits: $_" -ForegroundColor Yellow
}

# 7. Resumo final
Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "✅ Sincronização completa!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Status final:" -ForegroundColor Cyan
try {
    $finalStatus = git status --short 2>&1
    if ($finalStatus) {
        Write-Host $finalStatus
    } else {
        Write-Host "  (nenhuma mudança pendente)" -ForegroundColor Gray
    }
} catch {
    Write-Host "  (erro ao verificar status)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "📝 Últimos 3 commits:" -ForegroundColor Cyan
try {
    git log --oneline -3 2>&1 | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
} catch {
    Write-Host "  (erro ao listar commits)" -ForegroundColor Yellow
}
Write-Host ""
