# Script simples para capturar logs PULL SYNC
Write-Host "🧪 LOGS PULL SYNC" -ForegroundColor Cyan

$adbPath = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "📱 Verificando dispositivo..." -ForegroundColor Yellow
& $adbPath devices

Write-Host "`n🔄 Capturando logs..." -ForegroundColor Yellow
Write-Host "   Execute a sincronização no app" -ForegroundColor Gray

& $adbPath logcat -s SyncManagerV2:V
