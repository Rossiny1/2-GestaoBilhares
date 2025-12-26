# Script para executar migração de claims diretamente via Firebase CLI
# Usa o shell do Firebase Functions para executar a função callable

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Executar Migração de Claims" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

Write-Host "[1/2] Verificando Firebase CLI..." -ForegroundColor Yellow
if (-not (Test-Path $firebaseCmd)) {
    Write-Host "[ERRO] Firebase CLI nao encontrado" -ForegroundColor Red
    exit 1
}

$version = & $firebaseCmd --version 2>&1
Write-Host "[OK] Firebase CLI: $version" -ForegroundColor Green
Write-Host ""

Write-Host "[2/2] Executando migracao..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Abrindo Firebase Functions Shell..." -ForegroundColor Cyan
Write-Host "Quando o shell abrir, digite exatamente:" -ForegroundColor Yellow
Write-Host "  migrateUserClaims({})" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Enter para continuar..." -ForegroundColor Gray
Read-Host

# Abrir shell do Firebase Functions
& $firebaseCmd functions:shell --project gestaobilhares

