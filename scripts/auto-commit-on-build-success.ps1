# Script PowerShell para commitar e fazer push automaticamente quando build passa
# Este script será chamado automaticamente após build bem-sucedido

# Verificar se há mudanças para commitar
$status = git status --porcelain
if (-not $status) {
    Write-Host "ℹ️  Nenhuma mudança para commitar." -ForegroundColor Gray
    exit 0
}

# Criar mensagem de commit automática
$COMMIT_MSG = "Auto-commit: Correções de build - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

Write-Host "📝 Fazendo commit automático das mudanças..." -ForegroundColor Cyan
git add -A

git commit -m $COMMIT_MSG
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Nenhuma mudança para commitar ou commit falhou." -ForegroundColor Yellow
    exit 0
}

Write-Host "📤 Fazendo push para o repositório remoto..." -ForegroundColor Cyan
git push origin HEAD
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Push falhou. Verifique a conexão ou credenciais." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Mudanças commitadas e enviadas com sucesso!" -ForegroundColor Green
