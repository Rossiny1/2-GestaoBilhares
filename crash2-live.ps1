# ========================================
# CAPTURA DE CRASH DE TELAS DO APP - TEMPO REAL
# ========================================

param(
    [string]$PackageName = "com.example.gestaobilhares",
    [int]$TimeoutSeconds = 30
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE CRASH DE TELAS DO APP - TEMPO REAL" -ForegroundColor Cyan
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

Write-Host ""
Write-Host "5. INICIANDO CAPTURA DE CRASH DE TELAS EM TEMPO REAL..." -ForegroundColor Cyan
Write-Host "   - Pressione Ctrl+C para parar" -ForegroundColor White
Write-Host "   - Navegue pelas telas do app" -ForegroundColor White
Write-Host "   - Tente reproduzir o crash" -ForegroundColor White
Write-Host "   - Os logs aparecerão em tempo real abaixo" -ForegroundColor White
Write-Host ""

# Contadores para estatísticas
$androidRuntimeErrors = 0
$systemErrors = 0
$appErrors = 0
$fatalErrors = 0
$exceptionCount = 0
$crashCount = 0

Write-Host "CAPTURA INICIADA - Execute os passos acima!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Função para processar linha de log em tempo real
function Process-LogLine {
    param([string]$line)
    
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    
    # Verificar tipos de erro e aplicar cores
    if ($line -match "AndroidRuntime.*ERROR") {
        $script:androidRuntimeErrors++
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "System\.err.*ERROR") {
        $script:systemErrors++
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "$PackageName.*ERROR") {
        $script:appErrors++
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "FATAL|Fatal") {
        $script:fatalErrors++
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "Exception|exception") {
        $script:exceptionCount++
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "CRASH|Crash|crash") {
        $script:crashCount++
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "WARN|Warning|warning") {
        Write-Host $line -ForegroundColor DarkYellow
    }
    elseif ($line -match "INFO|Info|info") {
        Write-Host $line -ForegroundColor Cyan
    }
    else {
        Write-Host $line -ForegroundColor White
    }
}

# Capturar logs em tempo real
Write-Host "LOGS EM TEMPO REAL:" -ForegroundColor Green
Write-Host ""

try {
    # Iniciar processo de logcat em background
    $logcatProcess = Start-Process -FilePath $ADB -ArgumentList "logcat", "-v", "time", "-s", "AndroidRuntime:E", "System.err:E", "$PackageName:E", "AndroidRuntime:W", "System.err:W", "$PackageName:W", "*:S" -PassThru -NoNewWindow -RedirectStandardOutput -RedirectStandardError
    
    # Aguardar pelo tempo especificado
    $elapsed = 0
    while ($elapsed -lt $TimeoutSeconds -and !$logcatProcess.HasExited) {
        Start-Sleep -Seconds 1
        $elapsed++
        $remaining = $TimeoutSeconds - $elapsed
        
        # Mostrar progresso
        Write-Progress -Activity "Capturando logs de crash em tempo real" -Status "Aguardando operações..." -PercentComplete (($elapsed / $TimeoutSeconds) * 100) -CurrentOperation "Tempo restante: $remaining segundos"
        
        # Ler output do processo se disponível
        if ($logcatProcess.StandardOutput.Peek() -gt 0) {
            $line = $logcatProcess.StandardOutput.ReadLine()
            if ($line) {
                Process-LogLine $line
            }
        }
    }
    
    # Finalizar processo
    if (!$logcatProcess.HasExited) {
        $logcatProcess.Kill()
    }
    
} catch {
    Write-Host "Captura interrompida" -ForegroundColor Yellow
} finally {
    Write-Progress -Activity "Capturando logs de crash em tempo real" -Completed
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ESTATISTICAS DE CRASH:" -ForegroundColor White
Write-Host "   - Erros AndroidRuntime: $androidRuntimeErrors" -ForegroundColor $(if ($androidRuntimeErrors -gt 0) { "Red" } else { "Green" })
Write-Host "   - Erros System.err: $systemErrors" -ForegroundColor $(if ($systemErrors -gt 0) { "Red" } else { "Green" })
Write-Host "   - Erros do App: $appErrors" -ForegroundColor $(if ($appErrors -gt 0) { "Red" } else { "Green" })
Write-Host "   - Erros Fatais: $fatalErrors" -ForegroundColor $(if ($fatalErrors -gt 0) { "Red" } else { "Green" })
Write-Host "   - Exceções: $exceptionCount" -ForegroundColor $(if ($exceptionCount -gt 0) { "Red" } else { "Green" })
Write-Host "   - Crashes: $crashCount" -ForegroundColor $(if ($crashCount -gt 0) { "Red" } else { "Green" })

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
Write-Host "   1. Se encontrou erros acima, analise o stack trace" -ForegroundColor White
Write-Host "   2. Procure por 'AndroidRuntime' para ver erros do sistema" -ForegroundColor White
Write-Host "   3. Procure por 'System.err' para ver erros gerais" -ForegroundColor White
Write-Host "   4. Procure por '$PackageName' para ver erros específicos do app" -ForegroundColor White
Write-Host "   5. Procure por 'Exception' para ver exceções" -ForegroundColor White
Write-Host "   6. Procure por 'FATAL' para ver erros fatais" -ForegroundColor White

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE CRASH DE TELAS CONCLUIDA!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
