# 🚀 INICIAR AUTO-ACEITE INTELIGENTE - RÁPIDO
# Script para iniciar auto-aceite em background

Write-Host "🚀 INICIANDO AUTO-ACEITE INTELIGENTE..." -ForegroundColor Green

# Verificar se o arquivo existe
if (-not (Test-Path "auto-aceite-inteligente.ps1")) {
    Write-Host "❌ Arquivo auto-aceite-inteligente.ps1 não encontrado!" -ForegroundColor Red
    Write-Host "Execute este script no diretório correto." -ForegroundColor Yellow
    exit 1
}

try {
    # Iniciar em nova janela PowerShell
    $processo = Start-Process PowerShell -ArgumentList @(
        "-NoExit",
        "-Command", 
        "& '.\auto-aceite-inteligente.ps1'"
    ) -PassThru -WindowStyle Normal
    
    Write-Host "✅ Auto-aceite iniciado com sucesso!" -ForegroundColor Green
    Write-Host "📋 PID: $($processo.Id)" -ForegroundColor Cyan
    Write-Host "🖥️ Janela separada aberta para monitoramento" -ForegroundColor Blue
    Write-Host "⚡ Comandos seguros serão aprovados automaticamente" -ForegroundColor Yellow
    Write-Host "🛡️ Comandos perigosos precisarão aprovação manual" -ForegroundColor Red
    
    Write-Host ""
    Write-Host "🎯 COMANDOS AUTO-APROVADOS:" -ForegroundColor Green
    Write-Host "  - gradlew clean, build, test" -ForegroundColor White
    Write-Host "  - git status, add, commit, push" -ForegroundColor White  
    Write-Host "  - adb logcat" -ForegroundColor White
    Write-Host "  - Write-Host, ls, cd, etc." -ForegroundColor White
    
    Write-Host ""
    Write-Host "🔐 PARA PARAR:" -ForegroundColor Yellow
    Write-Host "  Ctrl+C na janela do auto-aceite" -ForegroundColor White
    Write-Host "  Ou feche a janela PowerShell" -ForegroundColor White
    
} catch {
    Write-Host "❌ Erro ao iniciar auto-aceite: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🚀 Auto-aceite ativo! Continue desenvolvendo normalmente." -ForegroundColor Green 