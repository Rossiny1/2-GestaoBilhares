# Script para capturar logs específicos do processo de acerto
# Autor: Assistente IA
# Data: 2025-01-07

Write-Host "=== CAPTURADOR DE LOGS DO ACERTO ===" -ForegroundColor Cyan
Write-Host "Iniciando captura de logs específicos do acerto..." -ForegroundColor Yellow

# Parar capturas anteriores
Write-Host "Parando processos adb anteriores..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -eq "adb"} | Stop-Process -Force -ErrorAction SilentlyContinue

# Aguardar um momento
Start-Sleep -Seconds 2

# Comando para capturar logs específicos do acerto
$logCommand = @"
adb logcat | findstr /i "SettlementViewModel\|SettlementFragment\|acerto\|debito\|foreign\|constraint\|error\|crash"
"@

Write-Host "Executando comando de captura..." -ForegroundColor Green
Write-Host "Comando: $logCommand" -ForegroundColor Gray
Write-Host ""
Write-Host "=== LOGS DO ACERTO CAPTURADOS ===" -ForegroundColor Cyan
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Red
Write-Host ""

# Executar o comando
Invoke-Expression $logCommand 