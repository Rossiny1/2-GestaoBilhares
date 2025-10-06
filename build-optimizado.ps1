# 🚀 SCRIPT DE BUILD OTIMIZADO - GestaoBilhares
# Otimizações para reduzir tempo de build de 4m para ~2m

Write-Host "🚀 INICIANDO BUILD OTIMIZADO..." -ForegroundColor Green

# 1. Limpar cache e daemons
Write-Host "🧹 Limpando cache e daemons..." -ForegroundColor Yellow
./gradlew --stop
taskkill /f /im java.exe 2>$null
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "app/build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue

# 2. Configurar variáveis de ambiente para performance
Write-Host "⚡ Configurando variáveis de ambiente..." -ForegroundColor Yellow
$env:GRADLE_OPTS = "-Xmx8192m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
$env:KOTLIN_DAEMON_OPTS = "-Xmx6144m -XX:+UseG1GC"

# 3. Build com otimizações
Write-Host "🔨 Executando build otimizado..." -ForegroundColor Green
$startTime = Get-Date

./gradlew assembleDebug --no-daemon --parallel --build-cache --configuration-cache --warning-mode none

$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host "✅ BUILD CONCLUÍDO!" -ForegroundColor Green
Write-Host "⏱️  Tempo total: $($duration.TotalMinutes.ToString('F1')) minutos" -ForegroundColor Cyan

# 4. Verificar se APK foi gerado
if (Test-Path "app/build/outputs/apk/debug/app-debug.apk") {
    Write-Host "📱 APK gerado com sucesso!" -ForegroundColor Green
    Write-Host "📍 Local: app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Cyan
} else {
    Write-Host "❌ APK não encontrado!" -ForegroundColor Red
}

Write-Host "🎯 OTIMIZAÇÕES APLICADAS:" -ForegroundColor Magenta
Write-Host "  • Cache limpo e daemons reiniciados" -ForegroundColor White
Write-Host "  • Memória aumentada para 8GB" -ForegroundColor White
Write-Host "  • Build paralelo ativado" -ForegroundColor White
Write-Host "  • Configuration cache ativado" -ForegroundColor White
Write-Host "  • Warnings desabilitados para velocidade" -ForegroundColor White
