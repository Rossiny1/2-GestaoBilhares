# ========================================
# MONITOR DE LOGS EM TEMPO REAL - GESTAO BILHARES
# Baseado na documentação oficial Android Developer
# ========================================

param(
    [string]$PackageName = "com.example.gestaobilhares",
    [int]$RefreshInterval = 2
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MONITOR DE LOGS EM TEMPO REAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar ADB
Write-Host "1. Verificando ADB..." -ForegroundColor Yellow
try {
    $adbVersion = & adb version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ ADB encontrado" -ForegroundColor Green
    } else {
        Write-Host "❌ ADB não encontrado" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Erro ao verificar ADB" -ForegroundColor Red
    exit 1
}

# Verificar dispositivos
$devices = & adb devices 2>$null
$deviceCount = ($devices | Where-Object { $_ -match "device$" }).Count

if ($deviceCount -eq 0) {
    Write-Host "❌ Nenhum dispositivo conectado" -ForegroundColor Red
    exit 1
}

Write-Host "✅ $deviceCount dispositivo(s) conectado(s)" -ForegroundColor Green

# Verificar app instalado
$installedApps = & adb shell pm list packages $PackageName 2>$null
if (-not ($installedApps -match $PackageName)) {
    Write-Host "❌ App $PackageName não encontrado" -ForegroundColor Red
    exit 1
}

Write-Host "✅ App $PackageName encontrado" -ForegroundColor Green

Write-Host ""
Write-Host "2. INICIANDO MONITOR EM TEMPO REAL..." -ForegroundColor Cyan
Write-Host "   - Pressione Ctrl+C para parar" -ForegroundColor White
Write-Host "   - Use o app normalmente" -ForegroundColor White
Write-Host "   - Erros serão destacados em tempo real" -ForegroundColor White
Write-Host ""

# Limpar logs
& adb logcat -c 2>$null

# Função para processar linha de log
function Process-LogLine {
    param([string]$line)
    
    if ([string]::IsNullOrEmpty($line)) { return }
    
    # Verificar se é do nosso app
    if ($line -match $PackageName) {
        # Verificar tipo de log
        if ($line -match "FATAL EXCEPTION") {
            Write-Host "🚨 CRASH: $line" -ForegroundColor Red -BackgroundColor Black
        } elseif ($line -match "AndroidRuntime.*ERROR") {
            Write-Host "❌ ERROR: $line" -ForegroundColor Red
        } elseif ($line -match "Exception") {
            Write-Host "⚠️ EXCEPTION: $line" -ForegroundColor Yellow
        } elseif ($line -match "Firebase|Google.*Services|GmsClient") {
            Write-Host "🔥 FIREBASE: $line" -ForegroundColor Magenta
        } elseif ($line -match "Database|SQLite|Room") {
            Write-Host "🗄️ DATABASE: $line" -ForegroundColor Blue
        } elseif ($line -match "AuthViewModel|LoginFragment") {
            Write-Host "🔐 AUTH: $line" -ForegroundColor Cyan
        } elseif ($line -match "OutOfMemory|ANR") {
            Write-Host "💾 MEMORY: $line" -ForegroundColor DarkRed
        } else {
            Write-Host "📱 APP: $line" -ForegroundColor Green
        }
    }
}

# Iniciar monitoramento
Write-Host "📱 MONITOR ATIVO - Use o app agora!" -ForegroundColor Green
Write-Host ""

try {
    # Comando para capturar logs em tempo real
    $logcatCommand = "adb logcat -v time -s System.err:V AndroidRuntime:E $PackageName:V *:S"
    
    # Executar logcat e processar linha por linha
    $process = Start-Process -FilePath "adb" -ArgumentList "logcat", "-v", "time", "-s", "System.err:V", "AndroidRuntime:E", "$PackageName:V", "*:S" -NoNewWindow -PassThru -RedirectStandardOutput
    
    # Ler output em tempo real
    $reader = $process.StandardOutput
    while (-not $reader.EndOfStream) {
        $line = $reader.ReadLine()
        if ($line) {
            Process-LogLine -line $line
        }
        Start-Sleep -Milliseconds 100
    }
} catch {
    Write-Host "Monitor interrompido: $($_.Exception.Message)" -ForegroundColor Yellow
} finally {
    if ($process -and -not $process.HasExited) {
        $process.Kill()
    }
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "MONITOR FINALIZADO!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
}
