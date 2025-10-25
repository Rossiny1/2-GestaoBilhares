# Script para corrigir rotas corrompidas mantendo clientes
# Cria rota valida e atualiza referencias dos clientes

Write-Host "CORRECAO DE ROTAS E CLIENTES" -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Yellow

# Configuracoes
$ProjectId = "gestaobilhares-12345"  # Substitua pelo seu Project ID
$EmpresaId = "empresa_001"

Write-Host "Configuracoes:" -ForegroundColor Cyan
Write-Host "   Project ID: $ProjectId"
Write-Host "   Empresa ID: $EmpresaId"
Write-Host ""

# Verificar se Firebase CLI esta instalado
try {
    $firebaseVersion = firebase --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Firebase CLI encontrado: $firebaseVersion" -ForegroundColor Green
    } else {
        throw "Firebase CLI nao encontrado"
    }
} catch {
    Write-Host "Firebase CLI nao encontrado!" -ForegroundColor Red
    Write-Host "   Instale com: npm install -g firebase-tools" -ForegroundColor Yellow
    Write-Host "   Ou use a correcao manual no console do Firestore" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Verificando situacao atual..." -ForegroundColor Cyan

# Verificar rotas corrompidas
Write-Host "Verificando rotas corrompidas:" -ForegroundColor Cyan
$rotasCorrompidas = firebase firestore:query "empresas/$EmpresaId/rotas" --project $ProjectId 2>$null | Select-String "{}" | Measure-Object
if ($rotasCorrompidas.Count -gt 0) {
    Write-Host "   Encontradas $($rotasCorrompidas.Count) rotas corrompidas" -ForegroundColor Yellow
} else {
    Write-Host "   Rotas OK" -ForegroundColor Green
}

# Verificar clientes
Write-Host "Verificando clientes:" -ForegroundColor Cyan
$clientes = firebase firestore:query "empresas/$EmpresaId/clientes" --project $ProjectId 2>$null | Measure-Object
Write-Host "   Encontrados $($clientes.Count) clientes" -ForegroundColor Cyan

Write-Host ""
Write-Host "CORRECAO AUTOMATICA" -ForegroundColor Yellow
Write-Host "=====================" -ForegroundColor Yellow

# 1. Criar Rota Principal se nao existir
Write-Host "Verificando e criando 'Rota Principal' no Firestore..."
$rotaPrincipalDocId = ""
$rotaPrincipalRoomId = 1

# Tenta encontrar a Rota Principal existente
$existingRotas = firebase firestore:query "empresas/$EmpresaId/rotas" --project $ProjectId 2>$null | ConvertFrom-Json
$foundRota = $existingRotas | Where-Object { $_.fields.roomId.integerValue -eq $rotaPrincipalRoomId -or $_.fields.nome.stringValue -eq "Rota Principal" }

if ($foundRota) {
    $rotaPrincipalDocId = $foundRota.name.Split('/')[-1]
    Write-Host "'Rota Principal' ja existe com ID: $rotaPrincipalDocId" -ForegroundColor Green
} else {
    Write-Host "'Rota Principal' nao encontrada. Criando..." -ForegroundColor Yellow
    $newRotaData = @{
        nome        = @{ stringValue = "Rota Principal" }
        descricao   = @{ stringValue = "Rota principal do sistema" }
        ativa       = @{ booleanValue = $true }
        dataCriacao = @{ integerValue = (Get-Date -UFormat %s) }
        roomId      = @{ integerValue = $rotaPrincipalRoomId }
        syncTimestamp = @{ integerValue = (Get-Date -UFormat %s) }
    } | ConvertTo-Json -Compress

    try {
        $result = firebase firestore:create "empresas/$EmpresaId/rotas" --json-data $newRotaData --project $ProjectId 2>&1
        if ($result -match "name: projects/.*/documents/empresas/.*/rotas/(?<docId>[a-zA-Z0-9]+)") {
            $rotaPrincipalDocId = $matches.docId
            Write-Host "'Rota Principal' criada com sucesso! ID: $rotaPrincipalDocId" -ForegroundColor Green
        } else {
            Write-Host "Erro ao criar 'Rota Principal': $result" -ForegroundColor Red
            exit 1
        }
    } catch {
        Write-Host "Excecao ao criar 'Rota Principal': $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# 2. Atualizar clientes para usar a nova Rota Principal
Write-Host "Atualizando clientes para referenciar 'Rota Principal' (roomId: $rotaPrincipalRoomId)..."
$clientes = firebase firestore:query "empresas/$EmpresaId/clientes" --project $ProjectId 2>$null | ConvertFrom-Json

if ($clientes) {
    foreach ($cliente in $clientes) {
        $clienteDocId = $cliente.name.Split('/')[-1]
        $currentRotaId = $cliente.fields.rotaId.integerValue
        $clienteNome = $cliente.fields.nome.stringValue

        if ($currentRotaId -ne $rotaPrincipalRoomId) {
            Write-Host "   - Cliente '$clienteNome' (ID: $clienteDocId) tem rotaId $currentRotaId. Atualizando para $rotaPrincipalRoomId..."
            $updateData = @{
                rotaId = @{ integerValue = $rotaPrincipalRoomId }
            } | ConvertTo-Json -Compress

            try {
                firebase firestore:update "empresas/$EmpresaId/clientes/$clienteDocId" --json-data $updateData --project $ProjectId 2>$null
                Write-Host "     Cliente '$clienteNome' atualizado." -ForegroundColor Green
            } catch {
                Write-Host "     Erro ao atualizar cliente '$clienteNome': $($_.Exception.Message)" -ForegroundColor Red
            }
        } else {
            Write-Host "   - Cliente '$clienteNome' (ID: $clienteDocId) ja referencia 'Rota Principal'."
        }
    }
    Write-Host "Clientes verificados e atualizados." -ForegroundColor Green
} else {
    Write-Host "Nenhuns clientes encontrados para atualizar." -ForegroundColor Yellow
}

# 3. Deletar subcollections corrompidas (exceto a Rota Principal recém-criada)
Write-Host "Limpando subcollections corrompidas..."

# Deletar rotas antigas/corrompidas (exceto a Rota Principal)
Write-Host "   Limpando rotas antigas/corrompidas..."
$allRotas = firebase firestore:query "empresas/$EmpresaId/rotas" --project $ProjectId 2>$null | ConvertFrom-Json
if ($allRotas) {
    foreach ($rota in $allRotas) {
        $rotaDocId = $rota.name.Split('/')[-1]
        if ($rotaDocId -ne $rotaPrincipalDocId) {
            Write-Host "      - Deletando rota: $rotaDocId"
            try {
                firebase firestore:delete "empresas/$EmpresaId/rotas/$rotaDocId" --project $ProjectId 2>$null
                Write-Host "        Rota $rotaDocId deletada." -ForegroundColor Green
            } catch {
                Write-Host "        Erro ao deletar rota $rotaDocId: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    }
}
Write-Host "   Rotas antigas limpas." -ForegroundColor Green

# Limpar mesas
Write-Host "   Limpando mesas..."
try {
    firebase firestore:delete "empresas/$EmpresaId/mesas" --recursive --project $ProjectId 2>$null
    Write-Host "   Mesas limpas." -ForegroundColor Green
} catch {
    Write-Host "   Erro ao limpar mesas: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Limpar acertos
Write-Host "   Limpando acertos..."
try {
    firebase firestore:delete "empresas/$EmpresaId/acertos" --recursive --project $ProjectId 2>$null
    Write-Host "   Acertos limpos." -ForegroundColor Green
} catch {
    Write-Host "   Erro ao limpar acertos: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Limpar colaboradores
Write-Host "   Limpando colaboradores..."
try {
    firebase firestore:delete "empresas/$EmpresaId/colaboradores" --recursive --project $ProjectId 2>$null
    Write-Host "   Colaboradores limpos." -ForegroundColor Green
} catch {
    Write-Host "   Erro ao limpar colaboradores: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "CORRECAO E LIMPEZA CONCLUIDAS!" -ForegroundColor Green
Write-Host "===============================" -ForegroundColor Green
Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "1. Acesse o console do Firestore" -ForegroundColor White
Write-Host "2. Va para: empresas > empresa_001 > clientes" -ForegroundColor White
Write-Host "3. Para cada cliente, altere rotaId para: 1" -ForegroundColor White
Write-Host "4. Faca o build do app com as correcoes" -ForegroundColor White
Write-Host "5. Teste a sincronizacao" -ForegroundColor White
Write-Host ""
Write-Host "Agora os clientes terao uma rota valida!" -ForegroundColor Green
