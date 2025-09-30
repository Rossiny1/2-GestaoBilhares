# ========================================
# MONITOR DETALHADO DO BANCO DE DADOS
# ========================================
#
# Script para monitorar em tempo real a criação de dados no banco
# Execute este script em paralelo com o app para identificar exatamente
# onde os dados estão sendo criados automaticamente
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MONITOR DETALHADO DO BANCO DE DADOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Este script ira monitorar em tempo real todas as operacoes" -ForegroundColor Yellow
Write-Host "de criacao de dados no banco de dados do app." -ForegroundColor Yellow
Write-Host ""
Write-Host "O que sera monitorado:" -ForegroundColor Green
Write-Host "   - Insercoes de rotas (INSERT INTO rotas)" -ForegroundColor Green
Write-Host "   - Insercoes de clientes (INSERT INTO clientes)" -ForegroundColor Green
Write-Host "   - Insercoes de ciclos (INSERT INTO ciclos_acerto)" -ForegroundColor Green
Write-Host "   - Insercoes de acertos (INSERT INTO acertos)" -ForegroundColor Green
Write-Host "   - Insercoes de mesas (INSERT INTO mesas)" -ForegroundColor Green
Write-Host ""
Write-Host "Como usar:" -ForegroundColor Cyan
Write-Host "   1. Execute este script em um terminal" -ForegroundColor Cyan
Write-Host "   2. Execute o app em outro terminal/celular" -ForegroundColor Cyan
Write-Host "   3. Observe os logs para identificar onde os dados sao criados" -ForegroundColor Cyan
Write-Host ""
Write-Host "Monitorando banco de dados..." -ForegroundColor Green

# Verificar se o ADB está disponível
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB nao encontrado em $adbPath" -ForegroundColor Red
    exit 1
}

# Verificar conexão com dispositivo
$devices = & $adbPath devices 2>$null | Where-Object { $_ -match "device$" }
if (-not $devices) {
    Write-Host "ERRO: Nenhum dispositivo conectado" -ForegroundColor Red
    exit 1
}

Write-Host "Dispositivo conectado!" -ForegroundColor Green

# Função para verificar dados atuais
function Verificar-Dados {
    Write-Host "VERIFICACAO ATUAL ($(Get-Date -Format 'HH:mm:ss'))" -ForegroundColor Yellow
    
    # Contar registros em cada tabela
    $rotas = & $adbPath shell "sqlite3 /data/data/com.example.gestaobilhares/databases/gestao_bilhares_database.db 'SELECT COUNT(*) FROM rotas;'"
    $clientes = & $adbPath shell "sqlite3 /data/data/com.example.gestaobilhares/databases/gestao_bilhares_database.db 'SELECT COUNT(*) FROM clientes;'"
    $ciclos = & $adbPath shell "sqlite3 /data/data/com.example.gestaobilhares/databases/gestao_bilhares_database.db 'SELECT COUNT(*) FROM ciclos_acerto;'"
    $acertos = & $adbPath shell "sqlite3 /data/data/com.example.gestaobilhares/databases/gestao_bilhares_database.db 'SELECT COUNT(*) FROM acertos;'"
    $mesas = & $adbPath shell "sqlite3 /data/data/com.example.gestaobilhares/databases/gestao_bilhares_database.db 'SELECT COUNT(*) FROM mesas;'"
    
    Write-Host "   ROTAS: $rotas" -ForegroundColor Green
    Write-Host "   CLIENTES: $clientes" -ForegroundColor Green
    Write-Host "   CICLOS: $ciclos" -ForegroundColor Green
    Write-Host "   ACERTOS: $acertos" -ForegroundColor Green
    Write-Host "   MESAS: $mesas" -ForegroundColor Green
    Write-Host ""
    
    # Listar rotas específicas (Centro, Zona Norte, Zona Sul)
    Write-Host "   ROTAS ESPECIFICAS:" -ForegroundColor Cyan
    $rotasEspecificas = & $adbPath shell "sqlite3 /data/data/com.example.gestaobilhares/databases/gestao_bilhares_database.db 'SELECT id, nome, ativo FROM rotas WHERE nome LIKE \"%Centro%\" OR nome LIKE \"%Norte%\" OR nome LIKE \"%Sul%\" ORDER BY nome;'"
    Write-Host $rotasEspecificas -ForegroundColor Gray
    Write-Host ""
}

Write-Host "Iniciando monitoramento..." -ForegroundColor Green
Write-Host "Para parar o monitoramento: Ctrl+C" -ForegroundColor Red
Write-Host ""

# Loop de monitoramento
try {
    while ($true) {
        Verificar-Dados
        Start-Sleep -Seconds 3
    }
} catch {
    Write-Host ""
    Write-Host "Monitoramento interrompido pelo usuario" -ForegroundColor Red
    Write-Host "Verifique os logs acima para analise completa" -ForegroundColor Cyan
}