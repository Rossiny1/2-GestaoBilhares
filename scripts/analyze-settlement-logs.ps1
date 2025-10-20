# ========================================
# SCRIPT DE ANÁLISE DE LOGS DO HISTÓRICO DE ACERTOS - GESTAO BILHARES
# Analisa logs capturados para identificar problemas
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANÁLISE DE LOGS DO HISTÓRICO DE ACERTOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se há arquivos de log
$logDir = "logs"
if (-not (Test-Path $logDir)) {
    Write-Host "ERRO: Diretório de logs não encontrado: $logDir" -ForegroundColor Red
    Write-Host "Execute primeiro o script capture-settlement-debug.ps1" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

# Listar arquivos de log disponíveis
$logFiles = Get-ChildItem -Path $logDir -Filter "settlement-history-debug_*.log" | Sort-Object LastWriteTime -Descending

if ($logFiles.Count -eq 0) {
    Write-Host "ERRO: Nenhum arquivo de log encontrado" -ForegroundColor Red
    Write-Host "Execute primeiro o script capture-settlement-debug.ps1" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Arquivos de log encontrados:" -ForegroundColor Yellow
for ($i = 0; $i -lt $logFiles.Count; $i++) {
    $file = $logFiles[$i]
    Write-Host "$($i + 1). $($file.Name) ($($file.LastWriteTime))" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Selecione o arquivo para analisar (1-$($logFiles.Count)):" -ForegroundColor Yellow
$selection = Read-Host "Digite o número"

if (-not ($selection -match '^\d+$') -or [int]$selection -lt 1 -or [int]$selection -gt $logFiles.Count) {
    Write-Host "ERRO: Seleção inválida" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

$selectedFile = $logFiles[[int]$selection - 1]
Write-Host "Analisando arquivo: $($selectedFile.Name)" -ForegroundColor Green
Write-Host ""

# Ler o arquivo de log
$logContent = Get-Content $selectedFile.FullName

if ($logContent.Count -eq 0) {
    Write-Host "ERRO: Arquivo de log está vazio" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Total de linhas no log: $($logContent.Count)" -ForegroundColor Yellow
Write-Host ""

# Análise dos logs
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANÁLISE DOS LOGS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Verificar inicialização do adapter
Write-Host "1. VERIFICAÇÃO DA INICIALIZAÇÃO DO ADAPTER:" -ForegroundColor Yellow
$adapterInit = $logContent | Select-String "RecyclerViews configurados"
if ($adapterInit) {
    Write-Host "✅ Adapter inicializado corretamente" -ForegroundColor Green
    $adapterInit | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
} else {
    Write-Host "❌ Adapter NÃO foi inicializado" -ForegroundColor Red
}
Write-Host ""

# 2. Verificar carregamento do histórico
Write-Host "2. VERIFICAÇÃO DO CARREGAMENTO DO HISTÓRICO:" -ForegroundColor Yellow
$historyLoad = $logContent | Select-String "Carregando histórico de acertos"
if ($historyLoad) {
    Write-Host "✅ Carregamento do histórico iniciado" -ForegroundColor Green
    $historyLoad | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
} else {
    Write-Host "❌ Carregamento do histórico NÃO foi iniciado" -ForegroundColor Red
}
Write-Host ""

# 3. Verificar acertos encontrados no banco
Write-Host "3. VERIFICAÇÃO DE ACERTOS NO BANCO:" -ForegroundColor Yellow
$acertosEncontrados = $logContent | Select-String "Acertos encontrados no banco"
if ($acertosEncontrados) {
    Write-Host "✅ Acertos encontrados no banco" -ForegroundColor Green
    $acertosEncontrados | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
} else {
    Write-Host "❌ Nenhum acerto encontrado no banco" -ForegroundColor Red
}
Write-Host ""

# 4. Verificar recebimento do histórico no fragment
Write-Host "4. VERIFICAÇÃO DO RECEBIMENTO NO FRAGMENT:" -ForegroundColor Yellow
$historicoRecebido = $logContent | Select-String "HISTÓRICO RECEBIDO"
if ($historicoRecebido) {
    Write-Host "✅ Histórico recebido no fragment" -ForegroundColor Green
    $historicoRecebido | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
} else {
    Write-Host "❌ Histórico NÃO foi recebido no fragment" -ForegroundColor Red
}
Write-Host ""

# 5. Verificar envio para o adapter
Write-Host "5. VERIFICAÇÃO DO ENVIO PARA O ADAPTER:" -ForegroundColor Yellow
$listaEnviada = $logContent | Select-String "Lista enviada para o adapter"
if ($listaEnviada) {
    Write-Host "✅ Lista enviada para o adapter" -ForegroundColor Green
    $listaEnviada | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
} else {
    Write-Host "❌ Lista NÃO foi enviada para o adapter" -ForegroundColor Red
}
Write-Host ""

# 6. Verificar erros
Write-Host "6. VERIFICAÇÃO DE ERROS:" -ForegroundColor Yellow
$erros = $logContent | Select-String "ERRO|ERROR|Exception|Adapter não inicializado"
if ($erros) {
    Write-Host "❌ Erros encontrados:" -ForegroundColor Red
    $erros | ForEach-Object { Write-Host "   $_" -ForegroundColor Red }
} else {
    Write-Host "✅ Nenhum erro encontrado" -ForegroundColor Green
}
Write-Host ""

# 7. Resumo da análise
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA ANÁLISE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$problemas = @()
if (-not $adapterInit) { $problemas += "Adapter não inicializado" }
if (-not $historyLoad) { $problemas += "Carregamento do histórico não iniciado" }
if (-not $acertosEncontrados) { $problemas += "Nenhum acerto encontrado no banco" }
if (-not $historicoRecebido) { $problemas += "Histórico não recebido no fragment" }
if (-not $listaEnviada) { $problemas += "Lista não enviada para o adapter" }
if ($erros) { $problemas += "Erros encontrados nos logs" }

if ($problemas.Count -eq 0) {
    Write-Host "✅ TODOS OS COMPONENTES FUNCIONANDO CORRETAMENTE" -ForegroundColor Green
    Write-Host "O problema pode estar na UI ou no layout" -ForegroundColor Yellow
} else {
    Write-Host "❌ PROBLEMAS IDENTIFICADOS:" -ForegroundColor Red
    $problemas | ForEach-Object { Write-Host "   - $_" -ForegroundColor Red }
}

Write-Host ""
Read-Host "Pressione Enter para sair"
