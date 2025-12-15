# Script para fazer login no Firebase e deploy dos indices
# Execute este script apos instalar Firebase CLI

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Login e Deploy de Indices Firestore" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH completo (incluindo Node.js)
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

# Verificar se Firebase CLI existe
if (-not (Test-Path $firebaseCmd)) {
    Write-Host "[ERRO] Firebase CLI nao encontrado em: $firebaseCmd" -ForegroundColor Red
    Write-Host "Execute primeiro: .\instalar-firebase-completo.ps1" -ForegroundColor Yellow
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
        Write-Host $loginList -ForegroundColor Gray
    }
} catch {
    Write-Host "[ERRO] Erro ao verificar login: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verificar arquivo de indices
Write-Host "[3/3] Verificando arquivo de indices..." -ForegroundColor Yellow
if (-not (Test-Path "firestore.indexes.json")) {
    Write-Host "[ERRO] Arquivo firestore.indexes.json nao encontrado!" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Arquivo firestore.indexes.json encontrado" -ForegroundColor Green
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
& $firebaseCmd use gestaobilhares 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[AVISO] Nao foi possivel selecionar projeto automaticamente" -ForegroundColor Yellow
    Write-Host "Tentando deploy com projeto especificado..." -ForegroundColor Yellow
}

Write-Host ""

# Fazer deploy dos indices
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Fazendo deploy dos indices..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Isso pode levar alguns minutos..." -ForegroundColor Gray
Write-Host ""

try {
    $deployOutput = & $firebaseCmd deploy --only firestore:indexes --project gestaobilhares 2>&1
    $deployResult = $deployOutput | Out-String
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  Deploy concluido com sucesso!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Os indices estao sendo criados no Firestore." -ForegroundColor Yellow
        Write-Host "Isso pode levar 5-15 minutos." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Acompanhe o progresso em:" -ForegroundColor Cyan
        Write-Host "  https://console.firebase.google.com/project/gestaobilhares/firestore/indexes" -ForegroundColor White
        Write-Host ""
        Write-Host "Voce recebera um email quando os indices estiverem prontos." -ForegroundColor Gray
    } else {
        Write-Host ""
        Write-Host "[AVISO] Deploy retornou codigo de erro" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Saida do comando:" -ForegroundColor Cyan
        Write-Host $deployResult -ForegroundColor Gray
        
        # Verificar se o erro é sobre índices não necessários
        if ($deployResult -match "not necessary") {
            Write-Host ""
            Write-Host "========================================" -ForegroundColor Yellow
            Write-Host "  INDICES DE CAMPO UNICO REMOVIDOS" -ForegroundColor Yellow
            Write-Host "========================================" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "O Firestore cria indices de campo unico automaticamente." -ForegroundColor White
            Write-Host "Apenas indices compostos foram mantidos no arquivo." -ForegroundColor White
            Write-Host ""
            Write-Host "IMPORTANTE:" -ForegroundColor Cyan
            Write-Host "Os indices de campo unico (lastModified) serao criados automaticamente" -ForegroundColor White
            Write-Host "quando o app executar as queries pela primeira vez." -ForegroundColor White
            Write-Host ""
            Write-Host "Isso significa que:" -ForegroundColor Yellow
            Write-Host "1. Primeira execucao pode ser mais lenta (criando indices)" -ForegroundColor White
            Write-Host "2. Depois funcionara normalmente" -ForegroundColor White
            Write-Host "3. Nao ha necessidade de criar manualmente" -ForegroundColor White
            Write-Host ""
            Write-Host "Os indices compostos (items) foram deployados com sucesso!" -ForegroundColor Green
        } else {
            Write-Host ""
            Write-Host "[ERRO] Falha no deploy dos indices" -ForegroundColor Red
            Write-Host "Verifique se esta logado e se o projeto esta configurado corretamente" -ForegroundColor Yellow
            exit 1
        }
    }
} catch {
    Write-Host ""
    Write-Host "[ERRO] Erro ao fazer deploy: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

