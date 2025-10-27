# ========================================
# CAPTURADOR DE LOGS DE SINCRONIZAÇÃO
# ========================================

Write-Host "Capturando logs de sincronização do GestaoBilhares..." -ForegroundColor Cyan

# Caminho do ADB
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Nome do pacote do app
$PACKAGE_NAME = "com.example.gestaobilhares"

# Verificar dispositivos
Write-Host "Verificando dispositivos..." -ForegroundColor Yellow
$devices = & $ADB devices
$deviceCount = ($devices | Where-Object { $_ -match "device$" }).Count

if ($deviceCount -eq 0) {
    Write-Host "Nenhum dispositivo conectado" -ForegroundColor Red
    exit 1
}

Write-Host "$deviceCount dispositivo(s) conectado(s)" -ForegroundColor Green

# Verificar app
Write-Host "Verificando app..." -ForegroundColor Yellow
$installedApps = & $ADB shell pm list packages $PACKAGE_NAME
if ($installedApps -match $PACKAGE_NAME) {
    Write-Host "App $PACKAGE_NAME encontrado" -ForegroundColor Green
} else {
    Write-Host "App $PACKAGE_NAME não encontrado" -ForegroundColor Red
    exit 1
}

# Limpar logs antigos
Write-Host "Limpando logs antigos..." -ForegroundColor Yellow
& $ADB logcat -c

Write-Host ""
Write-Host "INSTRUÇÕES:" -ForegroundColor Cyan
Write-Host "1. Abra o app e faça login" -ForegroundColor White
Write-Host "2. Cadastre um cliente" -ForegroundColor White
Write-Host "3. Clique no botão de sincronização" -ForegroundColor White
Write-Host "4. Aguarde 30 segundos" -ForegroundColor White
Write-Host "5. Pressione Ctrl+C para parar" -ForegroundColor Yellow
Write-Host ""

# Capturar logs específicos de sincronização
Write-Host "Capturando logs de sincronização..." -ForegroundColor Green
Write-Host "Execute os passos acima agora!" -ForegroundColor Yellow

# Aguardar um pouco para o usuário executar os passos
Start-Sleep -Seconds 5

# Capturar logs com filtros específicos para sincronização
$OutputFile = "logcat-sync-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
& $ADB logcat -v time -s "SyncManagerV2:*" "RoutesFragment:*" "AppRepository:*" "FirebaseAuth:*" "FirebaseFirestore:*" "$PACKAGE_NAME:*" | Tee-Object -FilePath $OutputFile

Write-Host ""
Write-Host "Logs capturados em: $OutputFile" -ForegroundColor Green

# Análise básica
if (Test-Path $OutputFile) {
    $logContent = Get-Content $OutputFile -Raw
    $syncManagerLogs = ($logContent | Select-String -Pattern "SyncManagerV2" -AllMatches).Matches.Count
    $routesFragmentLogs = ($logContent | Select-String -Pattern "RoutesFragment" -AllMatches).Matches.Count
    $firebaseLogs = ($logContent | Select-String -Pattern "Firebase" -AllMatches).Matches.Count
    $errorCount = ($logContent | Select-String -Pattern "ERROR|Exception|Failed" -AllMatches).Matches.Count

    Write-Host ""
    Write-Host "ANÁLISE DOS LOGS:" -ForegroundColor Cyan
    Write-Host "   - Logs SyncManagerV2: $syncManagerLogs" -ForegroundColor $(if ($syncManagerLogs -gt 0) { "Green" } else { "Red" })
    Write-Host "   - Logs RoutesFragment: $routesFragmentLogs" -ForegroundColor $(if ($routesFragmentLogs -gt 0) { "Green" } else { "Red" })
    Write-Host "   - Logs Firebase: $firebaseLogs" -ForegroundColor $(if ($firebaseLogs -gt 0) { "Green" } else { "Red" })
    Write-Host "   - Erros encontrados: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })

    # Verificar empresa_id
    $empresaIdLogs = $logContent | Select-String -Pattern "Empresa ID|empresa_id" -AllMatches
    if ($empresaIdLogs) {
        Write-Host "   - Empresa ID: CONFIGURADO" -ForegroundColor Green
    } else {
        Write-Host "   - Empresa ID: NÃO ENCONTRADO" -ForegroundColor Red
    }

    # Verificar fila de sincronização
    $queueLogs = $logContent | Select-String -Pattern "Fila de sincronização|operacoes|PENDING" -AllMatches
    if ($queueLogs) {
        Write-Host "   - Fila de sincronização: ENCONTRADA" -ForegroundColor Green
    } else {
        Write-Host "   - Fila de sincronização: NÃO ENCONTRADA" -ForegroundColor Red
    }

    # Verificar operações Firestore
    $firestoreLogs = $logContent | Select-String -Pattern "Firestore|SET executado|DELETE executado|Firestore Path" -AllMatches
    if ($firestoreLogs) {
        Write-Host "   - Operações Firestore: ENCONTRADAS" -ForegroundColor Green
    } else {
        Write-Host "   - Operações Firestore: NÃO ENCONTRADAS" -ForegroundColor Red
    }

    Write-Host ""
    Write-Host "DIAGNÓSTICO:" -ForegroundColor Cyan
    if ($syncManagerLogs -eq 0) {
        Write-Host "❌ PROBLEMA: SyncManagerV2 não está sendo executado" -ForegroundColor Red
    }
    if ($queueLogs -eq 0) {
        Write-Host "❌ PROBLEMA: Nenhuma operação na fila de sincronização" -ForegroundColor Red
    }
    if ($firestoreLogs -eq 0) {
        Write-Host "❌ PROBLEMA: Operações não estão chegando ao Firestore" -ForegroundColor Red
    }
    if ($syncManagerLogs -gt 0 -and $queueLogs -gt 0 -and $firestoreLogs -gt 0 -and $errorCount -eq 0) {
        Write-Host "✅ SINCRONIZAÇÃO FUNCIONANDO: Todos os componentes estão operando" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Arquivo de logs: $OutputFile" -ForegroundColor White
Write-Host "Abra o arquivo para análise detalhada" -ForegroundColor White 