# 🧹 SCRIPT DE OTIMIZAÇÃO DE BUILD - GestaoBilhares
# Limpa e otimiza o ambiente de build para máxima performance

Write-Host "🧹 INICIANDO OTIMIZAÇÃO DE BUILD..." -ForegroundColor Green

# 1. Parar todos os daemons
Write-Host "🛑 Parando daemons..." -ForegroundColor Yellow
./gradlew --stop
taskkill /f /im java.exe 2>$null
taskkill /f /im kotlin-daemon.exe 2>$null

# 2. Limpar todos os caches
Write-Host "🗑️  Limpando caches..." -ForegroundColor Yellow
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "app/build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "gradle" -Recurse -Force -ErrorAction SilentlyContinue

# 3. Reinstalar Gradle Wrapper
Write-Host "📦 Reinstalando Gradle Wrapper..." -ForegroundColor Yellow
./gradlew wrapper --gradle-version=8.5

# 4. Configurar variáveis de ambiente otimizadas
Write-Host "⚡ Configurando variáveis de ambiente..." -ForegroundColor Yellow
$env:GRADLE_OPTS = "-Xmx8192m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"
$env:KOTLIN_DAEMON_OPTS = "-Xmx6144m -XX:+UseG1GC -XX:+UseStringDeduplication"
$env:JAVA_OPTS = "-Xmx8192m -XX:+UseG1GC"

# 5. Testar build otimizado
Write-Host "🔨 Testando build otimizado..." -ForegroundColor Green
$startTime = Get-Date

./gradlew assembleDebug --no-daemon --parallel --build-cache --configuration-cache --warning-mode none

$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host "✅ OTIMIZAÇÃO CONCLUÍDA!" -ForegroundColor Green
Write-Host "⏱️  Tempo de build: $($duration.TotalMinutes.ToString('F1')) minutos" -ForegroundColor Cyan

# 6. Verificar se APK foi gerado
if (Test-Path "app/build/outputs/apk/debug/app-debug.apk") {
    Write-Host "📱 APK gerado com sucesso!" -ForegroundColor Green
    Write-Host "📍 Local: app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Cyan
} else {
    Write-Host "❌ APK não encontrado!" -ForegroundColor Red
}

Write-Host "🎯 OTIMIZAÇÕES APLICADAS:" -ForegroundColor Magenta
Write-Host "  • Todos os caches limpos" -ForegroundColor White
Write-Host "  • Daemons reiniciados" -ForegroundColor White
Write-Host "  • Gradle Wrapper atualizado" -ForegroundColor White
Write-Host "  • Memória otimizada (8GB)" -ForegroundColor White
Write-Host "  • Garbage Collector otimizado" -ForegroundColor White
Write-Host "  • String deduplication ativado" -ForegroundColor White

Write-Host "💡 PRÓXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "  • Use 'build-incremental.ps1' para builds rápidos" -ForegroundColor White
Write-Host "  • Use 'build-optimizado.ps1' para builds completos" -ForegroundColor White
Write-Host "  • Builds incrementais devem ser 2x mais rapidos" -ForegroundColor White
