# SCRIPT DE BUILD SIMPLES - GestaoBilhares
# Build sem otimizacoes complexas para evitar erros

Write-Host "INICIANDO BUILD SIMPLES..." -ForegroundColor Green

# Parar daemons
Write-Host "Parando daemons..." -ForegroundColor Yellow
./gradlew --stop

# Limpar caches basicos
Write-Host "Limpando caches..." -ForegroundColor Yellow
./gradlew clean

# Build simples
Write-Host "Executando build..." -ForegroundColor Green
$startTime = Get-Date

try {
    ./gradlew assembleDebug
    $endTime = Get-Date
    $duration = $endTime - $startTime
    
    Write-Host "BUILD CONCLUIDO!" -ForegroundColor Green
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
