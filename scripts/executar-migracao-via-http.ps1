# Script para executar migração via HTTP callable
# Usa token do Firebase CLI para autenticação

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Executar Migração de Claims via HTTP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

Write-Host "Como a aba 'Testing' nao esta disponivel no console," -ForegroundColor Yellow
Write-Host "vamos executar a funcao diretamente via HTTP." -ForegroundColor Yellow
Write-Host ""
Write-Host "OPCAO 1: Via Firebase CLI Shell (Recomendado)" -ForegroundColor Green
Write-Host "  Execute no terminal:" -ForegroundColor White
Write-Host "    firebase functions:shell --project gestaobilhares" -ForegroundColor Cyan
Write-Host "  Depois digite:" -ForegroundColor White
Write-Host "    migrateUserClaims({})" -ForegroundColor Cyan
Write-Host ""
Write-Host "OPCAO 2: Via Script Node.js Local" -ForegroundColor Green
Write-Host "  Vou criar um script que executa a migracao localmente" -ForegroundColor White
Write-Host "  usando o Firebase Admin SDK." -ForegroundColor White
Write-Host ""

$opcao = Read-Host "Escolha opcao (1 ou 2)"

if ($opcao -eq "1") {
    Write-Host ""
    Write-Host "Abrindo Firebase Functions Shell..." -ForegroundColor Cyan
    Write-Host "Quando abrir, digite: migrateUserClaims({})" -ForegroundColor Yellow
    Write-Host ""
    & $firebaseCmd functions:shell --project gestaobilhares
} elseif ($opcao -eq "2") {
    Write-Host ""
    Write-Host "Executando script local..." -ForegroundColor Cyan
    Write-Host ""
    Set-Location functions
    node executar-migracao-local.js
    Set-Location ..
} else {
    Write-Host "[ERRO] Opcao invalida" -ForegroundColor Red
    exit 1
}

