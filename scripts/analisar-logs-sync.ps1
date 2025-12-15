# Script para analisar logs de sincronização
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE DE LOGS DE SINCRONIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se ADB existe
if (-not (Test-Path $adbPath)) {
    Write-Host "[ERRO] ADB não encontrado em: $adbPath" -ForegroundColor Red
    Write-Host "Verifique o caminho do Android SDK" -ForegroundColor Yellow
    exit 1
}

# Verificar dispositivo
Write-Host "Verificando dispositivo..." -ForegroundColor Yellow
$devices = & $adbPath devices 2>&1
if ($devices -notmatch "device$") {
    Write-Host "[ERRO] Nenhum dispositivo conectado!" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Dispositivo conectado" -ForegroundColor Green
Write-Host ""

# Capturar logs do app
Write-Host "Capturando logs do app GestaoBilhares..." -ForegroundColor Yellow
$packageName = "com.example.gestaobilhares"

# Logs de sincronização
Write-Host "`n=== LOGS DE SINCRONIZACAO ===" -ForegroundColor Cyan
$syncLogs = & $adbPath logcat -d -s SyncRepository:* SyncWorker:* SyncManager:* RoutesFragment:* AppRepository:* 2>&1
if ($syncLogs) {
    $syncLogs | ForEach-Object { Write-Host $_ -ForegroundColor White }
} else {
    Write-Host "Nenhum log de sincronização encontrado" -ForegroundColor Yellow
}

# Logs de erro do app
Write-Host "`n=== ERROS DO APP ===" -ForegroundColor Red
$errorLogs = & $adbPath logcat -d *:E | Select-String -Pattern $packageName
if ($errorLogs) {
    $errorLogs | ForEach-Object { Write-Host $_ -ForegroundColor Red }
} else {
    Write-Host "Nenhum erro encontrado" -ForegroundColor Green
}

# Verificar se app está rodando
Write-Host "`n=== STATUS DO APP ===" -ForegroundColor Cyan
$runningApps = & $adbPath shell "ps | grep $packageName" 2>&1
if ($runningApps -match $packageName) {
    Write-Host "App está rodando" -ForegroundColor Green
    Write-Host $runningApps -ForegroundColor Gray
} else {
    Write-Host "App NÃO está rodando!" -ForegroundColor Red
    Write-Host "Inicie o app primeiro" -ForegroundColor Yellow
}

# Verificar logs de Firebase/Firestore
Write-Host "`n=== LOGS FIREBASE/FIRESTORE ===" -ForegroundColor Cyan
$firebaseLogs = & $adbPath logcat -d | Select-String -Pattern "Firebase|Firestore|FIREBASE|FIRESTORE" -Context 0,1
if ($firebaseLogs) {
    $firebaseLogs | Select-Object -Last 20 | ForEach-Object { Write-Host $_ -ForegroundColor Magenta }
} else {
    Write-Host "Nenhum log do Firebase encontrado" -ForegroundColor Yellow
}

# Verificar conectividade
Write-Host "`n=== VERIFICACAO DE CONECTIVIDADE ===" -ForegroundColor Cyan
$networkInfo = & $adbPath shell "dumpsys connectivity" 2>&1 | Select-String -Pattern "activeNetwork|isConnected" | Select-Object -First 5
if ($networkInfo) {
    Write-Host $networkInfo -ForegroundColor Gray
} else {
    Write-Host "Não foi possível verificar conectividade" -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "ANALISE CONCLUIDA" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

