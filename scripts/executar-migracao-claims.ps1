# Script para executar migração de claims de usuários
# Chama a função callable migrateUserClaims do Firebase

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Migração de Claims de Usuários" -ForegroundColor Cyan
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

Write-Host "[1/3] Verificando login..." -ForegroundColor Yellow
try {
    $loginList = & $firebaseCmd login:list 2>&1
    if ($LASTEXITCODE -ne 0 -or $loginList -match "No authorized accounts") {
        Write-Host "[ERRO] Nao esta logado. Execute: firebase login" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Logado no Firebase" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Erro ao verificar login: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Obter token de autenticação
Write-Host "[2/3] Obtendo token de autenticacao..." -ForegroundColor Yellow
try {
    # Usar Firebase CLI para obter token
    $tokenOutput = & $firebaseCmd login:ci 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[AVISO] Nao foi possivel obter token automaticamente" -ForegroundColor Yellow
        Write-Host "Voce precisara executar a migracao manualmente via Firebase Console" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Opcao 1: Via Firebase Console" -ForegroundColor Cyan
        Write-Host "  1. Acesse: https://console.firebase.google.com/project/gestaobilhares/functions" -ForegroundColor White
        Write-Host "  2. Encontre a funcao 'migrateUserClaims'" -ForegroundColor White
        Write-Host "  3. Execute via interface ou use o emulador" -ForegroundColor White
        Write-Host ""
        Write-Host "Opcao 2: Via Node.js (requer firebase-admin configurado)" -ForegroundColor Cyan
        Write-Host "  cd functions" -ForegroundColor White
        Write-Host "  node ../scripts/migrar-claims-usuarios.js" -ForegroundColor White
        Write-Host ""
        exit 0
    }
} catch {
    Write-Host "[AVISO] Nao foi possivel obter token automaticamente" -ForegroundColor Yellow
}

Write-Host ""

# Instruções para execução manual
Write-Host "[3/3] Instrucoes para executar migracao:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Como as funcoes callable requerem autenticacao, voce tem duas opcoes:" -ForegroundColor Cyan
Write-Host ""
Write-Host "OPCAO 1: Via Firebase Console (Recomendado)" -ForegroundColor Green
Write-Host "  1. Acesse: https://console.firebase.google.com/project/gestaobilhares/functions" -ForegroundColor White
Write-Host "  2. Clique na funcao 'migrateUserClaims'" -ForegroundColor White
Write-Host "  3. Use a aba 'Testing' para executar com dados vazios: {}" -ForegroundColor White
Write-Host ""
Write-Host "OPCAO 2: Via Firebase CLI (se configurado)" -ForegroundColor Green
Write-Host "  firebase functions:shell" -ForegroundColor White
Write-Host "  migrateUserClaims({})" -ForegroundColor White
Write-Host ""
Write-Host "OPCAO 3: Validar claims antes de migrar:" -ForegroundColor Green
Write-Host "  Execute: .\scripts\validar-claims-usuarios.ps1" -ForegroundColor White
Write-Host ""

