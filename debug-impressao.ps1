# Script para capturar logs de debug da impressão Bluetooth
# Desenvolvido para debug da funcionalidade de impressão térmica

$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "INICIANDO CAPTURADOR DE LOGS DE IMPRESSAO BLUETOOTH" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Parar captura anterior se estiver rodando
Write-Host "PARANDO CAPTURAS ANTERIORES..." -ForegroundColor Yellow
& $ADB logcat -c

# Capturar logs específicos de impressão
Write-Host "INICIANDO CAPTURA DE LOGS..." -ForegroundColor Green
Write-Host "FILTROS APLICADOS:" -ForegroundColor Yellow
Write-Host "   - SettlementSummaryDialog" -ForegroundColor White
Write-Host "   - BluetoothPrinterHelper" -ForegroundColor White
Write-Host "   - Bluetooth" -ForegroundColor White
Write-Host "   - Error" -ForegroundColor Red

Write-Host ""
Write-Host "INSTRUCOES:" -ForegroundColor Cyan
Write-Host "1. Execute este script" -ForegroundColor White
Write-Host "2. No app, tente imprimir um recibo" -ForegroundColor White
Write-Host "3. Observe os logs em tempo real" -ForegroundColor White
Write-Host "4. Pressione Ctrl+C para parar" -ForegroundColor White
Write-Host ""

# Capturar logs com filtros específicos
& $ADB logcat -v time | Select-String -Pattern "SettlementSummaryDialog|BluetoothPrinterHelper|Bluetooth|Error|Exception|FATAL|AndroidRuntime" 