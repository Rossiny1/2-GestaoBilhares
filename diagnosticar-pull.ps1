$adb = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "=== DIAGNOSTICANDO PROBLEMA DE PULL ===" -ForegroundColor Cyan
Write-Host ""

# Limpar logs
Write-Host "1. Limpando logs anteriores..." -ForegroundColor Yellow
& $adb logcat -c | Out-Null
Write-Host "   [OK] Logs limpos" -ForegroundColor Green
Write-Host ""

# Buscar logs específicos de pull
Write-Host "2. Buscando logs de PULL..." -ForegroundColor Yellow
$pullLogs = & $adb logcat -d | Select-String -Pattern "INICIANDO SINCRONIZAÇÃO PULL|Pull cancelada|dispositivo offline|Conectando ao Firestore|Iniciando pull de clientes|Pull Clientes" -CaseSensitive:$false
if ($pullLogs) {
    Write-Host "   Logs de PULL encontrados:" -ForegroundColor Green
    $pullLogs | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
} else {
    Write-Host "   [PROBLEMA] Nenhum log de PULL encontrado!" -ForegroundColor Red
    Write-Host "   Isso significa que syncPull() não está sendo executado ou está falhando silenciosamente" -ForegroundColor Yellow
}
Write-Host ""

# Verificar logs de erro relacionados
Write-Host "3. Buscando erros relacionados..." -ForegroundColor Yellow
$errors = & $adb logcat -d *:E | Select-String -Pattern "SyncRepository|Firestore|Firebase|NetworkUtils" -CaseSensitive:$false
if ($errors) {
    Write-Host "   Erros encontrados:" -ForegroundColor Red
    $errors | Select-Object -Last 10 | ForEach-Object { Write-Host "   $_" -ForegroundColor Red }
} else {
    Write-Host "   [OK] Nenhum erro encontrado" -ForegroundColor Green
}
Write-Host ""

# Verificar se há logs de warning sobre offline
Write-Host "4. Verificando avisos de offline..." -ForegroundColor Yellow
$warnings = & $adb logcat -d *:W | Select-String -Pattern "offline|Offline|OFFLINE|dispositivo offline|Pull cancelada" -CaseSensitive:$false
if ($warnings) {
    Write-Host "   Avisos encontrados:" -ForegroundColor Yellow
    $warnings | Select-Object -Last 10 | ForEach-Object { Write-Host "   $_" -ForegroundColor Yellow }
} else {
    Write-Host "   Nenhum aviso de offline encontrado" -ForegroundColor Gray
}
Write-Host ""

Write-Host "=== DIAGNOSTICO CONCLUIDO ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor Yellow
Write-Host "1. Execute a sincronização novamente no app" -ForegroundColor White
Write-Host "2. Execute este script novamente para ver os novos logs" -ForegroundColor White
Write-Host "3. Se não houver logs de PULL, o problema pode ser:" -ForegroundColor White
Write-Host "   - NetworkUtils.isConnected() retornando false" -ForegroundColor Yellow
Write-Host "   - syncPull() não está sendo chamado" -ForegroundColor Yellow
Write-Host "   - Exceção sendo capturada silenciosamente" -ForegroundColor Yellow

