$adb = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$logFile = "logs-sync-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

Write-Host "Capturando logs de sincronização..." -ForegroundColor Cyan

# Capturar todos os logs do SyncRepository
$logs = & $adb logcat -d | Select-String -Pattern "SyncRepository|syncPull|syncPush|NetworkUtils|Firestore|Firebase" -CaseSensitive:$false

if ($logs) {
    $logs | Out-File -FilePath $logFile -Encoding UTF8
    Write-Host "Logs salvos em: $logFile" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== ÚLTIMOS 30 LOGS ===" -ForegroundColor Cyan
    $logs | Select-Object -Last 30 | ForEach-Object { Write-Host $_ }
} else {
    Write-Host "Nenhum log encontrado" -ForegroundColor Yellow
}

