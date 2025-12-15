# 🎯 SIMPLIFICAÇÃO RADICAL DO BUILD
# Estratégia: Remover complexidade desnecessária

Write-Host "🎯 SIMPLIFICANDO BUILD RADICALMENTE..." -ForegroundColor Red

# 1. PARAR TUDO
Write-Host "🛑 Parando todos os processos..." -ForegroundColor Yellow
./gradlew --stop
taskkill /f /im java.exe 2>$null
taskkill /f /im kotlin-daemon.exe 2>$null

# 2. LIMPAR TUDO
Write-Host "🧹 Limpando completamente..." -ForegroundColor Yellow
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "app/build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue

# 3. CONFIGURAÇÃO MÍNIMA
Write-Host "⚙️ Aplicando configuração mínima..." -ForegroundColor Yellow
$env:GRADLE_OPTS = "-Xmx4g -XX:+UseG1GC"
$env:KOTLIN_DAEMON_OPTS = "-Xmx2g"

# 4. BUILD SIMPLES (SEM OTIMIZAÇÕES COMPLEXAS)
Write-Host "🔨 Build simples..." -ForegroundColor Green
$startTime = Get-Date

./gradlew assembleDebug --no-daemon --no-parallel --no-build-cache --no-configuration-cache

$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host "✅ BUILD SIMPLES CONCLUÍDO!" -ForegroundColor Green
Write-Host "⏱️ Tempo: $($duration.TotalMinutes.ToString('F1')) minutos" -ForegroundColor Cyan

if (Test-Path "app/build/outputs/apk/debug/app-debug.apk") {
    Write-Host "📱 APK gerado!" -ForegroundColor Green
} else {
    Write-Host "❌ Falhou - vamos para estratégia alternativa" -ForegroundColor Red
}
