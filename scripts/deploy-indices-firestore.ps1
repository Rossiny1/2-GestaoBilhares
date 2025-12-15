# Script para fazer deploy dos índices do Firestore
# Requer Firebase CLI instalado: npm install -g firebase-tools

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Deploy de Índices Firestore" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH completo (incluindo Node.js)
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

# Verificar se Firebase CLI está instalado
Write-Host "[1/4] Verificando Firebase CLI..." -ForegroundColor Yellow

if (-not (Test-Path $firebaseCmd)) {
    # Tentar encontrar no PATH
    $firebaseInstalled = Get-Command firebase -ErrorAction SilentlyContinue
    if ($firebaseInstalled) {
        $firebaseCmd = "firebase"
    } else {
        Write-Host "[ERRO] Firebase CLI não encontrado!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Para instalar, execute:" -ForegroundColor Yellow
        Write-Host "  npm install -g firebase-tools" -ForegroundColor White
        Write-Host ""
        Write-Host "Ou crie os índices manualmente no Firebase Console:" -ForegroundColor Yellow
        Write-Host "  https://console.firebase.google.com/project/gestaobilhares/firestore/indexes" -ForegroundColor White
        Write-Host ""
        Write-Host "Consulte o guia: GUIA-CRIACAO-INDICES-FIRESTORE.md" -ForegroundColor Yellow
        exit 1
    }
}

try {
    $version = & $firebaseCmd --version 2>&1
    Write-Host "[OK] Firebase CLI encontrado: $version" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Firebase CLI não está funcionando" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verificar se está logado
Write-Host "[2/4] Verificando login no Firebase..." -ForegroundColor Yellow
$firebaseUser = & $firebaseCmd login:list 2>&1 | Select-String "Logged in as"

if (-not $firebaseUser) {
    Write-Host "[AVISO] Não está logado no Firebase. Fazendo login..." -ForegroundColor Yellow
    & $firebaseCmd login
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERRO] Falha no login do Firebase" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[OK] Logado no Firebase" -ForegroundColor Green
    Write-Host "  $firebaseUser" -ForegroundColor Gray
}
Write-Host ""

# Verificar se firebase.json existe
Write-Host "[3/4] Verificando configuração do Firebase..." -ForegroundColor Yellow
if (-not (Test-Path "firebase.json")) {
    Write-Host "[AVISO] firebase.json não encontrado. Inicializando Firestore..." -ForegroundColor Yellow
    & $firebaseCmd init firestore
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERRO] Falha na inicialização do Firestore" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[OK] firebase.json encontrado" -ForegroundColor Green
}
Write-Host ""

# Verificar se firestore.indexes.json existe
if (-not (Test-Path "firestore.indexes.json")) {
    Write-Host "[ERRO] firestore.indexes.json não encontrado!" -ForegroundColor Red
    Write-Host "O arquivo deve estar na raiz do projeto." -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] firestore.indexes.json encontrado" -ForegroundColor Green
Write-Host ""

# Fazer deploy dos índices
Write-Host "[4/4] Fazendo deploy dos índices..." -ForegroundColor Yellow
Write-Host ""
& $firebaseCmd deploy --only firestore:indexes

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  Deploy concluído com sucesso!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Os índices estão sendo criados no Firestore." -ForegroundColor Yellow
    Write-Host "Isso pode levar alguns minutos (5-15 min)." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Acompanhe o progresso em:" -ForegroundColor Cyan
    Write-Host "  https://console.firebase.google.com/project/gestaobilhares/firestore/indexes" -ForegroundColor White
    Write-Host ""
    Write-Host "Você receberá um email quando os índices estiverem prontos." -ForegroundColor Gray
} else {
    Write-Host ""
    Write-Host "[ERRO] Falha no deploy dos índices" -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternativa: Crie os índices manualmente no Firebase Console:" -ForegroundColor Yellow
    Write-Host "  https://console.firebase.google.com/project/gestaobilhares/firestore/indexes" -ForegroundColor White
    Write-Host ""
    Write-Host "Consulte o guia: GUIA-CRIACAO-INDICES-FIRESTORE.md" -ForegroundColor Yellow
    exit 1
}

