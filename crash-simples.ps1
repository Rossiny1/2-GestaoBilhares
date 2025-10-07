# ========================================
# SCRIPT SIMPLES DE MONITORAMENTO DE CRASH - GESTAO BILHARES
# Captura TODOS os crashes e erros do app
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MONITORAMENTO SIMPLES DE CRASH - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB está disponível
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK está instalado corretamente" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

# Verificar se o dispositivo está conectado
Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
$devices = & $adbPath devices

Write-Host "Dispositivos encontrados:" -ForegroundColor Yellow
Write-Host $devices -ForegroundColor Gray

# Verificar se há dispositivo conectado
$hasDevice = $devices | Select-String "device$"

if (-not $hasDevice) {
    Write-Host "ERRO: Nenhum dispositivo Android conectado" -ForegroundColor Red
    Write-Host "Conecte o dispositivo via USB e habilite a depuração" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Dispositivo conectado: OK" -ForegroundColor Green
Write-Host ""

# Limpar logcat anterior
Write-Host "Limpando logcat anterior..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "Iniciando monitoramento de crashes..." -ForegroundColor Green
Write-Host "Agora você pode testar o app no dispositivo" -ForegroundColor Green
Write-Host ""

# Padrão para capturar TODOS os crashes e erros
$pattern = "FATAL|AndroidRuntime|crash|Exception|Caused by|Error|ERROR|gestaobilhares"

Write-Host "LOGS MONITORADOS:" -ForegroundColor Cyan
Write-Host "- FATAL: Crashes fatais" -ForegroundColor Gray
Write-Host "- AndroidRuntime: Exceções do Android" -ForegroundColor Gray
Write-Host "- Exception: Todas as exceções" -ForegroundColor Gray
Write-Host "- Caused by: Stack traces" -ForegroundColor Gray
Write-Host "- Error/ERROR: Mensagens de erro" -ForegroundColor Gray
Write-Host "- gestaobilhares: Logs do nosso app" -ForegroundColor Gray
Write-Host ""

# Monitorar logcat filtrando apenas erros e crashes
try {
    Write-Host "Monitorando em tempo real..." -ForegroundColor Green
    Write-Host "Filtros: $pattern" -ForegroundColor Gray
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    & $adbPath logcat -v time | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}

Write-Host "" 
Write-Host "Monitoramento finalizado" -ForegroundColor Gray
Read-Host "Pressione Enter para sair"
