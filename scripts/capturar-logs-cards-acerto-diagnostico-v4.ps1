# Script para capturar logs de diagnostico do problema de Cards do Acerto nao aparecerem
# Versao: 4.0 - Simplificado usando cmd.exe para evitar problemas PowerShell

Write-Host "=== CAPTURA DE LOGS - DIAGNOSTICO CARDS ACERTO ===" -ForegroundColor Yellow
Write-Host "Objetivo: Capturar TODAS as atividades para descobrir por que Cards do Acerto nao aparecem" -ForegroundColor Cyan
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
Write-Host "  CAPTURANDO LOGS DO DIAGNOSTICO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "INSTRUCOES:" -ForegroundColor Green
Write-Host "  1. Nova Reforma: Selecione mesa, marque Panos, salve" -ForegroundColor White
Write-Host "  2. Acerto: Selecione cliente, adicione mesa, marque Trocar Pano, salve" -ForegroundColor White
Write-Host "  3. Verifique tela Reforma de Mesas" -ForegroundColor White
Write-Host "  4. Pressione Ctrl+C para parar" -ForegroundColor Red
Write-Host ""

# Usar cmd.exe para executar comando (evita problemas PowerShell)
$cmd = "/C `"$ADBPath`" logcat -v time -s DEBUG_CARDS:* RegistrarTrocaPanoUseCase:* SettlementViewModel:* NovaReformaViewModel:* MesasReformadasViewModel:* AppRepository:* BaseViewModel:* AndroidRuntime:*"

# Executar via cmd.exe
cmd.exe /c $cmd
