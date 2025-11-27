# Script para comentar referencias a R em ReciboPrinterHelper.kt
Write-Host "=== COMENTANDO REFERENCIAS A R EM ReciboPrinterHelper.kt ===" -ForegroundColor Cyan

$file = "core\src\main\java\com\example\gestaobilhares\utils\ReciboPrinterHelper.kt"
$content = Get-Content $file -Raw -Encoding UTF8

# Comentar todas as referencias a R.id e R.drawable
$content = $content -replace '(\s+)(val\s+\w+\s*=\s*reciboView\.findViewById[^)]+\(R\.)', '$1// TODO: R nao disponivel no modulo core - passar IDs como parametro`n$1// $2'
$content = $content -replace '(\s+)(imgLogo\.setImageResource\(R\.)', '$1// TODO: R nao disponivel no modulo core - passar resource ID como parametro`n$1// $2'

Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
Write-Host "Referencias a R comentadas em ReciboPrinterHelper.kt" -ForegroundColor Green
Write-Host "=== CORRECAO APLICADA ===" -ForegroundColor Green

