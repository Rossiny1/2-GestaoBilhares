# Script para testar build e capturar erros
Write-Host "Testando build..." -ForegroundColor Yellow

# Executar build e capturar output
$output = ./gradlew assembleDebug --no-daemon 2>&1 | Out-String

# Verificar se build passou
if ($LASTEXITCODE -eq 0) {
    Write-Host "BUILD PASSOU!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "BUILD FALHOU!" -ForegroundColor Red
    Write-Host ""
    Write-Host "=== ERROS ENCONTRADOS ===" -ForegroundColor Red
    $output | Select-String -Pattern "(error|ERROR|FAILED)" | Select-Object -First 30
    exit 1
}

