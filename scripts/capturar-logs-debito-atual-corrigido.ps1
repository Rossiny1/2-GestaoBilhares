# Script para capturar logs específicos do débito atual CORRIGIDO
# Autor: Assistente Android Senior
# Data: 2025-01-06
# OBJETIVO: Verificar se o débito atual está sendo calculado em tempo real

Write-Host "=== CAPTURADOR DE LOGS - DÉBITO ATUAL CORRIGIDO ===" -ForegroundColor Cyan
Write-Host "Iniciando captura de logs específicos do débito atual em tempo real..." -ForegroundColor Yellow

# Parar captura anterior se estiver rodando
$processos = Get-Process -Name "adb" -ErrorAction SilentlyContinue
if ($processos) {
    Write-Host "Parando processos ADB anteriores..." -ForegroundColor Yellow
    Stop-Process -Name "adb" -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

# Iniciar ADB
Write-Host "Iniciando ADB..." -ForegroundColor Green
adb start-server

# Verificar se há dispositivos conectados
$devices = adb devices
if ($devices -notmatch "device$") {
    Write-Host "ERRO: Nenhum dispositivo Android conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo e tente novamente." -ForegroundColor Red
    exit 1
}

Write-Host "Dispositivo conectado. Iniciando captura de logs..." -ForegroundColor Green

# Criar arquivo de log com timestamp
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = "logcat-debito-atual-corrigido-$timestamp.txt"

Write-Host "Arquivo de log: $logFile" -ForegroundColor Cyan

# Comando para capturar logs específicos do débito atual
$logcatCommand = @"
adb logcat -c
adb logcat | Select-String -Pattern "Débito atual|DÉBITO ATUAL|updateCalculations|SettlementFragment" | Tee-Object -FilePath "$logFile"
"@

Write-Host "=== INSTRUÇÕES PARA TESTE ===" -ForegroundColor Yellow
Write-Host "1. Instale o APK gerado" -ForegroundColor White
Write-Host "2. Faça login no app" -ForegroundColor White
Write-Host "3. Vá para uma rota e selecione um cliente" -ForegroundColor White
Write-Host "4. Clique em 'Novo Acerto'" -ForegroundColor White
Write-Host "5. Observe o campo 'Débito Atual' - deve mostrar o mesmo valor do 'Débito Anterior' inicialmente" -ForegroundColor White
Write-Host "6. Digite valores nos relógios das mesas - o 'Débito Atual' deve aumentar" -ForegroundColor White
Write-Host "7. Digite um desconto - o 'Débito Atual' deve diminuir" -ForegroundColor White
Write-Host "8. Selecione métodos de pagamento e digite valores - o 'Débito Atual' deve diminuir" -ForegroundColor White
Write-Host "9. Pressione Ctrl+C para parar a captura" -ForegroundColor White

Write-Host "`nIniciando captura de logs..." -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow

# Executar comando de captura
try {
    Invoke-Expression $logcatCommand
} catch {
    Write-Host "Captura interrompida pelo usuário" -ForegroundColor Yellow
}

Write-Host "`n=== ANÁLISE DOS LOGS ===" -ForegroundColor Cyan
if (Test-Path $logFile) {
    $logContent = Get-Content $logFile
    $totalLines = $logContent.Count
    
    Write-Host "Total de linhas capturadas: $totalLines" -ForegroundColor White
    
    # Análise específica
    $debitoAtualLines = $logContent | Select-String "DÉBITO ATUAL CALCULADO EM TEMPO REAL"
    $updateCalculationsLines = $logContent | Select-String "INICIANDO CÁLCULOS"
    $formulaLines = $logContent | Select-String "FÓRMULA:"
    
    Write-Host "`n=== RESULTADOS ===" -ForegroundColor Yellow
    Write-Host "Cálculos de débito atual: $($debitoAtualLines.Count)" -ForegroundColor White
    Write-Host "Funções updateCalculations chamadas: $($updateCalculationsLines.Count)" -ForegroundColor White
    Write-Host "Fórmulas registradas: $($formulaLines.Count)" -ForegroundColor White
    
    if ($debitoAtualLines.Count -gt 0) {
        Write-Host "`n✅ SUCESSO: Débito atual está sendo calculado em tempo real!" -ForegroundColor Green
        Write-Host "Último cálculo: $($debitoAtualLines[-1])" -ForegroundColor White
    } else {
        Write-Host "`n❌ PROBLEMA: Débito atual não está sendo calculado!" -ForegroundColor Red
    }
    
    if ($formulaLines.Count -gt 0) {
        Write-Host "`nÚltima fórmula calculada: $($formulaLines[-1])" -ForegroundColor Cyan
    }
    
    Write-Host "`nLogs salvos em: $logFile" -ForegroundColor Green
} else {
    Write-Host "❌ ERRO: Arquivo de log não foi criado!" -ForegroundColor Red
}

Write-Host "`n=== FIM DA CAPTURA ===" -ForegroundColor Cyan 