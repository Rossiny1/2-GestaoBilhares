# Script para monitorar atualizações de ciclo nas rotas
# Filtra logs relacionados a mudanças de ciclo e atualização de rotas

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

Write-Host "=== MONITORAMENTO DE ATUALIZAÇÃO DE CICLOS NAS ROTAS ===" -ForegroundColor Cyan
Write-Host "Filtros aplicados:" -ForegroundColor Yellow
Write-Host "- RoutesFragment: Logs de atualização da tela de rotas" -ForegroundColor White
Write-Host "- RoutesViewModel: Logs do ViewModel das rotas" -ForegroundColor White
Write-Host "- RotaRepository: Logs de cálculo de ciclo atual" -ForegroundColor White
Write-Host "- CicloAcertoDao: Logs de operações com ciclos" -ForegroundColor White
Write-Host "- Flow atualizado: Logs de atualização do Flow" -ForegroundColor White
Write-Host "- Ciclo: Logs de mudanças de ciclo" -ForegroundColor White
Write-Host "- Status: Logs de mudanças de status" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Ctrl+C para parar o monitoramento" -ForegroundColor Yellow
Write-Host ""

# Padrão de busca para logs de atualização de ciclos
$pattern = "RoutesFragment|RoutesViewModel|RotaRepository|CicloAcertoDao|RoutesAdapter|Flow atualizado|Ciclo|Status|onResume|refresh|aplicarFiltro|getRotasResumoComAtualizacaoTempoReal|calcularCicloAtualReal|determinarStatusRotaEmTempoReal|DEBUG CICLO|TEXTO CICLO|CALCULANDO CICLO"

try {
    & $adbPath logcat -c
    & $adbPath logcat | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}
