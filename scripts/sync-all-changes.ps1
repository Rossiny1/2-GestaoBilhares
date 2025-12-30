# Script PowerShell para sincronizar todas as mudanças (GitHub e local)
# Uso: .\scripts\sync-all-changes.ps1

Write-Host "🔄 Sincronizando todas as mudanças..." -ForegroundColor Cyan
Write-Host ""

# 1. Verificar status atual
Write-Host "📊 Verificando status do repositório..." -ForegroundColor Yellow
git status --short

# 2. Adicionar todas as mudanças locais (se houver)
$changes = git status --porcelain
if ($changes) {
    Write-Host ""
    Write-Host "📝 Mudanças locais detectadas. Fazendo commit..." -ForegroundColor Yellow
    git add -A
    
    $COMMIT_MSG = "Auto-sync: Salvando mudanças locais - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    git commit -m $COMMIT_MSG
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Mudanças locais commitadas!" -ForegroundColor Green
    }
}

# 3. Buscar mudanças remotas
Write-Host ""
Write-Host "📥 Buscando mudanças do GitHub..." -ForegroundColor Yellow
git fetch origin

# 4. Verificar se há mudanças remotas para puxar
$LOCAL = git rev-parse @
$REMOTE = git rev-parse @{u} 2>$null
$BASE = git merge-base @ @{u} 2>$null

if ($REMOTE -and $BASE) {
    if ($LOCAL -eq $REMOTE) {
        Write-Host "✅ Repositório local está atualizado com o remoto." -ForegroundColor Green
    }
    elseif ($LOCAL -eq $BASE) {
        Write-Host "📥 Atualizações disponíveis no GitHub. Fazendo pull..." -ForegroundColor Yellow
        git pull origin
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Mudanças do GitHub baixadas!" -ForegroundColor Green
        }
    }
    elseif ($REMOTE -eq $BASE) {
        Write-Host "📤 Você tem commits locais. Fazendo push..." -ForegroundColor Yellow
        git push origin HEAD
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Mudanças locais enviadas para o GitHub!" -ForegroundColor Green
        }
    }
    else {
        Write-Host "⚠️  Divergência detectada. Faça merge manualmente." -ForegroundColor Red
    }
}

# 5. Fazer push de qualquer commit local pendente
Write-Host ""
Write-Host "📤 Verificando commits locais não enviados..." -ForegroundColor Yellow
$LOCAL_COMMITS = git log @{u}..@ --oneline 2>$null
if ($LOCAL_COMMITS) {
    Write-Host "Encontrados commits locais não enviados:" -ForegroundColor Yellow
    $LOCAL_COMMITS | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }
    Write-Host ""
    Write-Host "📤 Fazendo push para o GitHub..." -ForegroundColor Yellow
    git push origin HEAD
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Todos os commits foram enviados para o GitHub!" -ForegroundColor Green
    }
}

# 6. Resumo final
Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "✅ Sincronização completa!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Status final:" -ForegroundColor Cyan
git status --short
Write-Host ""
Write-Host "📝 Últimos 3 commits:" -ForegroundColor Cyan
git log --oneline -3
Write-Host ""
