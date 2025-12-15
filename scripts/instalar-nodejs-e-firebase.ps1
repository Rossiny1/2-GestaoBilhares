# Script para instalar Node.js e Firebase CLI automaticamente
# Execute este script quando estiver pronto

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Instalacao Node.js e Firebase CLI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se Node.js ja esta instalado
Write-Host "[1/3] Verificando Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Node.js ja esta instalado: $nodeVersion" -ForegroundColor Green
        $nodeInstalled = $true
    } else {
        $nodeInstalled = $false
    }
} catch {
    $nodeInstalled = $false
}

if (-not $nodeInstalled) {
    Write-Host "[AVISO] Node.js nao encontrado. Tentando instalar..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Instalando Node.js LTS via winget..." -ForegroundColor Cyan
    Write-Host "Isso pode levar alguns minutos..." -ForegroundColor Gray
    Write-Host ""
    
    try {
        # Tentar instalar via winget
        winget install OpenJS.NodeJS.LTS --silent --accept-package-agreements --accept-source-agreements
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] Node.js instalado com sucesso!" -ForegroundColor Green
            Write-Host ""
            Write-Host "IMPORTANTE: Feche e reabra o PowerShell para carregar as variaveis de ambiente" -ForegroundColor Yellow
            Write-Host "Depois execute este script novamente para instalar Firebase CLI" -ForegroundColor Yellow
            exit 0
        } else {
            Write-Host "[ERRO] Falha na instalacao via winget" -ForegroundColor Red
            Write-Host ""
            Write-Host "Instalacao manual necessaria:" -ForegroundColor Yellow
            Write-Host "1. Acesse: https://nodejs.org/" -ForegroundColor White
            Write-Host "2. Baixe a versao LTS (botao verde)" -ForegroundColor White
            Write-Host "3. Execute o instalador" -ForegroundColor White
            Write-Host "4. Marque 'Add to PATH' durante instalacao" -ForegroundColor White
            Write-Host "5. Feche e reabra o PowerShell" -ForegroundColor White
            exit 1
        }
    } catch {
        Write-Host "[ERRO] Erro ao instalar Node.js: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host ""
        Write-Host "Instalacao manual necessaria:" -ForegroundColor Yellow
        Write-Host "1. Acesse: https://nodejs.org/" -ForegroundColor White
        Write-Host "2. Baixe a versao LTS (botao verde)" -ForegroundColor White
        Write-Host "3. Execute o instalador" -ForegroundColor White
        Write-Host "4. Marque 'Add to PATH' durante instalacao" -ForegroundColor White
        Write-Host "5. Feche e reabra o PowerShell" -ForegroundColor White
        exit 1
    }
}

Write-Host ""

# Verificar npm
Write-Host "[2/3] Verificando npm..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] npm encontrado: $npmVersion" -ForegroundColor Green
    } else {
        throw "npm nao encontrado"
    }
} catch {
    Write-Host "[ERRO] npm nao encontrado. Reinstale Node.js." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Instalar Firebase CLI
Write-Host "[3/3] Instalando Firebase CLI..." -ForegroundColor Yellow
try {
    $firebaseInstalled = Get-Command firebase -ErrorAction SilentlyContinue
    if ($firebaseInstalled) {
        $firebaseVersion = firebase --version 2>&1
        Write-Host "[OK] Firebase CLI ja esta instalado: $firebaseVersion" -ForegroundColor Green
    } else {
        Write-Host "Instalando Firebase CLI via npm..." -ForegroundColor Cyan
        Write-Host "Isso pode levar 1-2 minutos..." -ForegroundColor Gray
        Write-Host ""
        
        npm install -g firebase-tools
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "[OK] Firebase CLI instalado com sucesso!" -ForegroundColor Green
        } else {
            Write-Host "[ERRO] Falha na instalacao do Firebase CLI" -ForegroundColor Red
            Write-Host "Tente executar manualmente: npm install -g firebase-tools" -ForegroundColor Yellow
            exit 1
        }
    }
} catch {
    Write-Host "[ERRO] Erro ao instalar Firebase CLI: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Tente executar manualmente: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Instalacao concluida!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Verificar versoes finais
Write-Host "Versoes instaladas:" -ForegroundColor Cyan
try {
    $nodeVer = node --version
    Write-Host "  Node.js: $nodeVer" -ForegroundColor White
} catch {}

try {
    $npmVer = npm --version
    Write-Host "  npm: $npmVer" -ForegroundColor White
} catch {}

try {
    $firebaseVer = firebase --version
    Write-Host "  Firebase CLI: $firebaseVer" -ForegroundColor White
} catch {}

Write-Host ""
Write-Host "Proximos passos:" -ForegroundColor Cyan
Write-Host "1. Fazer login: firebase login" -ForegroundColor White
Write-Host "2. Deploy indices: .\deploy-indices-firestore.ps1" -ForegroundColor White
Write-Host ""

