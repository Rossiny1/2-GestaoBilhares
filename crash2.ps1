# ========================================
# CAPTURA DE CRASH DE TELAS DO APP
# ========================================

param(
    [string]$PackageName = "com.example.gestaobilhares",
    [int]$TimeoutSeconds = 30
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE CRASH DE TELAS DO APP" -ForegroundColor Cyan
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
$OutputFile = "logcat-crash-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

Write-Host ""
Write-Host "5. INICIANDO CAPTURA DE CRASH DE TELAS..." -ForegroundColor Cyan
Write-Host "   - Pressione Ctrl+C para parar" -ForegroundColor White
Write-Host "   - Navegue pelas telas do app" -ForegroundColor White
Write-Host "   - Tente reproduzir o crash" -ForegroundColor White
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
        Write-Progress -Activity "Capturando logs de crash" -Status "Aguardando operações..." -PercentComplete (($elapsed / $TimeoutSeconds) * 100) -CurrentOperation "Tempo restante: $remaining segundos"
    }
} catch {
    Write-Host "Captura interrompida" -ForegroundColor Yellow
} finally {
    Write-Progress -Activity "Capturando logs de crash" -Completed
}

# Capturar logs com filtros específicos para crash
Write-Host "6. Capturando logs de crash..." -ForegroundColor Yellow
& $ADB logcat -v time -s AndroidRuntime:E System.err:E $PackageName:E AndroidRuntime:W System.err:W $PackageName:W *:S > $OutputFile 2>&1

# Verificar arquivo
if (Test-Path $OutputFile) {
    $fileSize = (Get-Item $OutputFile).Length
    Write-Host "Logs capturados: $OutputFile ($fileSize bytes)" -ForegroundColor Green
} else {
    Write-Host "Nenhum log foi capturado" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "7. ANALISANDO LOGS DE CRASH..." -ForegroundColor Cyan

# Análise específica para crash
$logContent = Get-Content $OutputFile -Raw
$androidRuntimeErrors = ($logContent | Select-String -Pattern "AndroidRuntime.*ERROR" -AllMatches).Matches.Count
$systemErrors = ($logContent | Select-String -Pattern "System.err.*ERROR" -AllMatches).Matches.Count
$appErrors = ($logContent | Select-String -Pattern "$PackageName.*ERROR" -AllMatches).Matches.Count
$fatalErrors = ($logContent | Select-String -Pattern "FATAL|Fatal" -AllMatches).Matches.Count
$exceptionCount = ($logContent | Select-String -Pattern "Exception|exception" -AllMatches).Matches.Count
$crashCount = ($logContent | Select-String -Pattern "CRASH|Crash|crash" -AllMatches).Matches.Count

Write-Host "ESTATISTICAS DE CRASH:" -ForegroundColor White
Write-Host "   - Erros AndroidRuntime: $androidRuntimeErrors" -ForegroundColor $(if ($androidRuntimeErrors -gt 0) { "Red" } else { "Green" })
Write-Host "   - Erros System.err: $systemErrors" -ForegroundColor $(if ($systemErrors -gt 0) { "Red" } else { "Green" })
Write-Host "   - Erros do App: $appErrors" -ForegroundColor $(if ($appErrors -gt 0) { "Red" } else { "Green" })
Write-Host "   - Erros Fatais: $fatalErrors" -ForegroundColor $(if ($fatalErrors -gt 0) { "Red" } else { "Green" })
Write-Host "   - Exceções: $exceptionCount" -ForegroundColor $(if ($exceptionCount -gt 0) { "Red" } else { "Green" })
Write-Host "   - Crashes: $crashCount" -ForegroundColor $(if ($crashCount -gt 0) { "Red" } else { "Green" })
Write-Host "   - Tamanho do arquivo: $fileSize bytes" -ForegroundColor White

# Análise específica de crash
Write-Host ""
Write-Host "ANALISE DE CRASH:" -ForegroundColor Cyan

# Verificar erros AndroidRuntime
$androidRuntimeLogs = $logContent | Select-String -Pattern "AndroidRuntime.*ERROR" -AllMatches
if ($androidRuntimeLogs) {
    Write-Host "ERROS ANDROIDRUNTIME:" -ForegroundColor Red
    $androidRuntimeLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
} else {
    Write-Host "ERROS ANDROIDRUNTIME: NÃO ENCONTRADOS" -ForegroundColor Green
}

# Verificar erros System.err
$systemErrLogs = $logContent | Select-String -Pattern "System.err.*ERROR" -AllMatches
if ($systemErrLogs) {
    Write-Host "ERROS SYSTEM.ERR:" -ForegroundColor Red
    $systemErrLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
} else {
    Write-Host "ERROS SYSTEM.ERR: NÃO ENCONTRADOS" -ForegroundColor Green
}

# Verificar erros do app
$appErrorLogs = $logContent | Select-String -Pattern "$PackageName.*ERROR" -AllMatches
if ($appErrorLogs) {
    Write-Host "ERROS DO APP:" -ForegroundColor Red
    $appErrorLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
} else {
    Write-Host "ERROS DO APP: NÃO ENCONTRADOS" -ForegroundColor Green
}

# Verificar exceções
$exceptionLogs = $logContent | Select-String -Pattern "Exception|exception" -AllMatches
if ($exceptionLogs) {
    Write-Host "EXCEÇÕES DETECTADAS:" -ForegroundColor Yellow
    $exceptionLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Yellow }
} else {
    Write-Host "EXCEÇÕES: NÃO ENCONTRADAS" -ForegroundColor Green
}

# Verificar crashes
$crashLogs = $logContent | Select-String -Pattern "CRASH|Crash|crash" -AllMatches
if ($crashLogs) {
    Write-Host "CRASHES DETECTADOS:" -ForegroundColor Red
    $crashLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
} else {
    Write-Host "CRASHES: NÃO ENCONTRADOS" -ForegroundColor Green
}

Write-Host ""
Write-Host "DIAGNOSTICO:" -ForegroundColor Cyan

# Diagnóstico baseado nos logs
if ($androidRuntimeErrors -gt 0) {
    Write-Host "❌ PROBLEMA: Erros AndroidRuntime detectados" -ForegroundColor Red
    Write-Host "   SOLUÇÃO: Verifique os logs AndroidRuntime acima" -ForegroundColor White
}

if ($systemErrors -gt 0) {
    Write-Host "❌ PROBLEMA: Erros System.err detectados" -ForegroundColor Red
    Write-Host "   SOLUÇÃO: Verifique os logs System.err acima" -ForegroundColor White
}

if ($appErrors -gt 0) {
    Write-Host "❌ PROBLEMA: Erros específicos do app detectados" -ForegroundColor Red
    Write-Host "   SOLUÇÃO: Verifique os logs do app acima" -ForegroundColor White
}

if ($fatalErrors -gt 0) {
    Write-Host "❌ PROBLEMA: Erros fatais detectados" -ForegroundColor Red
    Write-Host "   SOLUÇÃO: Verifique os logs fatais acima" -ForegroundColor White
}

if ($exceptionCount -gt 0) {
    Write-Host "❌ PROBLEMA: Exceções detectadas" -ForegroundColor Red
    Write-Host "   SOLUÇÃO: Verifique as exceções acima" -ForegroundColor White
}

if ($crashCount -gt 0) {
    Write-Host "❌ PROBLEMA: Crashes detectados" -ForegroundColor Red
    Write-Host "   SOLUÇÃO: Verifique os crashes acima" -ForegroundColor White
}

if ($androidRuntimeErrors -eq 0 -and $systemErrors -eq 0 -and $appErrors -eq 0 -and $fatalErrors -eq 0 -and $exceptionCount -eq 0 -and $crashCount -eq 0) {
    Write-Host "✅ NENHUM CRASH DETECTADO: App funcionando normalmente" -ForegroundColor Green
    Write-Host "   VERIFICAÇÃO: Continue testando outras funcionalidades" -ForegroundColor White
}

Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "   1. Abra o arquivo: $OutputFile" -ForegroundColor White
Write-Host "   2. Procure por 'AndroidRuntime' para ver erros do sistema" -ForegroundColor White
Write-Host "   3. Procure por 'System.err' para ver erros gerais" -ForegroundColor White
Write-Host "   4. Procure por '$PackageName' para ver erros específicos do app" -ForegroundColor White
Write-Host "   5. Procure por 'Exception' para ver exceções" -ForegroundColor White
Write-Host "   6. Procure por 'FATAL' para ver erros fatais" -ForegroundColor White
Write-Host "   7. Se encontrar erros, analise o stack trace" -ForegroundColor White

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE CRASH DE TELAS CONCLUIDA!" -ForegroundColor Green
Write-Host "Arquivo: $OutputFile" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
