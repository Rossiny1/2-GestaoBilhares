# Script para monitorar logs de navegação da lista de clientes
# Foca especificamente no problema da lista ficando vazia ao voltar

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

Write-Host "=== MONITORAMENTO DE NAVEGAÇÃO DA LISTA DE CLIENTES ===" -ForegroundColor Cyan
Write-Host "Filtros aplicados:" -ForegroundColor Yellow
Write-Host "- ClientListFragment: Logs de ciclo de vida e carregamento" -ForegroundColor White
Write-Host "- ClientListViewModel: Logs de carregamento de dados" -ForegroundColor White
Write-Host "- ClientDetailFragment: Logs de navegação de volta" -ForegroundColor White
Write-Host "- LOG_CRASH: Logs de debug de navegação" -ForegroundColor White
Write-Host "- DEBUG_DIAG: Logs de diagnóstico" -ForegroundColor White
Write-Host "- AppLogger: Logs do sistema" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Ctrl+C para parar o monitoramento" -ForegroundColor Yellow
Write-Host ""

# Padrão de busca para logs de navegação da lista de clientes
$pattern = "ClientListFragment|ClientListViewModel|ClientDetailFragment|LOG_CRASH|DEBUG_DIAG|AppLogger|onViewCreated|onStart|onResume|carregarClientes|forcarRecarregamentoClientes|Dados recarregados|Dados forçados|clientes\.size|rotaId=|Navegando para detalhes|setupBackButtonHandler"

try {
    & $adbPath logcat -c
    & $adbPath logcat | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}
