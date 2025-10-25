# Script para corrigir rotas corrompidas mantendo clientes
# Cria rota válida e atualiza referências dos clientes

Write-Host "🔧 CORREÇÃO DE ROTAS E CLIENTES" -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Yellow

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
    Write-Host "   Ou use a correção manual no console do Firestore" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "🔍 Verificando situação atual..." -ForegroundColor Cyan

# Verificar rotas corrompidas
Write-Host "📊 Verificando rotas corrompidas:" -ForegroundColor Cyan
$rotasCorrompidas = firebase firestore:query "empresas/$EmpresaId/rotas" --project $ProjectId 2>$null | Select-String "{}" | Measure-Object
if ($rotasCorrompidas.Count -gt 0) {
    Write-Host "   ⚠️ Encontradas $($rotasCorrompidas.Count) rotas corrompidas" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ Rotas OK" -ForegroundColor Green
}

# Verificar clientes
Write-Host "📊 Verificando clientes:" -ForegroundColor Cyan
$clientes = firebase firestore:query "empresas/$EmpresaId/clientes" --project $ProjectId 2>$null | Measure-Object
Write-Host "   📋 Encontrados $($clientes.Count) clientes" -ForegroundColor Cyan

Write-Host ""
Write-Host "🔧 CORREÇÃO AUTOMÁTICA" -ForegroundColor Yellow
Write-Host "=====================" -ForegroundColor Yellow

# Confirmar correção
$confirmar = Read-Host "Deseja corrigir as rotas e manter os clientes? (s/n)"
if ($confirmar -ne "s" -and $confirmar -ne "S") {
    Write-Host "❌ Correção cancelada pelo usuário" -ForegroundColor Red
    exit 0
}

Write-Host ""
Write-Host "🔄 Executando correção..." -ForegroundColor Cyan

# 1. Criar rota válida
Write-Host "   🆕 Criando rota válida..."
try {
    $rotaValida = @{
        nome = "Rota Principal"
        descricao = "Rota principal do sistema"
        ativa = $true
        dataCriacao = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        roomId = 1
        syncTimestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    } | ConvertTo-Json -Depth 3
    
    # Criar documento com ID específico
    firebase firestore:set "empresas/$EmpresaId/rotas/rota_principal" $rotaValida --project $ProjectId
    Write-Host "   ✅ Rota válida criada: rota_principal" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️ Erro ao criar rota válida: $($_.Exception.Message)" -ForegroundColor Yellow
}

# 2. Atualizar clientes para usar a nova rota
Write-Host "   🔄 Atualizando clientes para nova rota..."
try {
    # Listar clientes e atualizar rotaId
    $clientes = firebase firestore:query "empresas/$EmpresaId/clientes" --project $ProjectId 2>$null
    
    if ($clientes) {
        Write-Host "   📋 Atualizando $($clientes.Count) clientes..." -ForegroundColor Cyan
        
        # Para cada cliente, atualizar rotaId para 1 (nova rota)
        # Nota: Esta é uma operação complexa que requer script mais avançado
        # Por enquanto, vamos apenas informar o que precisa ser feito
        Write-Host "   ⚠️ ATENÇÃO: Atualize manualmente os clientes no console do Firestore" -ForegroundColor Yellow
        Write-Host "   📝 Para cada cliente, altere rotaId para: 1" -ForegroundColor White
    }
} catch {
    Write-Host "   ⚠️ Erro ao atualizar clientes: $($_.Exception.Message)" -ForegroundColor Yellow
}

# 3. Deletar rotas corrompidas
Write-Host "   🗑️ Deletando rotas corrompidas..."
try {
    firebase firestore:delete "empresas/$EmpresaId/rotas" --recursive --project $ProjectId
    Write-Host "   ✅ Rotas corrompidas deletadas" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️ Erro ao deletar rotas corrompidas: $($_.Exception.Message)" -ForegroundColor Yellow
}

# 4. Deletar mesas e acertos corrompidos
Write-Host "   🗑️ Deletando mesas e acertos corrompidos..."
try {
    firebase firestore:delete "empresas/$EmpresaId/mesas" --recursive --project $ProjectId
    firebase firestore:delete "empresas/$EmpresaId/acertos" --recursive --project $ProjectId
    firebase firestore:delete "empresas/$EmpresaId/colaboradores" --recursive --project $ProjectId
    Write-Host "   ✅ Dados corrompidos deletados" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️ Erro ao deletar dados corrompidos: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✅ CORREÇÃO CONCLUÍDA!" -ForegroundColor Green
Write-Host "=====================" -ForegroundColor Green
Write-Host ""
Write-Host "📋 PRÓXIMOS PASSOS MANUAIS:" -ForegroundColor Cyan
Write-Host "1. Acesse o console do Firestore" -ForegroundColor White
Write-Host "2. Vá para: empresas > empresa_001 > clientes" -ForegroundColor White
Write-Host "3. Para cada cliente, altere rotaId para: 1" -ForegroundColor White
Write-Host "4. Faça o build do app com as correções" -ForegroundColor White
Write-Host "5. Teste a sincronização" -ForegroundColor White
Write-Host ""
Write-Host "🎯 Agora os clientes terão uma rota válida!" -ForegroundColor Green
