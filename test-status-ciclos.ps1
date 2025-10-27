# Script para testar status dos ciclos após sincronização
# Captura logs específicos de status de ciclos

Write-Host "=== TESTE DE STATUS DOS CICLOS ===" -ForegroundColor Green

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

Write-Host "`n=== CAPTURANDO LOGS DE STATUS DOS CICLOS ===" -ForegroundColor Yellow
Write-Host "Filtros aplicados:" -ForegroundColor Cyan
Write-Host "- Status do ciclo" -ForegroundColor Cyan
Write-Host "- EM_ANDAMENTO" -ForegroundColor Cyan
Write-Host "- FINALIZADO" -ForegroundColor Cyan
Write-Host "- StatusCicloAcerto" -ForegroundColor Cyan
Write-Host "- SyncManagerV2" -ForegroundColor Cyan
Write-Host "- criarCiclosAutomaticamente" -ForegroundColor Cyan

Write-Host "`nPressione Ctrl+C para parar a captura..." -ForegroundColor Yellow

# Capturar logs com filtros específicos para status dos ciclos
& $adb logcat -c
& $adb logcat | Select-String -Pattern "Status do ciclo|EM_ANDAMENTO|FINALIZADO|StatusCicloAcerto|SyncManagerV2|criarCiclosAutomaticamente" | Tee-Object -FilePath "logcat-status-ciclos-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
