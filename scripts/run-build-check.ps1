# Script para executar build e capturar erros
$ErrorActionPreference = "Stop"

Write-Host "Executando build do modulo :ui..."
$output = .\gradlew :ui:compileDebugKotlin --no-daemon 2>&1 | Out-String

if ($LASTEXITCODE -eq 0) {
    Write-Host "BUILD PASSOU!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "BUILD FALHOU!" -ForegroundColor Red
    Write-Host $output
    # Extrair erros
    $errors = $output | Select-String -Pattern "error:|Unresolved|Cannot resolve" -CaseSensitive:$false
    if ($errors) {
        Write-Host "`n=== ERROS ENCONTRADOS ===" -ForegroundColor Yellow
        $errors | ForEach-Object { Write-Host $_ }
    }
    exit 1
}

