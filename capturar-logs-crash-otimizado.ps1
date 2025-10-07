# ========================================
# SCRIPT DE CAPTURA DE LOGS DE CRASH - OTIMIZADO
# Baseado na documentação oficial Android Developer
# ========================================

param(
    [string]$PackageName = "com.example.gestaobilhares",
    [int]$TimeoutSeconds = 30,
    [string]$OutputFile = "logcat-crash-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE LOGS DE CRASH - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se ADB está disponível
Write-Host "1. Verificando ADB..." -ForegroundColor Yellow
try {
    $adbVersion = & adb version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "ADB encontrado: $($adbVersion[0])" -ForegroundColor Green
    } else {
        Write-Host "ADB nao encontrado. Verifique se esta no PATH." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Erro ao verificar ADB: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Verificar dispositivos conectados
Write-Host "2. Verificando dispositivos..." -ForegroundColor Yellow
$devices = & adb devices 2>$null
$deviceCount = ($devices | Where-Object { $_ -match "device$" }).Count

if ($deviceCount -eq 0) {
    Write-Host "Nenhum dispositivo conectado. Conecte um dispositivo e habilite USB Debugging." -ForegroundColor Red
    exit 1
} elseif ($deviceCount -gt 1) {
    Write-Host "Multiplos dispositivos conectados. Usando o primeiro." -ForegroundColor Yellow
}

Write-Host "$deviceCount dispositivo(s) conectado(s)" -ForegroundColor Green

# Limpar logs anteriores
Write-Host "3. Limpando logs anteriores..." -ForegroundColor Yellow
& adb logcat -c 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Logs anteriores limpos" -ForegroundColor Green
} else {
    Write-Host "⚠️ Não foi possível limpar logs anteriores" -ForegroundColor Yellow
}

# Verificar se o app está instalado
Write-Host "4. Verificando instalação do app..." -ForegroundColor Yellow
$installedApps = & adb shell pm list packages $PackageName 2>$null
if ($installedApps -match $PackageName) {
    Write-Host "✅ App $PackageName encontrado" -ForegroundColor Green
} else {
    Write-Host "❌ App $PackageName não encontrado. Instale o APK primeiro." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "5. INICIANDO CAPTURA DE LOGS..." -ForegroundColor Cyan
Write-Host "   - Pressione Ctrl+C para parar a captura" -ForegroundColor White
Write-Host "   - Abra o app e reproduza o crash" -ForegroundColor White
Write-Host "   - Aguarde $TimeoutSeconds segundos ou pare manualmente" -ForegroundColor White
Write-Host ""

# Iniciar captura de logs com filtros otimizados
$logcatCommand = "adb logcat -v time -s System.err:V AndroidRuntime:E $PackageName:V *:S"
Write-Host "Comando: $logcatCommand" -ForegroundColor Gray

# Capturar logs em background
$job = Start-Job -ScriptBlock {
    param($cmd, $output)
    & cmd /c "$cmd > `"$output`" 2>&1"
} -ArgumentList $logcatCommand, $OutputFile

Write-Host "📱 CAPTURA INICIADA - Abra o app agora!" -ForegroundColor Green
Write-Host ""

# Aguardar timeout ou interrupção manual
try {
    $elapsed = 0
    while ($elapsed -lt $TimeoutSeconds) {
        Start-Sleep -Seconds 1
        $elapsed++
        $remaining = $TimeoutSeconds - $elapsed
        Write-Progress -Activity "Capturando logs de crash" -Status "Aguardando crash..." -PercentComplete (($elapsed / $TimeoutSeconds) * 100) -CurrentOperation "Tempo restante: $remaining segundos"
    }
} catch {
    Write-Host "Captura interrompida pelo usuário" -ForegroundColor Yellow
} finally {
    Write-Progress -Activity "Capturando logs de crash" -Completed
}

# Parar captura
Write-Host ""
Write-Host "6. Finalizando captura..." -ForegroundColor Yellow
Stop-Job $job -ErrorAction SilentlyContinue
Remove-Job $job -ErrorAction SilentlyContinue

# Parar logcat
& adb logcat -d > $null 2>&1

# Verificar se arquivo foi criado
if (Test-Path $OutputFile) {
    $fileSize = (Get-Item $OutputFile).Length
    Write-Host "✅ Logs capturados: $OutputFile ($fileSize bytes)" -ForegroundColor Green
} else {
    Write-Host "❌ Nenhum log foi capturado" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "7. ANALISANDO LOGS..." -ForegroundColor Cyan

# Análise básica dos logs
$logContent = Get-Content $OutputFile -Raw
$errorCount = ($logContent | Select-String -Pattern "FATAL|ERROR|Exception|Crash" -AllMatches).Matches.Count
$packageErrors = ($logContent | Select-String -Pattern $PackageName -AllMatches).Matches.Count

Write-Host "📊 ESTATÍSTICAS:" -ForegroundColor White
Write-Host "   - Erros encontrados: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
Write-Host "   - Logs do app: $packageErrors" -ForegroundColor $(if ($packageErrors -gt 0) { "Yellow" } else { "Gray" })
Write-Host "   - Tamanho do arquivo: $fileSize bytes" -ForegroundColor White

# Procurar por erros específicos
Write-Host ""
Write-Host "8. ERROS ESPECÍFICOS ENCONTRADOS:" -ForegroundColor Cyan

# Firebase/Google Services errors
$firebaseErrors = $logContent | Select-String -Pattern "Firebase|Google.*Services|GmsClient|DeadObjectException" -AllMatches
if ($firebaseErrors) {
    Write-Host "🔥 FIREBASE/GOOGLE SERVICES ERRORS:" -ForegroundColor Red
    $firebaseErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

# Database errors
$dbErrors = $logContent | Select-String -Pattern "SQLite|Room|Database|AppDatabase" -AllMatches
if ($dbErrors) {
    Write-Host "🗄️ DATABASE ERRORS:" -ForegroundColor Red
    $dbErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

# Authentication errors
$authErrors = $logContent | Select-String -Pattern "AuthViewModel|LoginFragment|Authentication" -AllMatches
if ($authErrors) {
    Write-Host "🔐 AUTHENTICATION ERRORS:" -ForegroundColor Red
    $authErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

# Memory/Performance errors
$memoryErrors = $logContent | Select-String -Pattern "OutOfMemory|ANR|NotResponding|GC" -AllMatches
if ($memoryErrors) {
    Write-Host "💾 MEMORY/PERFORMANCE ERRORS:" -ForegroundColor Red
    $memoryErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

# Network errors
$networkErrors = $logContent | Select-String -Pattern "Network|Connection|Timeout|Socket" -AllMatches
if ($networkErrors) {
    Write-Host "🌐 NETWORK ERRORS:" -ForegroundColor Red
    $networkErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

Write-Host ""
Write-Host "9. PRÓXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "   1. Abra o arquivo: $OutputFile" -ForegroundColor White
Write-Host "   2. Procure por 'FATAL EXCEPTION' ou 'AndroidRuntime'" -ForegroundColor White
Write-Host "   3. Identifique a linha exata do crash" -ForegroundColor White
Write-Host "   4. Verifique o stack trace completo" -ForegroundColor White
Write-Host "   5. Corrija o código baseado no erro encontrado" -ForegroundColor White

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA CONCLUÍDA COM SUCESSO!" -ForegroundColor Green
Write-Host "Arquivo: $OutputFile" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
