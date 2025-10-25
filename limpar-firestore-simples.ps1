# Script simples para limpar dados corrompidos do Firestore
Write-Host "LIMPANDO DADOS CORROMPIDOS DO FIRESTORE" -ForegroundColor Yellow
Write-Host "=======================================" -ForegroundColor Yellow

# Configuracoes
$ProjectId = "gestaobilhares-12345"
$EmpresaId = "empresa_001"

Write-Host "Project ID: $ProjectId" -ForegroundColor Cyan
Write-Host "Empresa ID: $EmpresaId" -ForegroundColor Cyan
Write-Host ""

# Verificar Firebase CLI
try {
    $firebaseVersion = firebase --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
    } else {
        throw "Firebase CLI nao encontrado"
    }
} catch {
    Write-Host "ERRO: Firebase CLI nao encontrado!" -ForegroundColor Red
    Write-Host "Instale com: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "INICIANDO LIMPEZA..." -ForegroundColor Cyan

# 1. Verificar se Rota Principal existe
Write-Host "Verificando Rota Principal..." -ForegroundColor Cyan
$rotas = firebase firestore:query "empresas/$EmpresaId/rotas" --project $ProjectId 2>$null
if ($rotas -match "Rota Principal") {
    Write-Host "Rota Principal ja existe" -ForegroundColor Green
} else {
    Write-Host "Criando Rota Principal..." -ForegroundColor Yellow
    $rotaData = @{
        nome = "Rota Principal"
        descricao = "Rota principal do sistema"
        ativa = $true
        dataCriacao = [int64]((Get-Date) - (Get-Date "1970-01-01")).TotalSeconds * 1000
        roomId = 1
        syncTimestamp = [int64]((Get-Date) - (Get-Date "1970-01-01")).TotalSeconds * 1000
    } | ConvertTo-Json -Compress
    
    try {
        firebase firestore:create "empresas/$EmpresaId/rotas" --json-data $rotaData --project $ProjectId 2>$null
        Write-Host "Rota Principal criada com sucesso" -ForegroundColor Green
    } catch {
        Write-Host "Erro ao criar Rota Principal: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 2. Limpar subcollections corrompidas
Write-Host ""
Write-Host "Limpando subcollections corrompidas..." -ForegroundColor Cyan

# Limpar mesas
Write-Host "Limpando mesas..." -ForegroundColor Yellow
try {
    firebase firestore:delete "empresas/$EmpresaId/mesas" --recursive --project $ProjectId 2>$null
    Write-Host "Mesas limpas" -ForegroundColor Green
} catch {
    Write-Host "Erro ao limpar mesas: $($_.Exception.Message)" -ForegroundColor Red
}

# Limpar acertos
Write-Host "Limpando acertos..." -ForegroundColor Yellow
try {
    firebase firestore:delete "empresas/$EmpresaId/acertos" --recursive --project $ProjectId 2>$null
    Write-Host "Acertos limpos" -ForegroundColor Green
} catch {
    Write-Host "Erro ao limpar acertos: $($_.Exception.Message)" -ForegroundColor Red
}

# Limpar colaboradores
Write-Host "Limpando colaboradores..." -ForegroundColor Yellow
try {
    firebase firestore:delete "empresas/$EmpresaId/colaboradores" --recursive --project $ProjectId 2>$null
    Write-Host "Colaboradores limpos" -ForegroundColor Green
} catch {
    Write-Host "Erro ao limpar colaboradores: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "LIMPEZA CONCLUIDA!" -ForegroundColor Green
Write-Host "==================" -ForegroundColor Green
Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "1. Teste a sincronizacao no app" -ForegroundColor White
Write-Host "2. Crie novos clientes, mesas e acertos" -ForegroundColor White
Write-Host "3. Verifique se aparecem no Firestore" -ForegroundColor White
Write-Host ""
Write-Host "Dados corrompidos foram limpos!" -ForegroundColor Green
