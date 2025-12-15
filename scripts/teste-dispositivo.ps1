# Script de teste para verificar dispositivo
Write-Host "=== TESTE DE DISPOSITIVO ===" -ForegroundColor Yellow

# Caminho do ADB
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "Caminho do ADB: $ADB" -ForegroundColor Gray
Write-Host "ADB existe: $(Test-Path $ADB)" -ForegroundColor $(if (Test-Path $ADB) { "Green" } else { "Red" })

Write-Host ""
Write-Host "Executando 'adb devices'..." -ForegroundColor Yellow
try {
    $devices = & $ADB devices 2>&1
    Write-Host "Saida completa:" -ForegroundColor Cyan
    Write-Host $devices -ForegroundColor White

    Write-Host ""
    Write-Host "Teste de regex 'device$':" -ForegroundColor Yellow
    $matchResult = $devices -match "device$"
    Write-Host "Resultado -match 'device$': $matchResult" -ForegroundColor $(if ($matchResult) { "Green" } else { "Red" })

    $notMatchResult = $devices -notmatch "device$"
    Write-Host "Resultado -notmatch 'device$': $notMatchResult" -ForegroundColor $(if (!$notMatchResult) { "Green" } else { "Red" })

} catch {
    Write-Host "ERRO ao executar ADB: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== FIM DO TESTE ===" -ForegroundColor Yellow
