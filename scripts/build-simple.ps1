# SCRIPT DE BUILD SIMPLES - GestaoBilhares
# Testa build apos correcoes de dependencias

Write-Host "INICIANDO BUILD SIMPLES..." -ForegroundColor Green

# Limpar caches
Write-Host "Limpando caches..." -ForegroundColor Yellow
./gradlew clean --no-daemon

# Build de teste
Write-Host "Executando build..." -ForegroundColor Green
$startTime = Get-Date

try {
    ./gradlew assembleDebug --no-daemon --no-configuration-cache
    $endTime = Get-Date
    $duration = $endTime - $startTime
    
    Write-Host "BUILD CONCLUIDO COM SUCESSO!" -ForegroundColor Green
    Write-Host "Tempo de build: $($duration.TotalMinutes.ToString('F1')) minutos" -ForegroundColor Cyan
    
    # Verificar se APK foi gerado
    if (Test-Path "app/build/outputs/apk/debug/app-debug.apk") {
        Write-Host "APK gerado com sucesso!" -ForegroundColor Green
        Write-Host "Local: app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Cyan
    } else {
        Write-Host "APK nao encontrado!" -ForegroundColor Red
    }
    
} catch {
    Write-Host "ERRO no build: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Verifique os logs acima para detalhes" -ForegroundColor Yellow
}
