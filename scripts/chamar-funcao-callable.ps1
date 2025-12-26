# Script para chamar função callable do Firebase
# Requer estar autenticado no Firebase CLI

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Executar Migração de Claims" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH completo
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

# Verificar se Firebase CLI existe
if (-not (Test-Path $firebaseCmd)) {
    Write-Host "[ERRO] Firebase CLI nao encontrado em: $firebaseCmd" -ForegroundColor Red
    Write-Host "Instale o Firebase CLI com: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Firebase CLI encontrado" -ForegroundColor Green
Write-Host ""

Write-Host "Para executar a função callable, você tem duas opções:" -ForegroundColor Yellow
Write-Host ""
Write-Host "OPÇÃO 1: Via Firebase Console (Mais Fácil)" -ForegroundColor Green
Write-Host "  1. Clique na função 'migrateUserClaims' na lista" -ForegroundColor White
Write-Host "  2. Isso abrirá os detalhes da função" -ForegroundColor White
Write-Host "  3. Procure por uma aba 'Testing' ou 'Testar' no topo" -ForegroundColor White
Write-Host "  4. Se não houver, procure por um botão 'Testar função' ou 'Invoke'" -ForegroundColor White
Write-Host "  5. Digite {} no campo de dados e clique em 'Executar'" -ForegroundColor White
Write-Host ""
Write-Host "OPÇÃO 2: Via URL Direta (Requer Autenticação)" -ForegroundColor Green
Write-Host "  URL: https://us-central1-gestaobilhares.cloudfunctions.net/migrateUserClaims" -ForegroundColor White
Write-Host "  Método: POST" -ForegroundColor White
Write-Host "  Headers: Authorization: Bearer [TOKEN]" -ForegroundColor White
Write-Host "  Body: { 'data': {} }" -ForegroundColor White
Write-Host ""
Write-Host "OPÇÃO 3: Via Firebase CLI Shell" -ForegroundColor Green
Write-Host "  Execute: firebase functions:shell" -ForegroundColor White
Write-Host "  Depois digite: migrateUserClaims({})" -ForegroundColor White
Write-Host ""

# Tentar abrir o Firebase Console
Write-Host "Deseja abrir o Firebase Console agora? (S/N)" -ForegroundColor Cyan
$resposta = Read-Host

if ($resposta -eq "S" -or $resposta -eq "s") {
    Write-Host ""
    Write-Host "Abrindo Firebase Console..." -ForegroundColor Yellow
    Start-Process "https://console.firebase.google.com/project/gestaobilhares/functions"
    Write-Host ""
    Write-Host "Quando a página abrir:" -ForegroundColor Yellow
    Write-Host "  1. Clique na função 'migrateUserClaims'" -ForegroundColor White
    Write-Host "  2. Procure pela aba 'Testing' ou botão 'Testar'" -ForegroundColor White
    Write-Host "  3. Execute com dados vazios: {}" -ForegroundColor White
}

Write-Host ""

