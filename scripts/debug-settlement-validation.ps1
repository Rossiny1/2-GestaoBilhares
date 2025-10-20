# Script para monitorar logs de validação de acertos
# Foca especificamente na lógica de validação de acertos duplicados

$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB está disponível
if (-not (Test-Path $adbPath)) {
    Write-Host "ADB não encontrado em: $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK está instalado corretamente." -ForegroundColor Yellow
    exit 1
}

# Verificar se há dispositivos conectados
$devices = & $adbPath devices
if ($devices -match "device$") {
    Write-Host "Dispositivo Android conectado. Iniciando monitoramento..." -ForegroundColor Green
} else {
    Write-Host "Nenhum dispositivo Android conectado." -ForegroundColor Red
    Write-Host "Conecte um dispositivo ou emulador e tente novamente." -ForegroundColor Yellow
    exit 1
}

Write-Host "=== MONITORAMENTO DE VALIDAÇÃO DE ACERTOS ===" -ForegroundColor Cyan
Write-Host "Filtros aplicados:" -ForegroundColor Yellow
Write-Host "- SETTLEMENT: Logs do SettlementViewModel" -ForegroundColor White
Write-Host "- DEBUG VALIDAÇÃO: Logs de debug da validação" -ForegroundColor White
Write-Host "- ACERTO JÁ EXISTE: Mensagens de bloqueio" -ForegroundColor White
Write-Host "- Validação passou: Mensagens de sucesso" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Ctrl+C para parar o monitoramento" -ForegroundColor Yellow
Write-Host ""

# Padrão de busca para logs de validação de acertos
$pattern = "SETTLEMENT|DEBUG VALIDAÇÃO|ACERTO JÁ EXISTE|Validação passou|SALVAR_ACERTO|cicloId usado|Total de acertos encontrados|Acerto.*Status|modoEdicao"

try {
    & $adbPath logcat -c
    & $adbPath logcat | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}
