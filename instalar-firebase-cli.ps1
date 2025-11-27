# Script para verificar e guiar instalacao do Firebase CLI
# Requer Node.js instalado primeiro

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Instalacao Firebase CLI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# PASSO 1: Verificar Node.js
Write-Host "[1/4] Verificando Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Node.js encontrado: $nodeVersion" -ForegroundColor Green
    } else {
        throw "Node.js nao encontrado"
    }
} catch {
    Write-Host "[ERRO] Node.js nao esta instalado!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Para instalar Node.js:" -ForegroundColor Yellow
    Write-Host "1. Acesse: https://nodejs.org/" -ForegroundColor White
    Write-Host "2. Baixe a versao LTS (botao verde)" -ForegroundColor White
    Write-Host "3. Execute o instalador" -ForegroundColor White
    Write-Host "4. IMPORTANTE: Marque 'Add to PATH' durante instalacao" -ForegroundColor Yellow
    Write-Host "5. Feche e reabra o PowerShell apos instalar" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Ou consulte: GUIA-INSTALAR-FIREBASE-CLI.md" -ForegroundColor Cyan
    exit 1
}

Write-Host ""

# PASSO 2: Verificar npm
Write-Host "[2/4] Verificando npm..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] npm encontrado: $npmVersion" -ForegroundColor Green
    } else {
        throw "npm nao encontrado"
    }
} catch {
    Write-Host "[ERRO] npm nao esta instalado!" -ForegroundColor Red
    Write-Host "npm deveria vir junto com Node.js. Tente reinstalar Node.js." -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# PASSO 3: Verificar Firebase CLI
Write-Host "[3/4] Verificando Firebase CLI..." -ForegroundColor Yellow
try {
    $firebaseVersion = firebase --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Firebase CLI ja esta instalado: $firebaseVersion" -ForegroundColor Green
        Write-Host ""
        Write-Host "Para fazer login:" -ForegroundColor Yellow
        Write-Host "  firebase login" -ForegroundColor White
        Write-Host ""
        Write-Host "Para fazer deploy dos indices:" -ForegroundColor Yellow
        Write-Host "  .\deploy-indices-firestore.ps1" -ForegroundColor White
        Write-Host "  OU" -ForegroundColor Gray
        Write-Host "  firebase deploy --only firestore:indexes" -ForegroundColor White
        exit 0
    } else {
        throw "Firebase CLI nao encontrado"
    }
} catch {
    Write-Host "[AVISO] Firebase CLI nao esta instalado" -ForegroundColor Yellow
    Write-Host ""
    
    # Perguntar se quer instalar
    $resposta = Read-Host "Deseja instalar o Firebase CLI agora? (S/N)"
    if ($resposta -eq "S" -or $resposta -eq "s" -or $resposta -eq "Y" -or $resposta -eq "y") {
        Write-Host ""
        Write-Host "[4/4] Instalando Firebase CLI..." -ForegroundColor Yellow
        Write-Host "Isso pode levar 1-2 minutos..." -ForegroundColor Gray
        Write-Host ""
        
        try {
            npm install -g firebase-tools
            if ($LASTEXITCODE -eq 0) {
                Write-Host ""
                Write-Host "[OK] Firebase CLI instalado com sucesso!" -ForegroundColor Green
                Write-Host ""
                Write-Host "Proximos passos:" -ForegroundColor Cyan
                Write-Host "1. Fazer login: firebase login" -ForegroundColor White
                Write-Host "2. Deploy indices: .\deploy-indices-firestore.ps1" -ForegroundColor White
            } else {
                Write-Host "[ERRO] Falha na instalacao do Firebase CLI" -ForegroundColor Red
                Write-Host "Tente executar manualmente: npm install -g firebase-tools" -ForegroundColor Yellow
                exit 1
            }
        } catch {
            Write-Host "[ERRO] Erro ao instalar Firebase CLI: $($_.Exception.Message)" -ForegroundColor Red
            Write-Host "Tente executar manualmente: npm install -g firebase-tools" -ForegroundColor Yellow
            exit 1
        }
    } else {
        Write-Host ""
        Write-Host "Instalacao cancelada." -ForegroundColor Yellow
        Write-Host "Para instalar manualmente, execute:" -ForegroundColor White
        Write-Host "  npm install -g firebase-tools" -ForegroundColor Gray
        exit 0
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Verificacao concluida!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

