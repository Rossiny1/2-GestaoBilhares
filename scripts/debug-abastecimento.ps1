# Script para debug específico do abastecimento
# Baseado no script crash que funciona

Write-Host "=== DEBUG ABASTECIMENTO ===" -ForegroundColor Green
Write-Host "Monitorando logs detalhados de abastecimento" -ForegroundColor Yellow
Write-Host ""

# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar dispositivo
$devices = & $adbPath devices
$hasDevice = $devices | Select-String "device$"

if (-not $hasDevice) {
    Write-Host "ERRO: Nenhum dispositivo conectado" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Dispositivo conectado: OK" -ForegroundColor Green
Write-Host ""

# Limpar logcat
& $adbPath logcat -c

Write-Host "Monitorando debug de abastecimento..." -ForegroundColor Cyan
Write-Host ""

# Filtrar logs específicos de debug
try {
    & $adbPath logcat -v time | Select-String -Pattern "VehicleDetailViewModel|DEBUG|TOTAL de abastecimentos|Abastecimento ID|Data=|Ano=|Match=|Filtro="
} catch {
    Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}
