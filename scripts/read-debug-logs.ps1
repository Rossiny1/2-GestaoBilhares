# ========================================
# SCRIPT DE LEITURA DE LOGS DE DEBUG - GESTAO BILHARES
# Filtra logs relacionados ao problema de rotaId do cliente
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "LEITURA DE LOGS DE DEBUG - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Caminho correto do ADB (mesmo do script crash.ps1)
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB está disponível
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK está instalado corretamente" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "ADB encontrado: OK" -ForegroundColor Green

# Verificar se há dispositivos conectados
Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
$devices = & $adbPath devices

Write-Host "Dispositivos encontrados:" -ForegroundColor Yellow
Write-Host $devices -ForegroundColor Gray

# Verificar se há linha com "device" no final
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

Write-Host "Iniciando monitoramento de logs de debug..." -ForegroundColor Green
Write-Host "Agora você pode testar o app no dispositivo" -ForegroundColor Green
Write-Host ""
Write-Host "LOGS MONITORADOS:" -ForegroundColor Cyan
Write-Host "- ClientDetailFragment: Logs do fragmento de detalhes do cliente" -ForegroundColor Gray
Write-Host "- ClientDetailViewModel: Logs do ViewModel de detalhes do cliente" -ForegroundColor Gray
Write-Host "- AppRepository: Logs do repositório principal" -ForegroundColor Gray
Write-Host "- Busca por rotaId: Logs relacionados ao problema de rota" -ForegroundColor Gray
Write-Host ""

# Padrão para capturar logs específicos do problema
$pattern = "ClientDetailFragment|ClientDetailViewModel|AppRepository|rotaId|buscarRotaIdPorCliente|Cliente encontrado|RotaId encontrado|Args\.clienteId|Buscando cliente|Cliente obtido do ViewModel"

Write-Host "Monitorando em tempo real..." -ForegroundColor Green
Write-Host "Filtros: $pattern" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Monitorar logcat filtrando apenas os logs relevantes
try {
    & $adbPath logcat -v time | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}

Write-Host "" 
Write-Host "Monitoramento finalizado" -ForegroundColor Gray
Read-Host "Pressione Enter para sair"