# ⚡ BUILD INCREMENTAL RÁPIDO - GestaoBilhares
# Para builds após mudanças pequenas (2-3x mais rápido)

Write-Host "⚡ INICIANDO BUILD INCREMENTAL..." -ForegroundColor Green

# 1. Configurar variáveis de ambiente para performance máxima
Write-Host "🚀 Configurando performance máxima..." -ForegroundColor Yellow
$env:GRADLE_OPTS = "-Xmx8192m -XX:+UseG1GC -XX:MaxGCPauseMillis=50"
$env:KOTLIN_DAEMON_OPTS = "-Xmx6144m -XX:+UseG1GC"

# 2. Build incremental com cache
Write-Host "🔨 Executando build incremental..." -ForegroundColor Green
$startTime = Get-Date

./gradlew assembleDebug --daemon --parallel --build-cache --configuration-cache --warning-mode none --continue

$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host "✅ BUILD INCREMENTAL CONCLUÍDO!" -ForegroundColor Green
Write-Host "⏱️  Tempo total: $($duration.TotalSeconds.ToString('F1')) segundos" -ForegroundColor Cyan

# 3. Verificar se APK foi gerado
if (Test-Path "app/build/outputs/apk/debug/app-debug.apk") {
    Write-Host "📱 APK gerado com sucesso!" -ForegroundColor Green
    Write-Host "📍 Local: app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Cyan
} else {
    Write-Host "❌ APK não encontrado!" -ForegroundColor Red
}

Write-Host "🎯 BUILD INCREMENTAL OTIMIZADO:" -ForegroundColor Magenta
Write-Host "  • Usa cache existente" -ForegroundColor White
Write-Host "  • Compila apenas arquivos alterados" -ForegroundColor White
Write-Host "  • Daemon mantido ativo" -ForegroundColor White
Write-Host "  • Performance máxima (8GB RAM)" -ForegroundColor White
