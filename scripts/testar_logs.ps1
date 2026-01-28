# Test script para analisar logs
Write-Host "Testando script..." -ForegroundColor Green

# Verificar se arquivo existe
if (Test-Path "logs_app_real.txt") {
    Write-Host "Arquivo de logs encontrado" -ForegroundColor Green
    $logs = Get-Content "logs_app_real.txt"
    Write-Host "Total de linhas: $($logs.Count)" -ForegroundColor Blue
    
    # Procurar erros
    $errors = $logs | Select-String "PERMISSION_DENIED"
    Write-Host "Erros encontrados: $($errors.Count)" -ForegroundColor Red
    
    if ($errors.Count -gt 0) {
        $errors | Out-File "erros.txt"
        Write-Host "Erros salvos em erros.txt" -ForegroundColor Green
    }
} else {
    Write-Host "Arquivo logs_app_real.txt nao encontrado" -ForegroundColor Red
}
