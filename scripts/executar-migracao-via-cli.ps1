# Script para executar migração de claims via Firebase Functions callable
# Usa Firebase CLI para autenticação

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Migração de Claims via Firebase CLI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

# Verificar Firebase CLI
if (-not (Test-Path $firebaseCmd)) {
    Write-Host "[ERRO] Firebase CLI nao encontrado" -ForegroundColor Red
    exit 1
}

Write-Host "[1/3] Verificando login..." -ForegroundColor Yellow
try {
    $loginList = & $firebaseCmd login:list 2>&1
    if ($loginList -match "No authorized accounts") {
        Write-Host "[ERRO] Nao esta logado. Execute: firebase login" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Logado no Firebase" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Erro ao verificar login" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Obter token
Write-Host "[2/3] Obtendo token de autenticacao..." -ForegroundColor Yellow
Write-Host "Isso pode abrir o navegador para autenticacao..." -ForegroundColor Gray
Write-Host ""

try {
    # Usar firebase functions:shell para executar a função
    Write-Host "Executando migracao via Firebase Functions Shell..." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Digite o seguinte comando no shell que sera aberto:" -ForegroundColor Yellow
    Write-Host "  migrateUserClaims({})" -ForegroundColor White
    Write-Host ""
    Write-Host "Pressione Enter para continuar..." -ForegroundColor Gray
    Read-Host
    
    # Abrir shell do Firebase Functions
    & $firebaseCmd functions:shell --project gestaobilhares
    
} catch {
    Write-Host "[ERRO] Erro ao executar: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Instrucoes Manuais" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Como alternativa, voce pode executar via Firebase Console:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Acesse: https://console.firebase.google.com/project/gestaobilhares/functions" -ForegroundColor White
Write-Host "2. Clique na funcao 'migrateUserClaims'" -ForegroundColor White
Write-Host "3. Vá na aba 'Testing'" -ForegroundColor White
Write-Host "4. Execute com dados vazios: {}" -ForegroundColor White
Write-Host ""

