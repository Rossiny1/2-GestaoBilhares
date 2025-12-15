$adb = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "=== VERIFICANDO SINCRONIZACAO ===" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar se app está rodando
Write-Host "1. Verificando se app está rodando..." -ForegroundColor Yellow
$appRunning = & $adb shell "ps | grep gestaobilhares" 2>&1
if ($appRunning -match "gestaobilhares") {
    Write-Host "   [OK] App está rodando" -ForegroundColor Green
} else {
    Write-Host "   [ERRO] App NÃO está rodando!" -ForegroundColor Red
    Write-Host "   Inicie o app primeiro" -ForegroundColor Yellow
}
Write-Host ""

# 2. Buscar logs de sincronização
Write-Host "2. Buscando logs de sincronização..." -ForegroundColor Yellow
$syncLogs = & $adb logcat -d | Select-String -Pattern "SyncRepository|SyncWorker|syncPull|syncPush|sincronizacao" -CaseSensitive:$false
if ($syncLogs) {
    Write-Host "   Logs encontrados:" -ForegroundColor Green
    $syncLogs | Select-Object -Last 10 | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
} else {
    Write-Host "   [AVISO] Nenhum log de sincronização encontrado" -ForegroundColor Yellow
    Write-Host "   Isso pode significar que:" -ForegroundColor Yellow
    Write-Host "   - A sincronização não foi executada" -ForegroundColor Yellow
    Write-Host "   - O app não está gerando logs" -ForegroundColor Yellow
}
Write-Host ""

# 3. Buscar erros do app
Write-Host "3. Buscando erros do app..." -ForegroundColor Yellow
$errors = & $adb logcat -d *:E | Select-String -Pattern "gestaobilhares"
if ($errors) {
    Write-Host "   Erros encontrados:" -ForegroundColor Red
    $errors | Select-Object -Last 10 | ForEach-Object { Write-Host "   $_" -ForegroundColor Red }
} else {
    Write-Host "   [OK] Nenhum erro encontrado" -ForegroundColor Green
}
Write-Host ""

# 4. Verificar conectividade
Write-Host "4. Verificando conectividade..." -ForegroundColor Yellow
$network = & $adb shell "dumpsys connectivity | grep -i 'activeNetwork\|isConnected'" 2>&1
if ($network) {
    Write-Host "   Status de rede:" -ForegroundColor Cyan
    Write-Host "   $network" -ForegroundColor Gray
} else {
    Write-Host "   Não foi possível verificar" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "=== FIM DA VERIFICACAO ===" -ForegroundColor Cyan

