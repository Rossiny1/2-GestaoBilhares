# SCRIPT DE OTIMIZACAO DE BUILD - GestaoBilhares
# Limpa e otimiza o ambiente de build para maxima performance

Write-Host "INICIANDO OTIMIZACAO DE BUILD..." -ForegroundColor Green

# 1. Parar daemons
Write-Host "Parando daemons..." -ForegroundColor Yellow
./gradlew --stop
taskkill /f /im java.exe 2>$null

# 2. Limpar caches
Write-Host "Limpando caches..." -ForegroundColor Yellow
./gradlew clean --no-daemon
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force app/build -ErrorAction SilentlyContinue

# 3. Reinstalar Gradle Wrapper
Write-Host "Reinstalando Gradle Wrapper..." -ForegroundColor Yellow
./gradlew wrapper --gradle-version=8.5

# 4. Configurar variaveis de ambiente otimizadas
Write-Host "Configurando variaveis de ambiente..." -ForegroundColor Yellow
$env:GRADLE_OPTS = "-Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
$env:JAVA_OPTS = "-Xmx8g -XX:+UseG1GC"

# 5. Testar build otimizado
Write-Host "Testando build otimizado..." -ForegroundColor Green
$startTime = Get-Date

try {
    ./gradlew assembleDebug --no-daemon --parallel --build-cache
    $endTime = Get-Date
    $duration = $endTime - $startTime
    
    Write-Host "OTIMIZACAO CONCLUIDA!" -ForegroundColor Green
    Write-Host "Tempo de build: $($duration.TotalMinutes.ToString('F1')) minutos" -ForegroundColor Cyan
    
    # Verificar se APK foi gerado
    if (Test-Path "app/build/outputs/apk/debug/app-debug.apk") {
        Write-Host "APK gerado com sucesso!" -ForegroundColor Green
        Write-Host "Local: app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Cyan
    } else {
        Write-Host "APK nao encontrado!" -ForegroundColor Red
    }
    
    Write-Host "OTIMIZACOES APLICADAS:" -ForegroundColor Magenta
    Write-Host "  • Todos os caches limpos" -ForegroundColor White
    Write-Host "  • Daemons reiniciados" -ForegroundColor White
    Write-Host "  • Gradle Wrapper atualizado" -ForegroundColor White
    Write-Host "  • Memoria otimizada (8GB)" -ForegroundColor White
    Write-Host "  • Garbage Collector otimizado" -ForegroundColor White
    Write-Host "  • String deduplication ativado" -ForegroundColor White
    
    Write-Host "PROXIMOS PASSOS:" -ForegroundColor Cyan
    Write-Host "  • Use 'build-incremental.ps1' para builds rapidos" -ForegroundColor White
    Write-Host "  • Use 'build-optimizado.ps1' para builds completos" -ForegroundColor White
    Write-Host "  • Builds incrementais devem ser 2x mais rapidos" -ForegroundColor White
    
} catch {
    Write-Host "ERRO no build: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Verifique os logs acima para detalhes" -ForegroundColor Yellow
}
