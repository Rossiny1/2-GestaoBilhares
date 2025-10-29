# Script para monitorar sincronização de ciclos em tempo real
# Baseado no crash3.ps1 que funciona

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
    Write-Host "Dispositivo Android conectado. Iniciando monitoramento de sincronização de ciclos..." -ForegroundColor Green
} else {
    Write-Host "Nenhum dispositivo Android conectado." -ForegroundColor Red
    Write-Host "Conecte um dispositivo ou emulador e tente novamente." -ForegroundColor Yellow
    exit 1
}

Write-Host "=== MONITORAMENTO DE SINCRONIZAÇÃO DE CICLOS ===" -ForegroundColor Cyan
Write-Host "Filtros aplicados:" -ForegroundColor Yellow
Write-Host "- SyncManagerV2: Logs do gerenciador de sincronização" -ForegroundColor White
Write-Host "- CicloAcertoRepo: Logs do repositório de ciclos" -ForegroundColor White
Write-Host "- AppRepository: Logs do repositório principal" -ForegroundColor White
Write-Host "- AndroidRuntime: Erros críticos do sistema Android" -ForegroundColor White
Write-Host "- System.err: Erros gerais do sistema" -ForegroundColor White
Write-Host "- ciclos: Todos os logs relacionados a ciclos" -ForegroundColor White
Write-Host "- numeroCiclo: Logs específicos do numeroCiclo" -ForegroundColor White
Write-Host ""
Write-Host "INSTRUÇÕES:" -ForegroundColor Cyan
Write-Host "1. Abra o app no dispositivo" -ForegroundColor White
Write-Host "2. Vá para a tela de sincronização" -ForegroundColor White
Write-Host "3. Execute a sincronização (PUSH ou PULL)" -ForegroundColor White
Write-Host "4. Os logs aparecerão em tempo real abaixo" -ForegroundColor White
Write-Host "5. Pressione Ctrl+C para parar o monitoramento" -ForegroundColor Yellow
Write-Host ""

# Padrão de busca para sincronização de ciclos
$pattern = "SyncManagerV2|CicloAcertoRepo|AppRepository|AndroidRuntime|System\.err|ciclo|Ciclo|numeroCiclo|numero_ciclo|CICLO|ERROR|Exception|FATAL"

try {
    & $adbPath logcat -c
    Write-Host "Logs limpos. Iniciando monitoramento..." -ForegroundColor Green
    Write-Host ""
    Write-Host "=== LOGS EM TEMPO REAL ===" -ForegroundColor Green
    Write-Host ""
    & $adbPath logcat | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}