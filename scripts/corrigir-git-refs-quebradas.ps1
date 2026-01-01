# Script para corrigir referências Git quebradas
# Erro: fatal: bad object refs/heads/master (1)

Write-Host "🔧 Corrigindo referências Git quebradas..." -ForegroundColor Cyan

# 1. Remover referência quebrada master
Write-Host "1. Removendo referência quebrada master..." -ForegroundColor Yellow
if (Test-Path ".git\refs\heads\master") {
    Remove-Item ".git\refs\heads\master" -Force -ErrorAction SilentlyContinue
    Write-Host "   ✅ Referência master removida" -ForegroundColor Green
} else {
    Write-Host "   ℹ️  Referência master não encontrada" -ForegroundColor Gray
}

# 2. Limpar referências quebradas no packed-refs
Write-Host "2. Verificando packed-refs..." -ForegroundColor Yellow
if (Test-Path ".git\packed-refs") {
    $packedRefs = Get-Content ".git\packed-refs" -ErrorAction SilentlyContinue
    if ($packedRefs -match "refs/heads/master") {
        Write-Host "   ⚠️  Referência master encontrada em packed-refs" -ForegroundColor Yellow
        Write-Host "   💡 Pode ser necessário editar manualmente" -ForegroundColor Gray
    }
}

# 3. Verificar status
Write-Host "3. Verificando status do Git..." -ForegroundColor Yellow
git status --short 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✅ Git funcionando corretamente" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Ainda há problemas" -ForegroundColor Yellow
}

# 4. Tentar fazer fetch
Write-Host "4. Fazendo fetch do origin..." -ForegroundColor Yellow
git fetch origin 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✅ Fetch concluído" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Fetch falhou, mas pode ser normal" -ForegroundColor Yellow
}

# 5. Tentar pull novamente
Write-Host "5. Tentando pull novamente..." -ForegroundColor Yellow
git pull --tags --autostash origin cursor/cursor-build-failure-fix-efaf 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✅ Pull concluído com sucesso!" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Pull ainda falhou" -ForegroundColor Yellow
    Write-Host "   💡 Tente: git fetch origin && git merge origin/cursor/cursor-build-failure-fix-efaf" -ForegroundColor Gray
}

Write-Host ""
Write-Host "✅ Correção concluída!" -ForegroundColor Green
