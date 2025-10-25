# Script simples para testar PULL SYNC
Write-Host "🧪 TESTE PULL SYNC - Sincronização Bidirecional" -ForegroundColor Cyan

$adbPath = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "📱 Verificando dispositivo..." -ForegroundColor Yellow
& $adbPath devices

Write-Host "`n🔄 Capturando logs PULL SYNC..." -ForegroundColor Yellow
Write-Host "   Execute a sincronização no app e observe os logs" -ForegroundColor Gray

& $adbPath logcat -s SyncManagerV2:V | findstr "PULL pull Baixando Cliente sincronizado Empresa ID Firestore"
