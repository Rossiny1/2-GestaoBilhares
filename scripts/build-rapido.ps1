# 🚀 SCRIPT DE BUILD RÁPIDO - Otimizado para Windows
# Foca apenas em gerar o APK, desabilitando tasks desnecessárias

$ErrorActionPreference = "Continue"

Write-Host "🚀 BUILD RÁPIDO - GestaoBilhares" -ForegroundColor Cyan
Write-Host ""

# Verificar se gradlew existe
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "❌ gradlew.bat não encontrado!" -ForegroundColor Red
    exit 1
}

# Medir tempo
$startTime = Get-Date

# Build otimizado - apenas assembleDebug, sem testes, sem lint, sem check
Write-Host "🔨 Executando build otimizado..." -ForegroundColor Yellow
Write-Host "   • Desabilitando testes" -ForegroundColor Gray
Write-Host "   • Desabilitando lint" -ForegroundColor Gray
Write-Host "   • Desabilitando verificações" -ForegroundColor Gray
Write-Host ""

.\gradlew.bat assembleDebug `
    --no-daemon `
    --parallel `
    --build-cache `
    --warning-mode none `
    -x test `
    -x lint `
    -x check `
    -x testDebugUnitTest `
    -x testReleaseUnitTest `
    -x lintDebug `
    -x lintRelease `
    -x checkDebug `
    -x checkRelease `
    -x jacocoTestReport `
    -x testCoverage

$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host ""
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ BUILD CONCLUÍDO COM SUCESSO!" -ForegroundColor Green
    Write-Host "⏱️  Tempo: $($duration.TotalMinutes.ToString('F1')) minutos ($($duration.TotalSeconds.ToString('F0')) segundos)" -ForegroundColor Cyan
    Write-Host ""
    
    # Verificar APK
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
        Write-Host "📦 APK gerado: $apkPath ($apkSize MB)" -ForegroundColor Green
    } else {
        Write-Host "⚠️  APK não encontrado em: $apkPath" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ BUILD FALHOU!" -ForegroundColor Red
    Write-Host "⏱️  Tempo: $($duration.TotalMinutes.ToString('F1')) minutos" -ForegroundColor Yellow
    exit 1
}
