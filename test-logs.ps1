# ========================================
# SCRIPT DE TESTE DE LOGS - GESTAO BILHARES
# Testa se os logs de crash estão funcionando
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE DE LOGS - GESTAO BILHARES" -ForegroundColor Cyan
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

Write-Host "Iniciando teste de logs..." -ForegroundColor Green
Write-Host "Agora você pode testar o app no dispositivo" -ForegroundColor Green
Write-Host ""

# Padrão específico para logs de crash
$pattern = "LOG_CRASH"

Write-Host "LOGS MONITORADOS:" -ForegroundColor Cyan
Write-Host "- LOG_CRASH: Logs de crash específicos do app" -ForegroundColor Red
Write-Host ""

# Monitorar logcat filtrando apenas logs de crash
try {
    Write-Host "Monitorando logs de crash..." -ForegroundColor Green
    Write-Host "Filtros: $pattern" -ForegroundColor Gray
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    & $adbPath logcat -v time | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}

Write-Host "" 
Write-Host "Teste de logs finalizado" -ForegroundColor Gray
Read-Host "Pressione Enter para sair"
