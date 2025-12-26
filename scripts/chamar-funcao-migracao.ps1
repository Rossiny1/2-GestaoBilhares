# Script para chamar função callable migrateUserClaims via HTTP
# Usa token do Firebase CLI para autenticação

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

# Verificar login
Write-Host "Verificando login no Firebase..." -ForegroundColor Yellow
try {
    $loginCheck = & $firebaseCmd login:list 2>&1
    if ($loginCheck -match "No authorized accounts" -or $LASTEXITCODE -ne 0) {
        Write-Host "[ERRO] Nao esta logado no Firebase" -ForegroundColor Red
        Write-Host "Execute: firebase login" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "[OK] Logado no Firebase" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Erro ao verificar login: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verificar se o comando functions:shell está disponível
Write-Host "Verificando disponibilidade do Firebase Functions Shell..." -ForegroundColor Yellow
try {
    $shellCheck = & $firebaseCmd functions:shell --help 2>&1
    if ($LASTEXITCODE -ne 0 -or $shellCheck -match "Unknown command" -or $shellCheck -match "not found") {
        Write-Host "[AVISO] O comando 'functions:shell' nao esta disponivel nesta versao do Firebase CLI" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "OPCOES ALTERNATIVAS:" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "OPCAO 1: Via Firebase Console (Recomendado)" -ForegroundColor Green
        Write-Host "  1. Acesse: https://console.firebase.google.com/project/gestaobilhares/functions" -ForegroundColor White
        Write-Host "  2. Clique na funcao 'migrateUserClaims'" -ForegroundColor White
        Write-Host "  3. Use a aba 'Testing' para executar com: {}" -ForegroundColor White
        Write-Host ""
        Write-Host "OPCAO 2: Via HTTP Request (usando curl ou Postman)" -ForegroundColor Green
        Write-Host "  URL: https://us-central1-gestaobilhares.cloudfunctions.net/migrateUserClaims" -ForegroundColor White
        Write-Host "  Metodo: POST" -ForegroundColor White
        Write-Host "  Headers: Content-Type: application/json" -ForegroundColor White
        Write-Host "  Body: { `"data`": {} }" -ForegroundColor White
        Write-Host ""
        Write-Host "OPCAO 3: Via Node.js Script" -ForegroundColor Green
        Write-Host "  Execute: .\scripts\executar-migracao-direto.ps1" -ForegroundColor White
        Write-Host ""
        
        # Perguntar se deseja abrir o console
        Write-Host "Deseja abrir o Firebase Console agora? (S/N)" -ForegroundColor Cyan
        $resposta = Read-Host
        if ($resposta -eq "S" -or $resposta -eq "s") {
            Start-Process "https://console.firebase.google.com/project/gestaobilhares/functions"
        }
        exit 0
    }
} catch {
    Write-Host "[AVISO] Nao foi possivel verificar o comando functions:shell" -ForegroundColor Yellow
}

Write-Host "[OK] Firebase Functions Shell disponivel" -ForegroundColor Green
Write-Host ""

Write-Host "INSTRUCOES:" -ForegroundColor Green
Write-Host "1. O shell do Firebase Functions sera aberto" -ForegroundColor White
Write-Host "2. Quando aparecer 'firebase >', digite:" -ForegroundColor White
Write-Host "   migrateUserClaims({})" -ForegroundColor Cyan
Write-Host "3. Pressione Enter" -ForegroundColor White
Write-Host "4. Aguarde a execucao (pode levar alguns minutos)" -ForegroundColor White
Write-Host "5. Depois execute: validateUserClaims({}) para validar" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Enter para abrir o shell..." -ForegroundColor Gray
Read-Host

Write-Host ""
Write-Host "Abrindo Firebase Functions Shell..." -ForegroundColor Cyan
Write-Host ""

# Abrir shell do Firebase Functions
try {
    & $firebaseCmd functions:shell --project gestaobilhares
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "[ERRO] Falha ao abrir o Firebase Functions Shell" -ForegroundColor Red
        Write-Host "Tente usar uma das opcoes alternativas listadas acima" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "[ERRO] Erro ao executar comando: $_" -ForegroundColor Red
    Write-Host "Tente usar uma das opcoes alternativas listadas acima" -ForegroundColor Yellow
    exit 1
}

