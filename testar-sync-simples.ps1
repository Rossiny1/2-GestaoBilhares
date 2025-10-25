# Teste simples de sincronização - GestaoBilhares
# Baseado no script que funciona

Write-Host "TESTE DE SINCRONIZAÇÃO SIMPLES" -ForegroundColor Cyan
Write-Host "===============================" -ForegroundColor Cyan

# Caminho do ADB (mesmo dos scripts que funcionam)
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $ADB)) { $ADB = "adb" }

# Verificar ADB
try {
    $null = & $ADB version 2>$null
    if ($LASTEXITCODE -ne 0) { throw "ADB não encontrado" }
    Write-Host "ADB: $ADB" -ForegroundColor Green
} catch {
    Write-Host "Erro: $_" -ForegroundColor Red
    exit 1
}

# Verificar dispositivo
Write-Host "Verificando dispositivo..." -ForegroundColor Yellow
$devices = & $ADB devices
$deviceCount = ($devices | Where-Object { $_ -match "device$" }).Count

if ($deviceCount -eq 0) {
    Write-Host "Nenhum dispositivo conectado" -ForegroundColor Red
    exit 1
}

Write-Host "$deviceCount dispositivo(s) conectado(s)" -ForegroundColor Green

# Verificar app
Write-Host "Verificando app..." -ForegroundColor Yellow
$installedApps = & $ADB shell pm list packages com.example.gestaobilhares
if ($installedApps -match "com.example.gestaobilhares") {
    Write-Host "App encontrado" -ForegroundColor Green
} else {
    Write-Host "App não encontrado" -ForegroundColor Red
    exit 1
}

# Limpar logs
Write-Host "Limpando logs..." -ForegroundColor Yellow
& $ADB logcat -c

Write-Host ""
Write-Host "INSTRUÇÕES:" -ForegroundColor Cyan
Write-Host "1. Abra o app GestaoBilhares" -ForegroundColor White
Write-Host "2. Faça login" -ForegroundColor White
Write-Host "3. Cadastre um cliente" -ForegroundColor White
Write-Host "4. Clique no botão de sincronização" -ForegroundColor White
Write-Host "5. Aguarde 20 segundos" -ForegroundColor White
Write-Host ""

# Aguardar
Write-Host "Aguardando 5 segundos para você executar os passos..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Capturar logs diretamente (sem jobs)
$OutputFile = "teste-sync-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
Write-Host "Capturando logs por 20 segundos..." -ForegroundColor Green
Write-Host "Execute os passos acima agora!" -ForegroundColor Yellow

# Usar timeout para capturar por 20 segundos
$process = Start-Process -FilePath $ADB -ArgumentList "logcat", "-v", "time", "-s", "SyncManagerV2:V", "RoutesFragment:V", "FirebaseAuth:V", "com.example.gestaobilhares:V", "*:S" -RedirectStandardOutput $OutputFile -NoNewWindow -PassThru

Start-Sleep -Seconds 20

# Parar o processo
$process.Kill()
$process.WaitForExit()

Write-Host "Captura finalizada!" -ForegroundColor Green

# Analisar logs
if (Test-Path $OutputFile) {
    $logContent = Get-Content $OutputFile -Raw
    
    Write-Host ""
    Write-Host "ANÁLISE DOS LOGS:" -ForegroundColor Cyan
    
    # Contar logs por categoria
    $syncManagerLogs = ($logContent | Select-String -Pattern "SyncManagerV2" -AllMatches).Matches.Count
    $routesFragmentLogs = ($logContent | Select-String -Pattern "RoutesFragment" -AllMatches).Matches.Count
    $firebaseLogs = ($logContent | Select-String -Pattern "Firebase" -AllMatches).Matches.Count
    $errorCount = ($logContent | Select-String -Pattern "ERROR|Exception|Failed" -AllMatches).Matches.Count
    
    Write-Host "   - Logs SyncManagerV2: $syncManagerLogs" -ForegroundColor $(if ($syncManagerLogs -gt 0) { "Green" } else { "Red" })
    Write-Host "   - Logs RoutesFragment: $routesFragmentLogs" -ForegroundColor $(if ($routesFragmentLogs -gt 0) { "Green" } else { "Red" })
    Write-Host "   - Logs Firebase: $firebaseLogs" -ForegroundColor $(if ($firebaseLogs -gt 0) { "Green" } else { "Red" })
    Write-Host "   - Erros encontrados: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
    
    # Verificar empresa_id
    $empresaIdLogs = $logContent | Select-String -Pattern "Empresa ID|empresa_id" -AllMatches
    if ($empresaIdLogs) {
        Write-Host "   - Empresa ID: CONFIGURADO" -ForegroundColor Green
        $empresaIdLogs | ForEach-Object { Write-Host "     $($_.Line)" -ForegroundColor Gray }
    } else {
        Write-Host "   - Empresa ID: NÃO ENCONTRADO" -ForegroundColor Red
    }
    
    # Verificar fila de sincronização
    $queueLogs = $logContent | Select-String -Pattern "Fila de sincronização|operacoes|PENDING" -AllMatches
    if ($queueLogs) {
        Write-Host "   - Fila de sincronização: ENCONTRADA" -ForegroundColor Green
        $queueLogs | ForEach-Object { Write-Host "     $($_.Line)" -ForegroundColor Gray }
    } else {
        Write-Host "   - Fila de sincronização: NÃO ENCONTRADA" -ForegroundColor Red
    }
    
    # Verificar operações Firestore
    $firestoreLogs = $logContent | Select-String -Pattern "Firestore|SET executado|DELETE executado|Firestore Path" -AllMatches
    if ($firestoreLogs) {
        Write-Host "   - Operações Firestore: ENCONTRADAS" -ForegroundColor Green
        $firestoreLogs | ForEach-Object { Write-Host "     $($_.Line)" -ForegroundColor Gray }
    } else {
        Write-Host "   - Operações Firestore: NÃO ENCONTRADAS" -ForegroundColor Red
    }
    
    # Verificar autenticação
    $authLogs = $logContent | Select-String -Pattern "FirebaseAuth|currentUser|autenticado" -AllMatches
    if ($authLogs) {
        Write-Host "   - Autenticação: DETECTADA" -ForegroundColor Green
        $authLogs | ForEach-Object { Write-Host "     $($_.Line)" -ForegroundColor Gray }
    } else {
        Write-Host "   - Autenticação: NÃO DETECTADA" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "DIAGNÓSTICO:" -ForegroundColor Cyan
    if ($syncManagerLogs -eq 0) {
        Write-Host "❌ PROBLEMA: SyncManagerV2 não está sendo executado" -ForegroundColor Red
        Write-Host "   SOLUÇÃO: Verifique se o botão de sincronização está funcionando" -ForegroundColor White
    }
    if ($queueLogs -eq 0) {
        Write-Host "❌ PROBLEMA: Nenhuma operação na fila de sincronização" -ForegroundColor Red
        Write-Host "   SOLUÇÃO: Cadastre um cliente primeiro para gerar operações" -ForegroundColor White
    }
    if ($firestoreLogs -eq 0) {
        Write-Host "❌ PROBLEMA: Operações não estão chegando ao Firestore" -ForegroundColor Red
        Write-Host "   SOLUÇÃO: Verifique empresa_id e autenticação" -ForegroundColor White
    }
    if ($authLogs -eq 0) {
        Write-Host "❌ PROBLEMA: Usuário não está autenticado no Firebase" -ForegroundColor Red
        Write-Host "   SOLUÇÃO: Faça login no app primeiro" -ForegroundColor White
    }
    if ($syncManagerLogs -gt 0 -and $queueLogs -gt 0 -and $firestoreLogs -gt 0 -and $errorCount -eq 0) {
        Write-Host "✅ SINCRONIZAÇÃO FUNCIONANDO: Todos os componentes estão operando" -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "Arquivo de logs: $OutputFile" -ForegroundColor White
    Write-Host "Abra o arquivo para análise detalhada" -ForegroundColor White
    
} else {
    Write-Host "Falha ao capturar logs" -ForegroundColor Red
}

Write-Host ""
Write-Host "TESTE CONCLUÍDO!" -ForegroundColor Green
