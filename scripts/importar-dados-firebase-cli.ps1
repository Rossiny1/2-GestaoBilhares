# Script para importar dados CSV para Firestore usando Firebase CLI
# Baseado no script de deploy de índices, mas adaptado para importação de dados

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Importação de Dados para Firestore" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Recarregar PATH completo (incluindo Node.js)
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
$env:Path += ";$env:APPDATA\npm"
$firebaseCmd = "$env:APPDATA\npm\firebase.cmd"

# Verificar se Firebase CLI está instalado
Write-Host "[1/5] Verificando Firebase CLI..." -ForegroundColor Yellow

if (-not (Test-Path $firebaseCmd)) {
    # Tentar encontrar no PATH
    $firebaseInstalled = Get-Command firebase -ErrorAction SilentlyContinue
    if ($firebaseInstalled) {
        $firebaseCmd = "firebase"
    }
    else {
        Write-Host "[ERRO] Firebase CLI não encontrado!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Para instalar, execute:" -ForegroundColor Yellow
        Write-Host "  npm install -g firebase-tools" -ForegroundColor White
        exit 1
    }
}

try {
    $version = & $firebaseCmd --version 2>&1
    Write-Host "[OK] Firebase CLI encontrado: $version" -ForegroundColor Green
}
catch {
    Write-Host "[ERRO] Firebase CLI não está funcionando" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Verificar se está logado
Write-Host "[2/5] Verificando login no Firebase..." -ForegroundColor Yellow
$firebaseUser = & $firebaseCmd login:list 2>&1 | Select-String "Logged in as"

if (-not $firebaseUser) {
    Write-Host "[AVISO] Não está logado no Firebase. Fazendo login..." -ForegroundColor Yellow
    & $firebaseCmd login --no-localhost
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERRO] Falha no login do Firebase" -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "[OK] Logado no Firebase" -ForegroundColor Green
    Write-Host "  $firebaseUser" -ForegroundColor Gray
}
Write-Host ""

# Verificar se firebase.json existe
Write-Host "[3/5] Verificando configuração do Firebase..." -ForegroundColor Yellow
if (-not (Test-Path "firebase.json")) {
    Write-Host "[AVISO] firebase.json não encontrado. Inicializando Firestore..." -ForegroundColor Yellow
    & $firebaseCmd init firestore
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERRO] Falha na inicialização do Firestore" -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "[OK] firebase.json encontrado" -ForegroundColor Green
}
Write-Host ""

# Verificar se arquivo CSV existe
Write-Host "[4/5] Verificando arquivo CSV..." -ForegroundColor Yellow
$csvPath = "anexos\Cadastro Clientes- Rota Bahia.csv"
if (-not (Test-Path $csvPath)) {
    Write-Host "[ERRO] Arquivo CSV não encontrado: $csvPath" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Arquivo CSV encontrado: $csvPath" -ForegroundColor Green

# Ler e processar CSV
Write-Host "[5/5] Processando e importando dados..." -ForegroundColor Yellow
Write-Host ""

# Importar módulos necessários
try {
    Add-Type -AssemblyName System.Text.Encoding
}
catch {
    Write-Host "[ERRO] Falha ao carregar assemblies .NET" -ForegroundColor Red
    exit 1
}

# Ler CSV com encoding correto (Windows-1252)
try {
    $content = Get-Content -Path $csvPath -Encoding UTF8
    Write-Host "[OK] CSV lido com sucesso" -ForegroundColor Green
}
catch {
    Write-Host "[AVISO] Tentando encoding Windows-1252..." -ForegroundColor Yellow
    try {
        $bytes = [System.IO.File]::ReadAllBytes($csvPath)
        $encoding = [System.Text.Encoding]::GetEncoding("Windows-1252")
        $content = $encoding.GetString($bytes)
        Write-Host "[OK] CSV lido com encoding Windows-1252" -ForegroundColor Green
    }
    catch {
        Write-Host "[ERRO] Falha ao ler arquivo CSV" -ForegroundColor Red
        exit 1
    }
}

# Processar linhas do CSV
$lines = $content -split "`n" | Where-Object { $_.Trim() -ne "" }
$totalLines = $lines.Count
$importados = 0
$erros = 0

Write-Host "Processando $totalLines registros..." -ForegroundColor Cyan

for ($i = 0; $i -lt $totalLines; $i++) {
    $line = $lines[$i].Trim()
    if ($line -eq "") { continue }
    
    $parts = $line -split ';'
    
    if ($parts.Length -lt 12) {
        Write-Host "[AVISO] Linha $($i+1) formato inválido, pulando..." -ForegroundColor Yellow
        $erros++
        continue
    }
    
    # Extrair dados
    $id = $parts[0].Replace('"', '').Trim()
    $nome = $parts[1].Replace('"', '').Trim()
    $cpf = $parts[2].Replace('"', '').Trim()
    $endereco = $parts[3].Replace('"', '').Trim()
    $cidade = $parts[4].Replace('"', '').Trim()
    $estado = $parts[5].Replace('"', '').Trim()
    $telefone1 = $parts[6].Replace('"', '').Trim()
    $telefone2 = $parts[7].Replace('"', '').Trim()
    $dataCadastro = $parts[9].Replace('"', '').Trim()
    $valorStr = $parts[11].Replace('"', '').Trim()
    $observacoes = $parts[12].Replace('"', '').Trim()
    
    # Converter valor monetário
    $valor = 0
    if ($valorStr -and $valorStr -ne "") {
        $valorClean = $valorStr.Replace('R$', '').Replace('.', '').Replace(',', '.').Trim()
        try {
            $valor = [double]$valorClean
        }
        catch {
            $valor = 0
        }
    }
    
    # Criar documento cliente
    $cliente = @{
        id                = [int]$id
        nome              = $nome
        cpf               = $cpf
        endereco          = $endereco
        cidade            = $cidade
        estado            = $estado
        telefone1         = $telefone1
        telefone2         = $telefone2
        dataCadastro      = if ($dataCadastro) { $dataCadastro } else { (Get-Date).ToString("yyyy-MM-dd") }
        valorUltimoAcerto = $valor
        observacoes       = $observacoes
        ativo             = $true
        rota_id           = 1
        createdAt         = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        updatedAt         = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    }
    
    # Gerar JSON temporário
    $tempFile = "temp_cliente_$i.json"
    $clienteJson = $cliente | ConvertTo-Json -Depth 10 -Compress
    $clienteJson | Out-File -FilePath $tempFile -Encoding UTF8
    
    try {
        # Importar via Firebase CLI usando REST API (alternativa)
        $token = & $firebaseCmd login:ci 2>&1
        if ($token -match "Token: (.+)") {
            $authToken = $matches[1].Trim()
            
            $headers = @{
                "Authorization" = "Bearer $authToken"
                "Content-Type"  = "application/json"
            }
            
            $body = $cliente | ConvertTo-Json -Depth 10 -Compress
            $url = "https://firestore.googleapis.com/v1/projects/gestaobilhares/databases/(default)/documents/clientes/$id"
            
            try {
                $response = Invoke-RestMethod -Uri $url -Method Patch -Headers $headers -Body $body -ErrorAction Stop
                $importados++
                Write-Host "[$($i+1)/$totalLines] ✅ Importado: $nome" -ForegroundColor Green
            }
            catch {
                $erros++
                Write-Host "[$($i+1)/$totalLines] ❌ Erro ao importar: $nome ($($_.Exception.Message))" -ForegroundColor Red
            }
        }
        else {
            $erros++
            Write-Host "[$($i+1)/$totalLines] ❌ Falha ao obter token de autenticação" -ForegroundColor Red
        }
    }
    catch {
        $erros++
        Write-Host "[$($i+1)/$totalLines] ❌ Exceção ao importar: $nome" -ForegroundColor Red
    }
    finally {
        # Remover arquivo temporário
        if (Test-Path $tempFile) {
            Remove-Item $tempFile -Force
        }
    }
    
    # Pequena pausa para não sobrecarregar
    Start-Sleep -Milliseconds 200
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Importação Concluída!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Resumo:" -ForegroundColor White
Write-Host "   ✅ Importados: $importados" -ForegroundColor Green
Write-Host "   ❌ Erros: $erros" -ForegroundColor Red
Write-Host "   📊 Total: $totalLines" -ForegroundColor White
Write-Host "   📈 Taxa de sucesso: $([math]::Round(($importados/$totalLines)*100, 1))%" -ForegroundColor Cyan
Write-Host ""
Write-Host "🎯 Próximos passos:" -ForegroundColor White
Write-Host "1. Abra o app Android" -ForegroundColor Gray
Write-Host "2. Vá em 'Rotas' para verificar os clientes" -ForegroundColor Gray
Write-Host "3. Clique em sincronizar para baixar os dados" -ForegroundColor Gray
Write-Host ""
Write-Host "🔗 Verificar no Firebase Console:" -ForegroundColor Cyan
Write-Host "  https://console.firebase.google.com/project/gestaobilhares/firestore/data/clientes" -ForegroundColor White
