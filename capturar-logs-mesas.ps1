# Script para capturar logs de sincronização de mesas
# Versão simplificada e funcional

Write-Host "🔍 CAPTURANDO LOGS DE SINCRONIZAÇÃO DE MESAS" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

# Configurações
$PackageName = "com.example.gestaobilhares"
$OutputFile = "logcat-mesas-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
$Duration = 60  # 60 segundos

Write-Host "📱 Verificando dispositivo..." -ForegroundColor Yellow

# Verificar ADB
$adbPath = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    $adbPath = "adb"
}

try {
    $devices = & $adbPath devices 2>$null
    if ($devices -match "device$") {
        Write-Host "✅ Dispositivo conectado" -ForegroundColor Green
    } else {
        Write-Host "❌ Nenhum dispositivo encontrado" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Erro ao verificar dispositivo: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "📋 Iniciando captura de logs..." -ForegroundColor Yellow
Write-Host "   Arquivo: $OutputFile" -ForegroundColor Cyan
Write-Host "   Duração: $Duration segundos" -ForegroundColor Cyan
Write-Host "   Filtros: SyncManagerV2, Mesa, Firestore" -ForegroundColor Cyan

Write-Host ""
Write-Host "🚀 EXECUTE AS SEGUINTES AÇÕES NO APP:" -ForegroundColor Magenta
Write-Host "   1. Crie uma mesa" -ForegroundColor White
Write-Host "   2. Faça um acerto" -ForegroundColor White
Write-Host "   3. Clique em SINCRONIZAR" -ForegroundColor White
Write-Host ""

# Capturar logs com filtros específicos
try {
    Write-Host "⏱️ Capturando logs por $Duration segundos..." -ForegroundColor Yellow
    
    $logcatProcess = Start-Process -FilePath $adbPath -ArgumentList "logcat", "-s", "SyncManagerV2:V", "Mesa:V", "Firestore:V", "FirebaseFirestore:V" -RedirectStandardOutput $OutputFile -PassThru -NoNewWindow
    
    # Aguardar o tempo especificado
    Start-Sleep -Seconds $Duration
    
    # Parar o processo
    Stop-Process -Id $logcatProcess.Id -Force -ErrorAction SilentlyContinue
    
    Write-Host "✅ Captura concluída!" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Erro durante captura: $($_.Exception.Message)" -ForegroundColor Red
}

# Verificar se arquivo foi criado
if (Test-Path $OutputFile) {
    $fileSize = (Get-Item $OutputFile).Length
    Write-Host "📄 Arquivo criado: $OutputFile ($fileSize bytes)" -ForegroundColor Green
    
    # Mostrar últimas linhas
    Write-Host ""
    Write-Host "📋 ÚLTIMAS LINHAS DO LOG:" -ForegroundColor Yellow
    Write-Host "=========================" -ForegroundColor Yellow
    
    try {
        $lastLines = Get-Content $OutputFile -Tail 20
        $lastLines | ForEach-Object { Write-Host $_ -ForegroundColor White }
    } catch {
        Write-Host "❌ Erro ao ler arquivo: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "🔍 ANÁLISE RÁPIDA:" -ForegroundColor Yellow
    Write-Host "=================" -ForegroundColor Yellow
    
    # Contar ocorrências importantes
    $syncCount = (Select-String -Path $OutputFile -Pattern "SyncManagerV2" -AllMatches).Matches.Count
    $mesaCount = (Select-String -Path $OutputFile -Pattern "Mesa" -AllMatches).Matches.Count
    $firestoreCount = (Select-String -Path $OutputFile -Pattern "Firestore" -AllMatches).Matches.Count
    $errorCount = (Select-String -Path $OutputFile -Pattern "ERROR" -AllMatches).Matches.Count
    
    Write-Host "   SyncManagerV2: $syncCount ocorrências" -ForegroundColor Cyan
    Write-Host "   Mesa: $mesaCount ocorrências" -ForegroundColor Cyan
    Write-Host "   Firestore: $firestoreCount ocorrências" -ForegroundColor Cyan
    Write-Host "   Erros: $errorCount ocorrências" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
    
} else {
    Write-Host "❌ Arquivo de log não foi criado" -ForegroundColor Red
}

Write-Host ""
Write-Host "✅ Script concluído!" -ForegroundColor Green
Write-Host "📁 Arquivo salvo em: $OutputFile" -ForegroundColor Cyan
