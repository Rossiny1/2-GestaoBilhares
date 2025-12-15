# 🚀 START AUTO-ACEITE - ULTRA RÁPIDO
Write-Host "🚀 INICIANDO AUTO-ACEITE ULTRA-RÁPIDO..." -ForegroundColor Green

if (Test-Path "auto-aceite-completo.ps1") {
    # Iniciar em background
    Start-Process PowerShell -ArgumentList "-NoExit", "-Command", "& '.\auto-aceite-completo.ps1'" -WindowStyle Minimized
    Write-Host "✅ Auto-aceite rodando em background!" -ForegroundColor Green
    Write-Host "⚡ Comandos gradle serão aceitos automaticamente" -ForegroundColor Yellow
} else {
    Write-Host "❌ Arquivo auto-aceite-completo.ps1 não encontrado" -ForegroundColor Red
}

# Aguardar um pouco e testar
Start-Sleep -Seconds 2
Write-Host "🧪 Testando auto-aceite..." -ForegroundColor Blue 