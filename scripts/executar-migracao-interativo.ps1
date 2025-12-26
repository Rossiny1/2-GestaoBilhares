# Script para executar migração via Firebase CLI Shell interativo

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Executar Migração via Firebase CLI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

Write-Host "Abrindo Firebase Functions Shell..." -ForegroundColor Yellow
Write-Host ""
Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Quando o shell abrir, digite: migrateUserClaims({})" -ForegroundColor White
Write-Host "2. Pressione Enter" -ForegroundColor White
Write-Host "3. Aguarde a execucao (pode levar alguns minutos)" -ForegroundColor White
Write-Host "4. Depois execute: validateUserClaims({}) para validar" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Enter para continuar..." -ForegroundColor Gray
Read-Host

# Abrir shell do Firebase Functions
& $firebaseCmd functions:shell --project gestaobilhares

