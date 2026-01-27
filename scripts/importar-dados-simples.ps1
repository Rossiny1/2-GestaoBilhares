# Script simples para importar dados CSV para Firestore
# Usa o JSON que já geramos e Firebase CLI com login existente

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Importação de Dados para Firestore" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se arquivo JSON existe
Write-Host "[1/3] Verificando arquivos..." -ForegroundColor Yellow

$jsonPath = "import-data\clientes_bahia_import.json"
if (-not (Test-Path $jsonPath)) {
    Write-Host "[ERRO] Arquivo JSON não encontrado: $jsonPath" -ForegroundColor Red
    Write-Host "Execute primeiro: cd import-data && node importar_json_local.js" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] Arquivo JSON encontrado: $jsonPath" -ForegroundColor Green

# Verificar Firebase CLI
$firebaseCmd = Get-Command firebase -ErrorAction SilentlyContinue
if (-not $firebaseCmd) {
    Write-Host "[ERRO] Firebase CLI não encontrado!" -ForegroundColor Red
    Write-Host "Instale com: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] Firebase CLI encontrado" -ForegroundColor Green

# Verificar login
$firebaseUser = & firebase login:list 2>&1 | Select-String "Logged in as"
if (-not $firebaseUser) {
    Write-Host "[ERRO] Não está logado no Firebase!" -ForegroundColor Red
    Write-Host "Execute: firebase login" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] Logado no Firebase: $firebaseUser" -ForegroundColor Green

Write-Host ""

# Ler JSON
Write-Host "[2/3] Carregando dados..." -ForegroundColor Yellow
try {
    $clientesJson = Get-Content -Path $jsonPath -Raw | ConvertFrom-Json
    $totalClientes = $clientesJson.Count
    Write-Host "[OK] Carregados $totalClientes clientes" -ForegroundColor Green
}
catch {
    Write-Host "[ERRO] Falha ao ler JSON: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Importar dados
Write-Host "[3/3] Importando para Firestore..." -ForegroundColor Yellow
Write-Host ""

$importados = 0
$erros = 0

foreach ($cliente in $clientesJson) {
    $id = $cliente.id
    $nome = $cliente.nome
    
    try {
        # Criar arquivo temporário para este cliente
        $tempFile = "temp_cliente_$id.json"
        $cliente | ConvertTo-Json -Depth 10 -Compress | Out-File -FilePath $tempFile -Encoding UTF8
        
        # Tentar importar usando REST API direta
        $url = "https://firestore.googleapis.com/v1/projects/gestaobilhares/databases/(default)/documents/clientes/$id"
        $body = @{
            fields = @{
                id                = @{ integerValue = $cliente.id }
                nome              = @{ stringValue = $cliente.nome }
                cpf               = @{ stringValue = $cliente.cpf }
                endereco          = @{ stringValue = $cliente.endereco }
                cidade            = @{ stringValue = $cliente.cidade }
                estado            = @{ stringValue = $cliente.estado }
                telefone1         = @{ stringValue = $cliente.telefone1 }
                telefone2         = @{ stringValue = $cliente.telefone2 }
                dataCadastro      = @{ stringValue = $cliente.dataCadastro }
                valorUltimoAcerto = @{ doubleValue = $cliente.valorUltimoAcerto }
                observacoes       = @{ stringValue = $cliente.observacoes }
                ativo             = @{ booleanValue = $cliente.ativo }
                rota_id           = @{ integerValue = $cliente.rota_id }
                createdAt         = @{ timestampValue = $cliente.createdAt }
                updatedAt         = @{ timestampValue = $cliente.updatedAt }
            }
        } | ConvertTo-Json -Depth 10 -Compress
        
        $headers = @{
            "Content-Type" = "application/json"
        }
        
        try {
            # Tentar criar documento (pode falhar sem autenticação)
            Invoke-WebRequest -Uri $url -Method PATCH -Headers $headers -Body $body -ErrorAction Stop | Out-Null
            $importados++
            Write-Host "[$($importados+$erros)/$totalClientes] ✅ Importado: $nome" -ForegroundColor Green
        }
        catch {
            # Se falhar, mostrar erro mas continuar
            $erros++
            Write-Host "[$($importados+$erros)/$totalClientes] ❌ Erro ao importar: $nome (Autenticação necessária)" -ForegroundColor Yellow
        }
        
        # Remover arquivo temporário
        if (Test-Path $tempFile) {
            Remove-Item $tempFile -Force
        }
        
    }
    catch {
        $erros++
        Write-Host "[$($importados+$erros)/$totalClientes] ❌ Exceção: $nome" -ForegroundColor Red
    }
    
    # Pequena pausa
    Start-Sleep -Milliseconds 100
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Importação Concluída!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Resumo:" -ForegroundColor White
Write-Host "   ✅ Importados: $importados" -ForegroundColor Green
Write-Host "   ❌ Erros: $erros" -ForegroundColor Yellow
Write-Host "   📊 Total: $totalClientes" -ForegroundColor White
Write-Host ""

if ($importados -gt 0) {
    Write-Host "🎉 Dados importados com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🎯 Próximos passos:" -ForegroundColor White
    Write-Host "1. Abra o app Android" -ForegroundColor Gray
    Write-Host "2. Vá em 'Rotas' para verificar os clientes" -ForegroundColor Gray
    Write-Host "3. Clique em sincronizar para baixar os dados" -ForegroundColor Gray
}
else {
    Write-Host "❌ Nenhum dado importado. Autenticação necessária." -ForegroundColor Red
    Write-Host ""
    Write-Host "🔧 Soluções:" -ForegroundColor Yellow
    Write-Host "1. Gerar nova service account no Firebase Console" -ForegroundColor Gray
    Write-Host "2. Usar script Node.js com service account" -ForegroundColor Gray
    Write-Host "3. Importar manualmente no Firebase Console" -ForegroundColor Gray
}

Write-Host ""
Write-Host "🔗 Firebase Console:" -ForegroundColor Cyan
Write-Host "  https://console.firebase.google.com/project/gestaobilhares/firestore/data/clientes" -ForegroundColor White
