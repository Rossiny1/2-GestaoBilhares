# Script para capturar logs específicos do GestaoBilhares
# Autor: Assistente IA
# Data: 2025-01-07

Write-Host "=== CAPTURADOR DE LOGS GESTAO BILHARES ===" -ForegroundColor Cyan
Write-Host "Iniciando captura de logs específicos..." -ForegroundColor Yellow

# Parar capturas anteriores
Write-Host "Parando processos adb anteriores..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -eq "adb"} | Stop-Process -Force -ErrorAction SilentlyContinue

# Aguardar um momento
Start-Sleep -Seconds 2

# Comando para capturar logs específicos do nosso app
$logCommand = @"
adb logcat | findstr /i "com.example.gestaobilhares\|GestaoBilhares\|AuthViewModel\|Firebase\|login\|crash\|error\|fatal"
"@

Write-Host "Executando comando de captura..." -ForegroundColor Green
Write-Host "Comando: $logCommand" -ForegroundColor Gray
Write-Host ""
Write-Host "=== LOGS CAPTURADOS ===" -ForegroundColor Cyan
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Red
Write-Host ""

# Executar o comando
Invoke-Expression $logCommand 