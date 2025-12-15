# Script para capturar logs específicos do débito atual
# Autor: Assistente Android Senior
# Data: 2025-01-06

Write-Host "=== CAPTURADOR DE LOGS - DÉBITO ATUAL ===" -ForegroundColor Cyan
Write-Host "Iniciando captura de logs específicos do débito atual..." -ForegroundColor Yellow

# Parar captura anterior se estiver rodando
$processos = Get-Process -Name "adb" -ErrorAction SilentlyContinue
if ($processos) {
    Write-Host "Parando processos ADB anteriores..." -ForegroundColor Yellow
    Stop-Process -Name "adb" -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

# Iniciar ADB
Write-Host "Iniciando ADB..." -ForegroundColor Green
adb start-server

# Verificar se há dispositivos conectados
$devices = adb devices
if ($devices -notmatch "device$") {
    Write-Host "ERRO: Nenhum dispositivo Android conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo e tente novamente." -ForegroundColor Red
    exit 1
}

Write-Host "Dispositivo conectado. Iniciando captura de logs..." -ForegroundColor Green

# Limpar logs anteriores
adb logcat -c

# Capturar logs específicos do débito atual
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = "logcat-debito-atual-$timestamp.txt"

Write-Host "Capturando logs para: $logFile" -ForegroundColor Cyan
Write-Host "Filtros aplicados:" -ForegroundColor Yellow
Write-Host "- SettlementViewModel" -ForegroundColor White
Write-Host "- SettlementFragment" -ForegroundColor White
Write-Host "- ClienteRepository" -ForegroundColor White
Write-Host "- ClienteDao" -ForegroundColor White
Write-Host "- Acerto" -ForegroundColor White

# Capturar logs com filtros específicos
adb logcat | Select-String -Pattern "SettlementViewModel|SettlementFragment|ClienteRepository|ClienteDao|Acerto|debito|Débito|DEBITO" | Tee-Object -FilePath $logFile

Write-Host "`n=== CAPTURA CONCLUÍDA ===" -ForegroundColor Green
Write-Host "Logs salvos em: $logFile" -ForegroundColor Cyan
Write-Host "Analise o arquivo para verificar o fluxo do débito atual." -ForegroundColor Yellow 