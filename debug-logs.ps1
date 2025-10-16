# Script simples para ler logs de debug
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "=== LOGS DE DEBUG - PROBLEMA ROTA ID ===" -ForegroundColor Green

# Verificar ADB
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado" -ForegroundColor Red
    exit 1
}

# Verificar dispositivo
$devices = & $adbPath devices
$hasDevice = $devices | Select-String "device$"
if (-not $hasDevice) {
    Write-Host "ERRO: Nenhum dispositivo conectado" -ForegroundColor Red
    exit 1
}

Write-Host "Dispositivo OK. Iniciando monitoramento..." -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow
Write-Host ""

# Limpar logs antigos
& $adbPath logcat -c

# Monitorar logs
& $adbPath logcat -v time | Select-String "ClientDetailFragment|ClientDetailViewModel|AppRepository|rotaId|buscarRotaIdPorCliente"
