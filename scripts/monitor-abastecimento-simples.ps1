# Script simples para monitorar apenas abastecimento
# Baseado no script crash que funciona

Write-Host "=== MONITOR DE ABASTECIMENTO ===" -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow
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

Write-Host "Monitorando abastecimento..." -ForegroundColor Cyan
Write-Host ""

# Filtrar apenas logs de abastecimento
try {
    & $adbPath logcat -v time | Select-String -Pattern "ExpenseRegisterViewModel|VehicleDetailViewModel|Salvando abastecimento|Abastecimento salvo|Despesa salva|Histórico de veículo|Carregando histórico|Abastecimentos encontrados|DEBUG|TOTAL de abastecimentos|Abastecimento ID|Data=|Ano=|Match=|Filtro=|Abastecimento FILTRADO"
} catch {
    Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}
