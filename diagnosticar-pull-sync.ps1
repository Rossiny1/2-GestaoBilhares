# Script para diagnosticar PULL SYNC
# Captura logs específicos da sincronização bidirecional

Write-Host "🔍 DIAGNÓSTICO PULL SYNC - Sincronização Bidirecional" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

# Verificar se ADB está disponível
$adbPath = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    $adbPath = "adb"
}

Write-Host "📱 Verificando dispositivo Android..." -ForegroundColor Yellow
$deviceCheck = & $adbPath devices 2>&1
if ($deviceCheck -match "device$") {
    Write-Host "✅ Dispositivo conectado" -ForegroundColor Green
} else {
    Write-Host "❌ Nenhum dispositivo encontrado" -ForegroundColor Red
    Write-Host "   Conecte um dispositivo Android e habilite USB Debugging" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n🔄 Iniciando captura de logs PULL SYNC..." -ForegroundColor Yellow
Write-Host "   Filtros: SyncManagerV2, Firebase, PULL SYNC" -ForegroundColor Gray
Write-Host "   Pressione Ctrl+C para parar" -ForegroundColor Gray

# Capturar logs com filtros específicos para PULL SYNC
& $adbPath logcat -c
& $adbPath logcat -s SyncManagerV2:V RoutesFragment:V FirebaseAuth:V FirebaseFirestore:V com.example.gestaobilhares:V | ForEach-Object {
    $line = $_
    
    # Destacar logs importantes
    if ($line -match "PULL SYNC|pullFromFirestore|Baixando clientes|Cliente sincronizado|Empresa ID") {
        Write-Host $line -ForegroundColor Green
    } elseif ($line -match "ERROR|Erro|Falha") {
        Write-Host $line -ForegroundColor Red
    } elseif ($line -match "WARN|Warning") {
        Write-Host $line -ForegroundColor Yellow
    } else {
        Write-Host $line -ForegroundColor White
    }
}
