# ========================================
# CAPTURA DE LOGS DE SINCRONIZAÇÃO
# ========================================

param(
    [string]$PackageName = "com.example.gestaobilhares",
    [int]$TimeoutSeconds = 60
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE LOGS DE SINCRONIZAÇÃO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar ADB
Write-Host "1. Verificando ADB..." -ForegroundColor Yellow
try {
    if (Test-Path $ADB) {
        Write-Host "ADB encontrado em: $ADB" -ForegroundColor Green
    } else {
        Write-Host "ADB nao encontrado em: $ADB" -ForegroundColor Red
        Write-Host "Tentando adb global..." -ForegroundColor Yellow
        $adbVersion = & adb version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "ADB encontrado globalmente" -ForegroundColor Green
            $ADB = "adb"
        } else {
            Write-Host "ADB nao encontrado" -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "Erro ao verificar ADB" -ForegroundColor Red
    exit 1
}

# Verificar dispositivos
Write-Host "2. Verificando dispositivos..." -ForegroundColor Yellow
$devices = & $ADB devices 2>$null
$deviceCount = ($devices | Where-Object { $_ -match "device$" }).Count

if ($deviceCount -eq 0) {
    Write-Host "Nenhum dispositivo conectado" -ForegroundColor Red
    exit 1
}

Write-Host "$deviceCount dispositivo(s) conectado(s)" -ForegroundColor Green

# Limpar logs
Write-Host "3. Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c 2>$null
Write-Host "Logs anteriores limpos" -ForegroundColor Green

# Verificar app
Write-Host "4. Verificando app..." -ForegroundColor Yellow
$installedApps = & $ADB shell pm list packages $PackageName 2>$null
if ($installedApps -match $PackageName) {
    Write-Host "App $PackageName encontrado" -ForegroundColor Green
} else {
    Write-Host "App $PackageName nao encontrado" -ForegroundColor Red
    exit 1
}

# Gerar nome do arquivo
$OutputFile = "logcat-sync-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

Write-Host ""
Write-Host "5. INICIANDO CAPTURA DE LOGS DE SINCRONIZAÇÃO..." -ForegroundColor Cyan
Write-Host "   - Pressione Ctrl+C para parar" -ForegroundColor White
Write-Host "   - Abra o app e faça login" -ForegroundColor White
Write-Host "   - Cadastre um cliente" -ForegroundColor White
Write-Host "   - Clique no botão de sincronização" -ForegroundColor White
Write-Host "   - Aguarde $TimeoutSeconds segundos" -ForegroundColor White
Write-Host ""

# Capturar logs
Write-Host "CAPTURA INICIADA - Execute os passos acima!" -ForegroundColor Green
Write-Host ""

try {
    $elapsed = 0
    while ($elapsed -lt $TimeoutSeconds) {
        Start-Sleep -Seconds 1
        $elapsed++
        $remaining = $TimeoutSeconds - $elapsed
        Write-Progress -Activity "Capturando logs de sincronização" -Status "Aguardando operações..." -PercentComplete (($elapsed / $TimeoutSeconds) * 100) -CurrentOperation "Tempo restante: $remaining segundos"
    }
} catch {
    Write-Host "Captura interrompida" -ForegroundColor Yellow
} finally {
    Write-Progress -Activity "Capturando logs de sincronização" -Completed
}

# Capturar logs com filtros específicos para sincronização
Write-Host "6. Capturando logs de sincronização..." -ForegroundColor Yellow
& $ADB logcat -v time -s SyncManagerV2:V RoutesFragment:V AppRepository:V FirebaseAuth:V FirebaseFirestore:V $PackageName:V *:S > $OutputFile 2>&1

# Verificar arquivo
if (Test-Path $OutputFile) {
    $fileSize = (Get-Item $OutputFile).Length
    Write-Host "Logs capturados: $OutputFile ($fileSize bytes)" -ForegroundColor Green
} else {
    Write-Host "Nenhum log foi capturado" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "7. ANALISANDO LOGS DE SINCRONIZAÇÃO..." -ForegroundColor Cyan

# Análise específica para sincronização
$logContent = Get-Content $OutputFile -Raw
$syncManagerLogs = ($logContent | Select-String -Pattern "SyncManagerV2" -AllMatches).Matches.Count
$routesFragmentLogs = ($logContent | Select-String -Pattern "RoutesFragment" -AllMatches).Matches.Count
$firebaseLogs = ($logContent | Select-String -Pattern "Firebase" -AllMatches).Matches.Count
$errorCount = ($logContent | Select-String -Pattern "ERROR|Exception|Failed" -AllMatches).Matches.Count

Write-Host "ESTATISTICAS DE SINCRONIZAÇÃO:" -ForegroundColor White
Write-Host "   - Logs SyncManagerV2: $syncManagerLogs" -ForegroundColor $(if ($syncManagerLogs -gt 0) { "Green" } else { "Red" })
Write-Host "   - Logs RoutesFragment: $routesFragmentLogs" -ForegroundColor $(if ($routesFragmentLogs -gt 0) { "Green" } else { "Red" })
Write-Host "   - Logs Firebase: $firebaseLogs" -ForegroundColor $(if ($firebaseLogs -gt 0) { "Green" } else { "Red" })
Write-Host "   - Erros encontrados: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
Write-Host "   - Tamanho do arquivo: $fileSize bytes" -ForegroundColor White

# Análise específica de sincronização
Write-Host ""
Write-Host "ANALISE DE SINCRONIZAÇÃO:" -ForegroundColor Cyan

# Verificar empresa_id
$empresaIdLogs = $logContent | Select-String -Pattern "Empresa ID|empresa_id" -AllMatches
if ($empresaIdLogs) {
    Write-Host "EMPRESA ID CONFIGURADO:" -ForegroundColor Green
    $empresaIdLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Green }
} else {
    Write-Host "EMPRESA ID: NÃO ENCONTRADO" -ForegroundColor Red
}

# Verificar fila de sincronização
$queueLogs = $logContent | Select-String -Pattern "Fila de sincronização|operacoes|PENDING" -AllMatches
if ($queueLogs) {
    Write-Host "FILA DE SINCRONIZAÇÃO:" -ForegroundColor Yellow
    $queueLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Yellow }
} else {
    Write-Host "FILA DE SINCRONIZAÇÃO: NÃO ENCONTRADA" -ForegroundColor Red
}

# Verificar operações Firestore
$firestoreLogs = $logContent | Select-String -Pattern "Firestore|SET executado|DELETE executado|Firestore Path" -AllMatches
if ($firestoreLogs) {
    Write-Host "OPERACOES FIRESTORE:" -ForegroundColor Green
    $firestoreLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Green }
} else {
    Write-Host "OPERACOES FIRESTORE: NÃO ENCONTRADAS" -ForegroundColor Red
}

# Verificar autenticação
$authLogs = $logContent | Select-String -Pattern "FirebaseAuth|currentUser|autenticado" -AllMatches
if ($authLogs) {
    Write-Host "AUTENTICACAO:" -ForegroundColor Green
    $authLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Green }
} else {
    Write-Host "AUTENTICACAO: NÃO ENCONTRADA" -ForegroundColor Red
}

# Verificar payload
$payloadLogs = $logContent | Select-String -Pattern "Payload|payload" -AllMatches
if ($payloadLogs) {
    Write-Host "PAYLOAD DETECTADO:" -ForegroundColor Yellow
    $payloadLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Yellow }
} else {
    Write-Host "PAYLOAD: NÃO ENCONTRADO" -ForegroundColor Red
}

Write-Host ""
Write-Host "DIAGNOSTICO:" -ForegroundColor Cyan

# Diagnóstico baseado nos logs
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

if ($errorCount -gt 0) {
    Write-Host "❌ PROBLEMA: Erros detectados durante sincronização" -ForegroundColor Red
    Write-Host "   SOLUÇÃO: Verifique os erros específicos acima" -ForegroundColor White
}

if ($syncManagerLogs -gt 0 -and $queueLogs -gt 0 -and $firestoreLogs -gt 0 -and $errorCount -eq 0) {
    Write-Host "✅ SINCRONIZAÇÃO FUNCIONANDO: Todos os componentes estão operando" -ForegroundColor Green
    Write-Host "   VERIFICAÇÃO: Confira o console do Firestore para ver os dados" -ForegroundColor White
}

Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "   1. Abra o arquivo: $OutputFile" -ForegroundColor White
Write-Host "   2. Procure por 'SyncManagerV2' para ver o fluxo de sincronização" -ForegroundColor White
Write-Host "   3. Verifique se 'empresa_id' está correto (deve ser 'empresa_001')" -ForegroundColor White
Write-Host "   4. Confirme se há operações na fila (PENDING)" -ForegroundColor White
Write-Host "   5. Verifique se as operações chegam ao Firestore" -ForegroundColor White
Write-Host "   6. Se tudo estiver OK, verifique o console do Firestore" -ForegroundColor White

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE LOGS DE SINCRONIZAÇÃO CONCLUIDA!" -ForegroundColor Green
Write-Host "Arquivo: $OutputFile" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
