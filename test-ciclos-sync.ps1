# Script para testar sincronização de ciclos
# Captura logs específicos de ciclos

Write-Host "=== TESTE DE SINCRONIZACAO DE CICLOS ===" -ForegroundColor Green

# Tentar encontrar ADB automaticamente
$adbPaths = @(
    "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "C:\Android\Sdk\platform-tools\adb.exe",
    "C:\Program Files\Android\Android Studio\sdk\platform-tools\adb.exe"
)

$adb = $null
foreach ($path in $adbPaths) {
    if (Test-Path $path) {
        $adb = $path
        Write-Host "ADB encontrado: $adb" -ForegroundColor Green
        break
    }
}

if (-not $adb) {
    Write-Host "ERRO: ADB nao encontrado. Instale o Android SDK." -ForegroundColor Red
    exit 1
}

Write-Host "`n=== CAPTURANDO LOGS DE CICLOS ===" -ForegroundColor Yellow
Write-Host "Filtros aplicados:" -ForegroundColor Cyan
Write-Host "- CicloAcerto" -ForegroundColor Cyan
Write-Host "- CICLO" -ForegroundColor Cyan
Write-Host "- ciclo" -ForegroundColor Cyan
Write-Host "- SyncManagerV2" -ForegroundColor Cyan

Write-Host "`nPressione Ctrl+C para parar a captura..." -ForegroundColor Yellow

# Capturar logs com filtros específicos para ciclos
& $adb logcat -c
& $adb logcat | Select-String -Pattern "CicloAcerto|CICLO|ciclo|SyncManagerV2" | Tee-Object -FilePath "logcat-ciclos-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
