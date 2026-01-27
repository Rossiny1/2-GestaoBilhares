# Script simples para testar importação de dados
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Teste de Importação para Firestore" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Verificar JSON
$jsonPath = "import-data\clientes_bahia_import.json"
if (-not (Test-Path $jsonPath)) {
    Write-Host "[ERRO] JSON não encontrado" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] JSON encontrado" -ForegroundColor Green

# Ler JSON
try {
    $clientesJson = Get-Content -Path $jsonPath -Raw | ConvertFrom-Json
    $totalClientes = $clientesJson.Count
    Write-Host "[OK] $totalClientes clientes carregados" -ForegroundColor Green
}
catch {
    Write-Host "[ERRO] Falha ao ler JSON" -ForegroundColor Red
    exit 1
}

# Testar importação de 1 cliente apenas
Write-Host ""
Write-Host "Testando importação de 1 cliente..." -ForegroundColor Yellow

$clienteTeste = $clientesJson[0]
$id = $clienteTeste.id
$nome = $clienteTeste.nome

Write-Host "Cliente teste: $nome (ID: $id)" -ForegroundColor Cyan

# Tentar importar via REST API
$url = "https://firestore.googleapis.com/v1/projects/gestaobilhares/databases/(default)/documents/clientes/$id"
$body = @{
    fields = @{
        id                = @{ integerValue = $clienteTeste.id }
        nome              = @{ stringValue = $clienteTeste.nome }
        cpf               = @{ stringValue = $clienteTeste.cpf }
        endereco          = @{ stringValue = $clienteTeste.endereco }
        cidade            = @{ stringValue = $clienteTeste.cidade }
        estado            = @{ stringValue = $clienteTeste.estado }
        telefone1         = @{ stringValue = $clienteTeste.telefone1 }
        telefone2         = @{ stringValue = $clienteTeste.telefone2 }
        dataCadastro      = @{ stringValue = $clienteTeste.dataCadastro }
        valorUltimoAcerto = @{ doubleValue = $clienteTeste.valorUltimoAcerto }
        observacoes       = @{ stringValue = $clienteTeste.observacoes }
        ativo             = @{ booleanValue = $clienteTeste.ativo }
        rota_id           = @{ integerValue = $clienteTeste.rota_id }
        createdAt         = @{ timestampValue = $clienteTeste.createdAt }
        updatedAt         = @{ timestampValue = $clienteTeste.updatedAt }
    }
} | ConvertTo-Json -Depth 10 -Compress

$headers = @{
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-WebRequest -Uri $url -Method PATCH -Headers $headers -Body $body -ErrorAction Stop
    Write-Host "✅ Cliente importado com sucesso!" -ForegroundColor Green
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Gray
}
catch {
    Write-Host "❌ Erro ao importar: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "🔧 Análise do erro:" -ForegroundColor Yellow
    if ($_.Exception.Message -match "401|403|UNAUTHENTICATED") {
        Write-Host "   Causa: Falha de autenticação" -ForegroundColor Gray
        Write-Host "   Solução: Precisa de service account válida" -ForegroundColor Gray
    }
    elseif ($_.Exception.Message -match "404|NOT_FOUND") {
        Write-Host "   Causa: Projeto não encontrado" -ForegroundColor Gray
        Write-Host "   Solução: Verificar project ID 'gestaobilhares'" -ForegroundColor Gray
    }
    else {
        Write-Host "   Causa: Erro de conexão ou permissão" -ForegroundColor Gray
        Write-Host "   Solução: Verificar rede e configurações" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Teste Concluído!" -ForegroundColor Cyan
Write-Host "========================================"
