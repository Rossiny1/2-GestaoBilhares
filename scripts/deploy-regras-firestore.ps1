# Script para fazer deploy das regras do Firestore
# Execute este script apos instalar Firebase CLI

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Deploy de Regras Firestore" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH completo (incluindo Node.js)
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

# Verificar se Firebase CLI existe
if (-not (Test-Path $firebaseCmd)) {
    Write-Host "[ERRO] Firebase CLI nao encontrado em: $firebaseCmd" -ForegroundColor Red
    Write-Host "Execute primeiro: .\fazer-login-e-deploy-indices.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/3] Verificando Firebase CLI..." -ForegroundColor Yellow
try {
    $version = & $firebaseCmd --version 2>&1
    Write-Host "[OK] Firebase CLI: $version" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Firebase CLI nao esta funcionando" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verificar se ja esta logado
Write-Host "[2/3] Verificando login..." -ForegroundColor Yellow
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

# Verificar arquivo de regras
Write-Host "[3/3] Verificando arquivo de regras..." -ForegroundColor Yellow
if (-not (Test-Path "firestore.rules")) {
    Write-Host "[ERRO] Arquivo firestore.rules nao encontrado!" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Arquivo firestore.rules encontrado" -ForegroundColor Green
Write-Host ""

# Verificar/criar firebase.json
if (-not (Test-Path "firebase.json")) {
    Write-Host "[AVISO] firebase.json nao encontrado. Criando..." -ForegroundColor Yellow
    @"
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  }
}
"@ | Out-File -FilePath "firebase.json" -Encoding UTF8
    Write-Host "[OK] firebase.json criado" -ForegroundColor Green
    Write-Host ""
}

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

# Fazer deploy das regras
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Fazendo deploy das regras..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Isso pode levar alguns segundos..." -ForegroundColor Gray
Write-Host ""

try {
    & $firebaseCmd deploy --only firestore:rules --project gestaobilhares

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  Deploy concluido com sucesso!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "As regras do Firestore foram atualizadas." -ForegroundColor Green
        Write-Host ""
        Write-Host "As novas regras estao ativas agora!" -ForegroundColor Yellow
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "[ERRO] Falha no deploy das regras" -ForegroundColor Red
        Write-Host "Verifique se esta logado e se o projeto esta configurado corretamente" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "[ERRO] Erro ao fazer deploy: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

