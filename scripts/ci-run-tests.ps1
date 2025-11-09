# FASE 12.12: Script para executar testes localmente (simula CI/CD)
# Uso: .\scripts\ci-run-tests.ps1

Write-Host "EXECUTANDO PIPELINE DE TESTES LOCAL" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan

# 1. Testes Unitarios
Write-Host ""
Write-Host "1. Executando Testes Unitarios..." -ForegroundColor Yellow
.\gradlew test --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Testes unitarios falharam!" -ForegroundColor Red
    exit 1
}
Write-Host "SUCCESS: Testes unitarios passaram!" -ForegroundColor Green

# 2. Analise de Codigo (Lint)
Write-Host ""
Write-Host "2. Executando Analise de Codigo (Lint)..." -ForegroundColor Yellow
.\gradlew lint --no-daemon
if ($LASTEXITCODE -eq 0) {
    Write-Host "SUCCESS: Analise de codigo concluida!" -ForegroundColor Green
} else {
    Write-Host "AVISO: Avisos de lint encontrados (nao critico)" -ForegroundColor Yellow
}

# 3. Build APK Debug
Write-Host ""
Write-Host "3. Construindo APK Debug..." -ForegroundColor Yellow
.\gradlew assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Build do APK Debug falhou!" -ForegroundColor Red
    exit 1
}
Write-Host "SUCCESS: APK Debug construido com sucesso!" -ForegroundColor Green

# Mostrar tamanho do APK
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length / 1MB
    Write-Host "Tamanho do APK: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "SUCCESS: PIPELINE LOCAL CONCLUIDA COM SUCESSO!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan
