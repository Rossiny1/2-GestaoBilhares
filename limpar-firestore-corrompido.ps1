# Script para limpar dados corrompidos do Firestore
# Executa limpeza seletiva mantendo dados válidos

Write-Host "🧹 LIMPEZA DO FIRESTORE - DADOS CORROMPIDOS" -ForegroundColor Yellow
Write-Host "===============================================" -ForegroundColor Yellow

# Configurações
$ProjectId = "gestaobilhares-12345"  # Substitua pelo seu Project ID
$EmpresaId = "empresa_001"

Write-Host "📋 Configurações:" -ForegroundColor Cyan
Write-Host "   Project ID: $ProjectId"
Write-Host "   Empresa ID: $EmpresaId"
Write-Host ""

# Verificar se Firebase CLI está instalado
try {
    $firebaseVersion = firebase --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
    } else {
        throw "Firebase CLI não encontrado"
    }
} catch {
    Write-Host "❌ Firebase CLI não encontrado!" -ForegroundColor Red
    Write-Host "   Instale com: npm install -g firebase-tools" -ForegroundColor Yellow
    Write-Host "   Ou use a limpeza manual no console do Firestore" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "🔍 Verificando dados corrompidos..." -ForegroundColor Cyan

# Verificar dados corrompidos
Write-Host "📊 Verificando subcollections:" -ForegroundColor Cyan

# Verificar rotas
Write-Host "   🔍 Verificando rotas..."
$rotasVazias = firebase firestore:query "empresas/$EmpresaId/rotas" --project $ProjectId 2>$null | Select-String "{}" | Measure-Object
if ($rotasVazias.Count -gt 0) {
    Write-Host "   ⚠️ Encontradas $($rotasVazias.Count) rotas com dados vazios" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ Rotas OK" -ForegroundColor Green
}

# Verificar mesas
Write-Host "   🔍 Verificando mesas..."
$mesasVazias = firebase firestore:query "empresas/$EmpresaId/mesas" --project $ProjectId 2>$null | Select-String "{}" | Measure-Object
if ($mesasVazias.Count -gt 0) {
    Write-Host "   ⚠️ Encontradas $($mesasVazias.Count) mesas com dados vazios" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ Mesas OK" -ForegroundColor Green
}

# Verificar acertos
Write-Host "   🔍 Verificando acertos..."
$acertosVazios = firebase firestore:query "empresas/$EmpresaId/acertos" --project $ProjectId 2>$null | Select-String "{}" | Measure-Object
if ($acertosVazios.Count -gt 0) {
    Write-Host "   ⚠️ Encontradas $($acertosVazios.Count) acertos com dados vazios" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ Acertos OK" -ForegroundColor Green
}

# Verificar colaboradores
Write-Host "   🔍 Verificando colaboradores..."
$colaboradoresVazios = firebase firestore:query "empresas/$EmpresaId/colaboradores" --project $ProjectId 2>$null | Select-String "{}" | Measure-Object
if ($colaboradoresVazios.Count -gt 0) {
    Write-Host "   ⚠️ Encontrados $($colaboradoresVazios.Count) colaboradores com dados vazios" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ Colaboradores OK" -ForegroundColor Green
}

Write-Host ""
Write-Host "🧹 LIMPEZA AUTOMÁTICA" -ForegroundColor Yellow
Write-Host "====================" -ForegroundColor Yellow

# Confirmar limpeza
$confirmar = Read-Host "Deseja limpar os dados corrompidos? (s/n)"
if ($confirmar -ne "s" -and $confirmar -ne "S") {
    Write-Host "❌ Limpeza cancelada pelo usuário" -ForegroundColor Red
    exit 0
}

Write-Host ""
Write-Host "🗑️ Executando limpeza..." -ForegroundColor Cyan

# Limpar rotas vazias
Write-Host "   🗑️ Limpando rotas corrompidas..."
try {
    firebase firestore:delete "empresas/$EmpresaId/rotas" --recursive --project $ProjectId
    Write-Host "   ✅ Rotas limpas" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️ Erro ao limpar rotas: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Limpar mesas vazias
Write-Host "   🗑️ Limpando mesas corrompidas..."
try {
    firebase firestore:delete "empresas/$EmpresaId/mesas" --recursive --project $ProjectId
    Write-Host "   ✅ Mesas limpas" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️ Erro ao limpar mesas: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Limpar acertos vazios
Write-Host "   🗑️ Limpando acertos corrompidos..."
try {
    firebase firestore:delete "empresas/$EmpresaId/acertos" --recursive --project $ProjectId
    Write-Host "   ✅ Acertos limpos" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️ Erro ao limpar acertos: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Limpar colaboradores vazios
Write-Host "   🗑️ Limpando colaboradores corrompidos..."
try {
    firebase firestore:delete "empresas/$EmpresaId/colaboradores" --recursive --project $ProjectId
    Write-Host "   ✅ Colaboradores limpos" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️ Erro ao limpar colaboradores: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✅ LIMPEZA CONCLUÍDA!" -ForegroundColor Green
Write-Host "===================" -ForegroundColor Green
Write-Host ""
Write-Host "📋 PRÓXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "1. Faça o build do app com as correções" -ForegroundColor White
Write-Host "2. Instale o APK no dispositivo" -ForegroundColor White
Write-Host "3. Crie novos dados (cliente, mesa, acertos)" -ForegroundColor White
Write-Host "4. Sincronize e verifique no Firestore" -ForegroundColor White
Write-Host ""
Write-Host "🎯 Agora os dados serão salvos corretamente!" -ForegroundColor Green
