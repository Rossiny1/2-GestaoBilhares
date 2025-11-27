# Script para instalar Firebase CLI recarregando PATH automaticamente
# Use este script se Node.js foi instalado mas comandos nao funcionam

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Instalacao Firebase CLI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH para incluir Node.js
Write-Host "[1/3] Recarregando variaveis de ambiente..." -ForegroundColor Yellow
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
Write-Host "[OK] PATH recarregado" -ForegroundColor Green
Write-Host ""

# Verificar Node.js
Write-Host "[2/3] Verificando Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Node.js encontrado: $nodeVersion" -ForegroundColor Green
    } else {
        throw "Node.js nao encontrado"
    }
} catch {
    Write-Host "[ERRO] Node.js nao encontrado mesmo apos recarregar PATH" -ForegroundColor Red
    Write-Host ""
    Write-Host "Solucao:" -ForegroundColor Yellow
    Write-Host "1. Feche e reabra o PowerShell completamente" -ForegroundColor White
    Write-Host "2. Execute: node --version" -ForegroundColor White
    Write-Host "3. Se funcionar, execute: npm install -g firebase-tools" -ForegroundColor White
    exit 1
}

Write-Host ""

# Verificar npm
Write-Host "Verificando npm..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] npm encontrado: $npmVersion" -ForegroundColor Green
    } else {
        throw "npm nao encontrado"
    }
} catch {
    Write-Host "[ERRO] npm nao encontrado" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verificar se Firebase CLI ja esta instalado
Write-Host "[3/3] Verificando Firebase CLI..." -ForegroundColor Yellow
try {
    $firebaseVersion = firebase --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Firebase CLI ja esta instalado: $firebaseVersion" -ForegroundColor Green
        Write-Host ""
        Write-Host "Proximos passos:" -ForegroundColor Cyan
        Write-Host "1. Fazer login: firebase login" -ForegroundColor White
        Write-Host "2. Deploy indices: .\deploy-indices-firestore.ps1" -ForegroundColor White
        exit 0
    }
} catch {
    # Firebase CLI nao esta instalado, continuar
}

# Instalar Firebase CLI
Write-Host "Instalando Firebase CLI..." -ForegroundColor Cyan
Write-Host "Isso pode levar 1-2 minutos. Aguarde..." -ForegroundColor Gray
Write-Host ""

try {
    npm install -g firebase-tools
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[OK] Firebase CLI instalado com sucesso!" -ForegroundColor Green
        Write-Host ""
        
        # Verificar instalacao
        $firebaseVersion = firebase --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Versao instalada: $firebaseVersion" -ForegroundColor Cyan
        }
        
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  Instalacao concluida!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Proximos passos:" -ForegroundColor Cyan
        Write-Host "1. Fazer login: firebase login" -ForegroundColor White
        Write-Host "2. Deploy indices: .\deploy-indices-firestore.ps1" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host "[ERRO] Falha na instalacao do Firebase CLI" -ForegroundColor Red
        Write-Host "Tente executar manualmente: npm install -g firebase-tools" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "[ERRO] Erro ao instalar Firebase CLI: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Tente executar manualmente:" -ForegroundColor Yellow
    Write-Host "  npm install -g firebase-tools" -ForegroundColor White
    exit 1
}

