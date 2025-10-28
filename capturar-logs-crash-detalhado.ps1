# Script para capturar logs detalhados do crash
# Baseado no crash3.ps1 com logs mais abrangentes

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
    Write-Host "Dispositivo Android conectado. Iniciando captura detalhada..." -ForegroundColor Green
} else {
    Write-Host "Nenhum dispositivo Android conectado." -ForegroundColor Red
    Write-Host "Conecte um dispositivo ou emulador e tente novamente." -ForegroundColor Yellow
    exit 1
}

Write-Host "=== CAPTURANDO LOGS CRASH DETALHADO ===" -ForegroundColor Cyan
Write-Host "Filtros aplicados:" -ForegroundColor Yellow
Write-Host "- AndroidRuntime: Erros críticos do sistema Android" -ForegroundColor White
Write-Host "- System.err: Erros gerais do sistema" -ForegroundColor White
Write-Host "- com.example.gestaobilhares: TODOS os logs do app" -ForegroundColor White
Write-Host "- FATAL: Erros fatais que causam crashes" -ForegroundColor White
Write-Host "- Exception: Exceções não tratadas" -ForegroundColor White
Write-Host "- CRASH: Crashes explícitos" -ForegroundColor White
Write-Host "- ERROR: Todos os tipos de erro" -ForegroundColor White
Write-Host "- WARN: Avisos importantes" -ForegroundColor White
Write-Host "- DEBUG: Logs de debug do app" -ForegroundColor White
Write-Host "- INFO: Logs informativos do app" -ForegroundColor White
Write-Host ""
Write-Host "INSTRUÇÕES:" -ForegroundColor Cyan
Write-Host "1. Vá para a tela de manutenção da mesa" -ForegroundColor White
Write-Host "2. Clique para selecionar pano" -ForegroundColor White
Write-Host "3. Aguarde o crash" -ForegroundColor White
Write-Host "4. Os logs aparecerão em tempo real abaixo" -ForegroundColor White
Write-Host "5. Pressione Ctrl+C para parar a captura" -ForegroundColor Yellow
Write-Host ""

# Padrão de busca MUITO mais abrangente para capturar TUDO
$pattern = "AndroidRuntime|System\.err|com\.example\.gestaobilhares|FATAL|Exception|CRASH|ERROR|WARN|Fatal|fatal|crash|error|exception|warn|DEBUG|INFO|debug|info|gestaobilhares"

$logFile = "logcat-crash-detalhado-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

try {
    & $adbPath logcat -c
    Write-Host "Logs limpos. Iniciando captura detalhada..." -ForegroundColor Green
    Write-Host "Arquivo de log: $logFile" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "=== LOGS EM TEMPO REAL ===" -ForegroundColor Green
    Write-Host ""
    & $adbPath logcat | Select-String -Pattern $pattern | Tee-Object -FilePath $logFile
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}
