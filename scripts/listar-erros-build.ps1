Write-Host "Executando build e capturando erros..." -ForegroundColor Cyan
Write-Host "Isso pode demorar alguns minutos..." -ForegroundColor Yellow

$output = .\gradlew assembleDebug --continue 2>&1

$erros = $output | Select-String "error:" | Select-Object -First 100

Write-Host "`n========================================" -ForegroundColor Red
Write-Host "ERROS ENCONTRADOS: $($erros.Count)" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red

$erros | ForEach-Object {
    Write-Host $_ -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Salvando em arquivo: erros-build.txt" -ForegroundColor Cyan
$erros | Out-File -FilePath "erros-build.txt" -Encoding UTF8

Write-Host "`nConcluido! Verifique erros-build.txt" -ForegroundColor Green

