# Script para capturar TODOS os logs do sistema
# Sem filtros para não perder nenhum crash

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
    Write-Host "Dispositivo Android conectado. Iniciando captura universal..." -ForegroundColor Green
} else {
    Write-Host "Nenhum dispositivo Android conectado." -ForegroundColor Red
    Write-Host "Conecte um dispositivo ou emulador e tente novamente." -ForegroundColor Yellow
    exit 1
}

Write-Host "=== CAPTURANDO TODOS OS LOGS ===" -ForegroundColor Cyan
Write-Host "ATENÇÃO: Este script captura TODOS os logs do sistema" -ForegroundColor Yellow
Write-Host "Sem filtros para garantir que nenhum crash seja perdido" -ForegroundColor Yellow
Write-Host ""
Write-Host "INSTRUÇÕES:" -ForegroundColor Cyan
Write-Host "1. Vá para a tela de manutenção da mesa" -ForegroundColor White
Write-Host "2. Clique para selecionar pano" -ForegroundColor White
Write-Host "3. Aguarde o crash" -ForegroundColor White
Write-Host "4. Os logs aparecerão em tempo real abaixo" -ForegroundColor White
Write-Host "5. Pressione Ctrl+C para parar a captura" -ForegroundColor Yellow
Write-Host ""

$logFile = "logcat-universal-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

try {
    & $adbPath logcat -c
    Write-Host "Logs limpos. Iniciando captura universal..." -ForegroundColor Green
    Write-Host "Arquivo de log: $logFile" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "=== LOGS EM TEMPO REAL ===" -ForegroundColor Green
    Write-Host ""
    # Capturar TODOS os logs sem filtros
    & $adbPath logcat | Tee-Object -FilePath $logFile
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}
