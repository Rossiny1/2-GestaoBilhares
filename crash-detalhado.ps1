# ========================================
# SCRIPT DETALHADO DE MONITORAMENTO DE CRASH - GESTAO BILHARES
# Captura logs específicos do app + crashes
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MONITORAMENTO DETALHADO DE CRASH - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB está disponível
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath" -ForegroundColor Red
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

Write-Host "Iniciando monitoramento detalhado..." -ForegroundColor Green
Write-Host "Agora você pode testar o app no dispositivo" -ForegroundColor Green
Write-Host ""

# Padrão expandido para capturar logs específicos do app
$pattern = "FATAL|AndroidRuntime|crash|Exception|Caused by|Error|ERROR|gestaobilhares|LOG_CRASH|LOG_APP|LOG_NAVIGATION|LOG_MENU|LOG_SIGNATURE|LOG_CONTRACT|LOG_ERROR|LOG_DEBUG|LOG_INFO|LOG_WARNING|LOG_VERBOSE|LOG_ASSERT|LOG_D|LOG_E|LOG_I|LOG_V|LOG_W|LOG_WTF"

Write-Host "LOGS MONITORADOS:" -ForegroundColor Cyan
Write-Host "- FATAL: Crashes fatais do app" -ForegroundColor Red
Write-Host "- AndroidRuntime: Exceções do sistema Android" -ForegroundColor Red
Write-Host "- Exception: Todas as exceções Java/Kotlin" -ForegroundColor Red
Write-Host "- Caused by: Stack traces completos" -ForegroundColor Red
Write-Host "- Error/ERROR: Mensagens de erro" -ForegroundColor Red
Write-Host "- gestaobilhares: Logs específicos do nosso app" -ForegroundColor Gray
Write-Host "- LOG_CRASH: Logs de crash específicos do app" -ForegroundColor Red
Write-Host "- LOG_APP: Logs de aplicação" -ForegroundColor Gray
Write-Host "- LOG_NAVIGATION: Logs de navegação" -ForegroundColor Gray
Write-Host "- LOG_MENU: Logs de menu" -ForegroundColor Gray
Write-Host "- LOG_SIGNATURE: Logs de assinatura" -ForegroundColor Gray
Write-Host "- LOG_CONTRACT: Logs de contratos" -ForegroundColor Gray
Write-Host "- LOG_ERROR: Logs de erro" -ForegroundColor Red
Write-Host "- LOG_DEBUG: Logs de debug" -ForegroundColor Gray
Write-Host "- LOG_INFO: Logs de informação" -ForegroundColor Gray
Write-Host "- LOG_WARNING: Logs de aviso" -ForegroundColor Yellow
Write-Host ""

# Monitorar logcat filtrando logs específicos
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
