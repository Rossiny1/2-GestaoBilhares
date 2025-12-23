# Script para fazer deploy das Firebase Functions
# Compila TypeScript e faz deploy

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Deploy de Firebase Functions" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH completo (incluindo Node.js)
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

# Verificar se Firebase CLI existe
if (-not (Test-Path $firebaseCmd)) {
    Write-Host "[ERRO] Firebase CLI nao encontrado em: $firebaseCmd" -ForegroundColor Red
    Write-Host "Execute primeiro: .\scripts\instalar-firebase-cli.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/4] Verificando Firebase CLI..." -ForegroundColor Yellow
try {
    $version = & $firebaseCmd --version 2>&1
    Write-Host "[OK] Firebase CLI: $version" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Firebase CLI nao esta funcionando" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verificar se ja esta logado
Write-Host "[2/4] Verificando login..." -ForegroundColor Yellow
try {
    $loginList = & $firebaseCmd login:list 2>&1
    if ($loginList -match "No authorized accounts") {
        Write-Host "[AVISO] Nao esta logado. Fazendo login..." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "O navegador sera aberto para autenticacao." -ForegroundColor Cyan
        Write-Host "Aguarde e faca login com sua conta Google do Firebase." -ForegroundColor Cyan
        Write-Host ""
        
        & $firebaseCmd login
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[ERRO] Falha no login" -ForegroundColor Red
            Write-Host "Tente executar manualmente: firebase login" -ForegroundColor Yellow
            exit 1
        }
        
        Write-Host ""
        Write-Host "[OK] Login realizado com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "[OK] Ja esta logado" -ForegroundColor Green
    }
} catch {
    Write-Host "[ERRO] Erro ao verificar login: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verificar Node.js e npm
Write-Host "[3/4] Verificando Node.js e npm..." -ForegroundColor Yellow
try {
    $nodeVersion = & node --version 2>&1
    $npmVersion = & npm --version 2>&1
    Write-Host "[OK] Node.js: $nodeVersion" -ForegroundColor Green
    Write-Host "[OK] npm: $npmVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Node.js ou npm nao encontrados" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Compilar TypeScript
Write-Host "[4/4] Compilando TypeScript..." -ForegroundColor Yellow
Set-Location functions
try {
    Write-Host "Instalando dependencias (se necessario)..." -ForegroundColor Gray
    & npm install 2>&1 | Out-Null
    
    Write-Host "Compilando TypeScript..." -ForegroundColor Gray
    & npm run build
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERRO] Falha na compilacao TypeScript" -ForegroundColor Red
        Set-Location ..
        exit 1
    }
    
    Write-Host "[OK] Compilacao concluida!" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Erro ao compilar: $($_.Exception.Message)" -ForegroundColor Red
    Set-Location ..
    exit 1
}

Set-Location ..

Write-Host ""

# Selecionar projeto Firebase
Write-Host "Selecionando projeto Firebase..." -ForegroundColor Yellow
try {
    & $firebaseCmd use gestaobilhares --project gestaobilhares 2>&1 | Out-Null
    Write-Host "[OK] Projeto 'gestaobilhares' selecionado." -ForegroundColor Green
} catch {
    Write-Host "[AVISO] Nao foi possivel selecionar projeto automaticamente" -ForegroundColor Yellow
    Write-Host "Tentando deploy com projeto especificado..." -ForegroundColor Yellow
}

Write-Host ""

# Fazer deploy das functions
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Fazendo deploy das Functions..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Isso pode levar alguns minutos..." -ForegroundColor Gray
Write-Host ""

try {
    & $firebaseCmd deploy --only functions --project gestaobilhares

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  Deploy concluido com sucesso!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "As Firebase Functions foram atualizadas." -ForegroundColor Green
        Write-Host ""
        Write-Host "Funcoes deployadas:" -ForegroundColor Yellow
        Write-Host "  - onUserCreated" -ForegroundColor White
        Write-Host "  - onCollaboratorUpdated" -ForegroundColor White
        Write-Host "  - onColaboradorRotaUpdated" -ForegroundColor White
        Write-Host "  - migrateUserClaims" -ForegroundColor White
        Write-Host "  - validateUserClaims" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "[ERRO] Falha no deploy das Functions" -ForegroundColor Red
        Write-Host "Verifique se esta logado e se o projeto esta configurado corretamente" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "[ERRO] Erro ao fazer deploy: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

