# ========================================
# DEBUG CRASH IMPRESSORA KP-1025
# ========================================

Write-Host "Debug Crash Impressora KP-1025 - GestaoBilhares" -ForegroundColor Cyan

# Caminho do ADB
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se dispositivo está conectado
Write-Host "Verificando dispositivo..." -ForegroundColor Yellow
$devices = & $ADB devices
if ($devices.Count -lt 2) {
    Write-Host "ERRO: Nenhum dispositivo conectado!" -ForegroundColor Red
    exit 1
}

Write-Host "SUCCESS: Dispositivo conectado!" -ForegroundColor Green

# Limpar logs antigos
Write-Host "Limpando logs antigos..." -ForegroundColor Yellow
& $ADB logcat -c

# Iniciar captura de logs em background
Write-Host "Iniciando captura de logs..." -ForegroundColor Green
$logFile = "logcat-kp1025-crash-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
Start-Job -ScriptBlock {
    param($adb, $logFile)
    & $adb logcat -s "AndroidRuntime:E" "FATAL:*" "System.err:*" "BluetoothAdapter:*" "BluetoothSocket:*" "SettlementSummaryDialog:*" "BluetoothPrinterHelper:*" | Tee-Object -FilePath $logFile
} -ArgumentList $ADB, $logFile

Write-Host "Logs sendo capturados em: $logFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "INSTRUÇÕES PARA TESTE:" -ForegroundColor Yellow
Write-Host "1. Abra o app GestaoBilhares" -ForegroundColor White
Write-Host "2. Faça login" -ForegroundColor White
Write-Host "3. Vá para uma rota" -ForegroundColor White
Write-Host "4. Selecione um cliente" -ForegroundColor White
Write-Host "5. Faça um acerto" -ForegroundColor White
Write-Host "6. No resumo, clique em IMPRIMIR" -ForegroundColor White
Write-Host "7. Selecione a impressora KP-1025" -ForegroundColor Red
Write-Host "8. Aguarde o crash acontecer" -ForegroundColor Red
Write-Host ""
Write-Host "Após o crash, pressione ENTER para parar a captura e analisar os logs..." -ForegroundColor Red

Read-Host

# Parar a captura
Stop-Job -Name "*"
Remove-Job -Name "*"

Write-Host "Captura finalizada!" -ForegroundColor Green
Write-Host "Logs salvos em: $logFile" -ForegroundColor Cyan

# Mostrar últimos logs
Write-Host ""
Write-Host "ÚLTIMOS LOGS CAPTURADOS:" -ForegroundColor Yellow
Get-Content $logFile -Tail 30 