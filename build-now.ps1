# Script de build limpo
Write-Host "=== INICIANDO BUILD ===" -ForegroundColor Cyan
.\gradlew clean assembleDebug --no-daemon
if ($LASTEXITCODE -eq 0) {
    Write-Host "=== BUILD SUCESSO ===" -ForegroundColor Green
} else {
    Write-Host "=== BUILD FALHOU ===" -ForegroundColor Red
    exit $LASTEXITCODE
}

