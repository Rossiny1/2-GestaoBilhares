# Script para capturar logs de diagnostico do problema de Cards do Acerto nao aparecerem
# Versao: 5.1 - Adaptado para diagnostico cirurgico com DEBUG_POPUP

Write-Host "=== CAPTURA DE LOGS - DIAGNOSTICO CIRURGICO PANO V5.1 ===" -ForegroundColor Yellow
Write-Host "Objetivo: Rastrear onde panoNovoId se perde na construcao do DTO" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB
$ADBPath = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (!(Test-Path $ADBPath)) {
    Write-Host "ADB nao encontrado em: $ADBPath" -ForegroundColor Red
    exit 1
}

# Verificar dispositivo
Write-Host "Verificando dispositivo..." -ForegroundColor Yellow
$devices = & $ADBPath devices
if ($devices -match "device$") {
    Write-Host "Dispositivo encontrado!" -ForegroundColor Green
} else {
    Write-Host "Nenhum dispositivo conectado!" -ForegroundColor Red
    exit 1
}

# Limpar logs
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $ADBPath logcat -c

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CAPTURANDO LOGS DO DIAGNOSTICO CIRURGICO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "INSTRUCOES ESPECIFICAS:" -ForegroundColor Green
Write-Host "  1. Abra app > Acerto" -ForegroundColor White
Write-Host "  2. Selecione cliente > Adicione mesa" -ForegroundColor White
Write-Host "  3. MARQUE Trocar Pano > Selecione pano na lista" -ForegroundColor White
Write-Host "  4. Salve acerto" -ForegroundColor White
Write-Host "  5. Abra tela Reforma de Mesas" -ForegroundColor White
Write-Host "  6. Pressione Ctrl+C para parar" -ForegroundColor Red
Write-Host ""
Write-Host "LOGS ESPERADOS:" -ForegroundColor Magenta
Write-Host '  DEBUG_POPUP: RASTREAMENTO PANO - APOS ATUALIZAR MESA' -ForegroundColor White
Write-Host '  DEBUG_POPUP: CONSTRUINDO MesaAcerto DTO' -ForegroundColor White
Write-Host '  DEBUG_POPUP: MesaAcerto CONSTRUIDA' -ForegroundColor White
Write-Host '  DEBUG_POPUP: dadosAcerto FINAL (antes de salvar)' -ForegroundColor White
Write-Host ""

# Executar logcat com filtros atualizados (incluindo DEBUG_POPUP)
& $ADBPath logcat -v time -s DEBUG_POPUP:* DEBUG_FIX:* DEBUG_CARDS:* RegistrarTrocaPanoUseCase:* SettlementViewModel:* NovaReformaViewModel:* MesasReformadasViewModel:* AppRepository:* BaseViewModel:* AndroidRuntime:*
