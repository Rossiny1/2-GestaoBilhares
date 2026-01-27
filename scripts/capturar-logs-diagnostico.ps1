# Script para capturar logs de diagnóstico - Cards Acerto V14
# Objetivo: Capturar logs DEBUG_CARDS e erros para diagnóstico de cards de Acerto
# Baseado no script capturar-logs-sincronizacao.ps1

Write-Host "=== CAPTURA DE LOGS - DIAGNÓSTICO CARDS ACERTO V14 ===" -ForegroundColor Yellow
Write-Host "Objetivo: Capturar logs DEBUG_CARDS e erros para identificar problema dos cards de Acerto" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB (padrão Windows)
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (!(Test-Path $ADB)) {
    Write-Host "ADB não encontrado em: $ADB" -ForegroundColor Red
    Write-Host "Verificando caminho alternativo..." -ForegroundColor Yellow
    $ADB = "c:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe"
    if (!(Test-Path $ADB)) {
        Write-Host "ADB não encontrado em: $ADB" -ForegroundColor Red
        Write-Host "Certifique-se de que o Android SDK está instalado" -ForegroundColor Red
        exit 1
    }
}

# Verificar se há dispositivo conectado
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $ADB devices
if ($devices -match "device$") {
    Write-Host "Dispositivo encontrado!" -ForegroundColor Green
} else {
    Write-Host "Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo USB ou inicie um emulador" -ForegroundColor Yellow
    exit 1
}

# Limpar logs anteriores
Write-Host ""
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c

# Iniciar captura de logs
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CAPTURANDO LOGS DE DIAGNÓSTICO V14" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtros ativos:" -ForegroundColor Yellow
Write-Host "  - DEBUG_CARDS (todos os níveis)" -ForegroundColor White
Write-Host "  - Erros gerais (*:E)" -ForegroundColor White
Write-Host ""
Write-Host "Logs específicos capturados:" -ForegroundColor Cyan
Write-Host "  - RegistrarTrocaPanoUseCase (início, mesa lookup, criação, inserção)" -ForegroundColor White
Write-Host "  - SettlementViewModel.registrarTrocaPanoNoHistorico (chamada, parâmetros)" -ForegroundColor White
Write-Host "  - MesasReformadasViewModel.carregarMesasReformadas (dados, cards gerados)" -ForegroundColor White
Write-Host "  - Erros e exceções gerais" -ForegroundColor White
Write-Host ""
Write-Host "Aguardando eventos dos testes..." -ForegroundColor Gray
Write-Host "Execute: 1) Nova Reforma  2) Acerto" -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Gray
Write-Host ""

# Capturar logs com filtros específicos
& $ADB logcat -v time DEBUG_CARDS:* *:E | ForEach-Object {
    $line = $_
    
    # Cores para diferentes tipos de logs
    if ($line -match "DEBUG_CARDS.*USE CASE INICIADO") {
        Write-Host $line -ForegroundColor Cyan -BackgroundColor DarkBlue
    }
    elseif ($line -match "DEBUG_CARDS.*ACERTO.*Registrando Troca de Pano") {
        Write-Host $line -ForegroundColor Yellow -BackgroundColor DarkRed
    }
    elseif ($line -match "DEBUG_CARDS.*CARREGANDO CARDS") {
        Write-Host $line -ForegroundColor Green -BackgroundColor DarkCyan
    }
    elseif ($line -match "DEBUG_CARDS.*SUCESSO|DEBUG_CARDS.*CONCLUIDO") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "DEBUG_CARDS.*ERRO|DEBUG_CARDS.*ERROR|DEBUG_CARDS.*Exception") {
        Write-Host $line -ForegroundColor Red -BackgroundColor DarkYellow
    }
    elseif ($line -match "DEBUG_CARDS.*Reformas do ACERTO|DEBUG_CARDS.*Historicos do ACERTO") {
        Write-Host $line -ForegroundColor Magenta -BackgroundColor Black
    }
    elseif ($line -match "DEBUG_CARDS.*Mesa:|DEBUG_CARDS.*Cards gerados") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "DEBUG_CARDS.*Dados recebidos|DEBUG_CARDS.*MesasReformadas") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "DEBUG_CARDS.*Inserindo|DEBUG_CARDS.*inserida com ID") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "DEBUG_CARDS.*Chamando registrarTrocaPanoUseCase") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "DEBUG_CARDS.*Atualizando pano atual|DEBUG_CARDS.*Mesa atualizada") {
        Write-Host $line -ForegroundColor DarkGreen
    }
    # Logs de erro gerais
    elseif ($line -match "FATAL|CRITICAL|Exception:|ERROR:|ERRO:") {
        Write-Host $line -ForegroundColor Red -BackgroundColor White
    }
    elseif ($line -match ".*at .*\(.*\)|.*Caused by:|.*Suppressed:") {
        Write-Host $line -ForegroundColor DarkRed
    }
    else {
        Write-Host $line
    }
}

Write-Host ""
Write-Host "Captura de logs finalizada." -ForegroundColor Yellow
Write-Host "Salve os logs em um arquivo para análise:" -ForegroundColor Cyan
Write-Host "powershell -File capturar-logs-diagnostico.ps1 > logs_diagnostico.txt" -ForegroundColor White
