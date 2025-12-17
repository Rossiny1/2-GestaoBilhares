# SCRIPT DE BUILD OTIMIZADO - GestaoBilhares
# Otimizacoes para reduzir tempo de build de 4m para ~2m

Write-Host "INICIANDO BUILD OTIMIZADO..." -ForegroundColor Green

# 1. Limpar cache e daemons
Write-Host "Limpando cache e daemons..." -ForegroundColor Yellow
./gradlew --stop
taskkill /f /im java.exe 2>$null
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "app/build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue

# 2. Configurar variaveis de ambiente para performance
Write-Host "Configurando variaveis de ambiente..." -ForegroundColor Yellow
$env:GRADLE_OPTS = "-Xmx8192m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
$env:KOTLIN_DAEMON_OPTS = "-Xmx6144m -XX:+UseG1GC"

# 3. Build com otimizacoes
Write-Host "Executando build otimizado..." -ForegroundColor Green
$startTime = Get-Date

./gradlew assembleDebug --no-daemon --parallel --build-cache --configuration-cache --warning-mode none

$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host "BUILD CONCLUIDO!" -ForegroundColor Green
Write-Host "Tempo total: $($duration.TotalMinutes.ToString('F1')) minutos" -ForegroundColor Cyan

if ($LASTEXITCODE -eq 0) {
    Write-Host "APK gerado com sucesso!" -ForegroundColor Green
    Write-Host "Local: app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Gray
} else {
    Write-Host "Build falhou!" -ForegroundColor Red
    exit 1
}
