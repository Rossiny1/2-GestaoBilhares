# ========================================
# CAPTURA DE LOGS DE CRASH - SIMPLES
# ========================================

param(
    [string]$PackageName = "com.example.gestaobilhares",
    [int]$TimeoutSeconds = 30
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE LOGS DE CRASH" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar ADB
Write-Host "1. Verificando ADB..." -ForegroundColor Yellow
try {
    $adbVersion = & adb version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "ADB encontrado" -ForegroundColor Green
    } else {
        Write-Host "ADB nao encontrado" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Erro ao verificar ADB" -ForegroundColor Red
    exit 1
}

# Verificar dispositivos
Write-Host "2. Verificando dispositivos..." -ForegroundColor Yellow
$devices = & adb devices 2>$null
$deviceCount = ($devices | Where-Object { $_ -match "device$" }).Count

if ($deviceCount -eq 0) {
    Write-Host "Nenhum dispositivo conectado" -ForegroundColor Red
    exit 1
}

Write-Host "$deviceCount dispositivo(s) conectado(s)" -ForegroundColor Green

# Limpar logs
Write-Host "3. Limpando logs anteriores..." -ForegroundColor Yellow
& adb logcat -c 2>$null
Write-Host "Logs anteriores limpos" -ForegroundColor Green

# Verificar app
Write-Host "4. Verificando app..." -ForegroundColor Yellow
$installedApps = & adb shell pm list packages $PackageName 2>$null
if ($installedApps -match $PackageName) {
    Write-Host "App $PackageName encontrado" -ForegroundColor Green
} else {
    Write-Host "App $PackageName nao encontrado" -ForegroundColor Red
    exit 1
}

# Gerar nome do arquivo
$OutputFile = "logcat-crash-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

Write-Host ""
Write-Host "5. INICIANDO CAPTURA DE LOGS..." -ForegroundColor Cyan
Write-Host "   - Pressione Ctrl+C para parar" -ForegroundColor White
Write-Host "   - Abra o app e reproduza o crash" -ForegroundColor White
Write-Host "   - Aguarde $TimeoutSeconds segundos" -ForegroundColor White
Write-Host ""

# Capturar logs
Write-Host "CAPTURA INICIADA - Abra o app agora!" -ForegroundColor Green
Write-Host ""

try {
    $elapsed = 0
    while ($elapsed -lt $TimeoutSeconds) {
        Start-Sleep -Seconds 1
        $elapsed++
        $remaining = $TimeoutSeconds - $elapsed
        Write-Progress -Activity "Capturando logs de crash" -Status "Aguardando crash..." -PercentComplete (($elapsed / $TimeoutSeconds) * 100) -CurrentOperation "Tempo restante: $remaining segundos"
    }
} catch {
    Write-Host "Captura interrompida" -ForegroundColor Yellow
} finally {
    Write-Progress -Activity "Capturando logs de crash" -Completed
}

# Capturar logs com filtros
Write-Host "6. Capturando logs..." -ForegroundColor Yellow
& adb logcat -v time -s System.err:V AndroidRuntime:E $PackageName:V *:S > $OutputFile 2>&1

# Verificar arquivo
if (Test-Path $OutputFile) {
    $fileSize = (Get-Item $OutputFile).Length
    Write-Host "Logs capturados: $OutputFile ($fileSize bytes)" -ForegroundColor Green
} else {
    Write-Host "Nenhum log foi capturado" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "7. ANALISANDO LOGS..." -ForegroundColor Cyan

# Análise básica
$logContent = Get-Content $OutputFile -Raw
$errorCount = ($logContent | Select-String -Pattern "FATAL|ERROR|Exception|Crash" -AllMatches).Matches.Count
$packageErrors = ($logContent | Select-String -Pattern $PackageName -AllMatches).Matches.Count

Write-Host "ESTATISTICAS:" -ForegroundColor White
Write-Host "   - Erros encontrados: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
Write-Host "   - Logs do app: $packageErrors" -ForegroundColor $(if ($packageErrors -gt 0) { "Yellow" } else { "Gray" })
Write-Host "   - Tamanho do arquivo: $fileSize bytes" -ForegroundColor White

# Procurar erros específicos
Write-Host ""
Write-Host "ERROS ESPECIFICOS ENCONTRADOS:" -ForegroundColor Cyan

# Firebase/Google Services errors
$firebaseErrors = $logContent | Select-String -Pattern "Firebase|Google.*Services|GmsClient|DeadObjectException" -AllMatches
if ($firebaseErrors) {
    Write-Host "FIREBASE/GOOGLE SERVICES ERRORS:" -ForegroundColor Red
    $firebaseErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

# Database errors
$dbErrors = $logContent | Select-String -Pattern "SQLite|Room|Database|AppDatabase" -AllMatches
if ($dbErrors) {
    Write-Host "DATABASE ERRORS:" -ForegroundColor Red
    $dbErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

# Authentication errors
$authErrors = $logContent | Select-String -Pattern "AuthViewModel|LoginFragment|Authentication" -AllMatches
if ($authErrors) {
    Write-Host "AUTHENTICATION ERRORS:" -ForegroundColor Red
    $authErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

# Memory/Performance errors
$memoryErrors = $logContent | Select-String -Pattern "OutOfMemory|ANR|NotResponding|GC" -AllMatches
if ($memoryErrors) {
    Write-Host "MEMORY/PERFORMANCE ERRORS:" -ForegroundColor Red
    $memoryErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

# Network errors
$networkErrors = $logContent | Select-String -Pattern "Network|Connection|Timeout|Socket" -AllMatches
if ($networkErrors) {
    Write-Host "NETWORK ERRORS:" -ForegroundColor Red
    $networkErrors | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor Red }
}

Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "   1. Abra o arquivo: $OutputFile" -ForegroundColor White
Write-Host "   2. Procure por 'FATAL EXCEPTION' ou 'AndroidRuntime'" -ForegroundColor White
Write-Host "   3. Identifique a linha exata do crash" -ForegroundColor White
Write-Host "   4. Verifique o stack trace completo" -ForegroundColor White
Write-Host "   5. Corrija o codigo baseado no erro encontrado" -ForegroundColor White

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA CONCLUIDA COM SUCESSO!" -ForegroundColor Green
Write-Host "Arquivo: $OutputFile" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
