# Script para monitorar logs de edição de cliente
# Foca especificamente na funcionalidade de edição de dados do cliente

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

Write-Host "=== MONITORAMENTO DE EDIÇÃO DE CLIENTE ===" -ForegroundColor Cyan
Write-Host "Filtros aplicados:" -ForegroundColor Yellow
Write-Host "- ClientDetailFragment: Logs de navegação para edição" -ForegroundColor White
Write-Host "- ClientRegisterFragment: Logs de carregamento e preenchimento" -ForegroundColor White
Write-Host "- ClientRegisterViewModel: Logs do ViewModel" -ForegroundColor White
Write-Host "- DEBUG NAVEGAÇÃO: Logs de debug da navegação" -ForegroundColor White
Write-Host "- PREENCHENDO CAMPOS: Logs de preenchimento dos campos" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Ctrl+C para parar o monitoramento" -ForegroundColor Yellow
Write-Host ""

# Padrão de busca para logs de edição de cliente e acerto
$pattern = "ClientDetailFragment|ClientRegisterFragment|ClientRegisterViewModel|SettlementFragment|SettlementViewModel|DEBUG NAVEGAÇÃO|PREENCHENDO CAMPOS|Modo EDIÇÃO|args\.clienteId|clientId sendo passado|Cliente recebido|Campos preenchidos|Acerto ID para edição|Modo edição|PREPARANDO MESAS PARA ACERTO|CARREGANDO DADOS DO ACERTO|PREENCHENDO CAMPOS COM DADOS|relógio inicial|relógio final|AcertoMesa encontrado"

try {
    & $adbPath logcat -c
    & $adbPath logcat | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}
