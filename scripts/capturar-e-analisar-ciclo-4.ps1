# Script avançado para capturar E analisar logs do CICLO 4 em tempo real
# Baseado no script que funciona, mas com análise automática

Write-Host "=== CAPTURA E ANALISE - CICLO 4 ===" -ForegroundColor Yellow
Write-Host "Objetivo: Capturar e analisar logs do ciclo 4 automaticamente" -ForegroundColor Cyan
Write-Host "Data/Hora: $(Get-Date)" -ForegroundColor Gray
Write-Host ""

# Caminho do ADB
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar ADB
if (!(Test-Path $ADB)) {
    Write-Host "ERRO: ADB nao encontrado em: $ADB" -ForegroundColor Red
    exit 1
}

# Verificar se ha dispositivo conectado
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $ADB devices
if ($devices -match "device$") {
    Write-Host "Dispositivo encontrado!" -ForegroundColor Green
} else {
    Write-Host "Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo USB ou inicie um emulador" -ForegroundColor Yellow
    exit 1
}

# Nome do arquivo de log
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile = "logs_ciclo_4_$timestamp.txt"

# Inicializar contadores
$ciclo4Encontrado = $false
$rotaAtualizada = $false
$sincronizacaoOk = $false
$errosEncontrados = 0

Write-Host ""
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c
Write-Host "SUCESSO: Logs limpos" -ForegroundColor Green

Write-Host ""
Write-Host "Iniciando captura com analise em tempo real..." -ForegroundColor Cyan
Write-Host "Arquivo: $logFile" -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Gray
Write-Host ""

# Capturar logs e analisar em tempo real
& $ADB logcat -v time -s SyncRepository:* RoutesViewModel:* RoutesFragment:* | ForEach-Object {
    $line = $_

    # Salvar no arquivo
    $line | Out-File -FilePath $logFile -Append -Encoding UTF8

    # ========== ANÁLISE EM TEMPO REAL ==========

    # Ciclo 4 encontrado
    if ($line -match "Ciclo ID=4" -and !$ciclo4Encontrado) {
        Write-Host "CICLO 4 ENCONTRADO: $line" -ForegroundColor Green
        $ciclo4Encontrado = $true
    }
    elseif ($line -match "numeroCiclo=4" -and !$ciclo4Encontrado) {
        Write-Host "CICLO 4 DETECTADO: $line" -ForegroundColor Green
        $ciclo4Encontrado = $true
    }

    # Rota atualizada com ciclo 4
    if ($line -match "atualizada.*ciclo 4|cicloAcertoAtual=4" -and !$rotaAtualizada) {
        Write-Host "ROTA ATUALIZADA: $line" -ForegroundColor Magenta
        $rotaAtualizada = $true
    }

    # Sincronização concluída
    if ($line -match "Sincroniza.*conclu.*sucesso|synchronized.*4" -and !$sincronizacaoOk) {
        Write-Host "SINCRONIZACAO OK: $line" -ForegroundColor Green
        $sincronizacaoOk = $true
    }

    # Erros críticos
    if ($line -match "ERRO.*ciclo|ERROR.*ciclo|Exception.*ciclo") {
        Write-Host "ERRO CRITICO: $line" -ForegroundColor Red
        $errosEncontrados++
    }

    # ========== MOSTRAR LOGS IMPORTANTES ==========

    # Ciclo 4 sendo processado
    if ($line -match "Inserindo novo ciclo.*ID=4|INSERIR.*ciclo.*ID=4") {
        Write-Host "INSERIR: $line" -ForegroundColor Green
    }
    elseif ($line -match "Rota verificada.*cicloAcertoAtual|cicloAcertoAtual.*4") {
        Write-Host "VERIFICAR: $line" -ForegroundColor Yellow
    }
    elseif ($line -match "Total de ciclos.*4") {
        Write-Host "TOTAL: $line" -ForegroundColor Cyan
    }
    elseif ($line -match "verificarCiclosNaoExibidos|forcarAtualizacaoCicloRota") {
        Write-Host "FALLBACK: $line" -ForegroundColor DarkYellow
    }
    elseif ($line -match "Mecanismo de fallback") {
        Write-Host "FALLBACK: $line" -ForegroundColor DarkYellow
    }

    # Warnings importantes
    elseif ($line -match "WARNING.*ciclo|WARN.*ciclo") {
        Write-Host "WARNING: $line" -ForegroundColor Yellow
    }
}

# ========== RESUMO FINAL ==========
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "           RESUMO DA ANÁLISE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Arquivo salvo: $logFile" -ForegroundColor White
Write-Host ""

# Verificar resultados
if ($ciclo4Encontrado) {
    Write-Host "SUCESSO - CICLO 4: Detectado nos logs" -ForegroundColor Green
} else {
    Write-Host "ERRO - CICLO 4: NAO encontrado nos logs" -ForegroundColor Red
}

if ($rotaAtualizada) {
    Write-Host "SUCESSO - ROTA: Atualizada com ciclo 4" -ForegroundColor Green
} else {
    Write-Host "ERRO - ROTA: NAO atualizada" -ForegroundColor Red
}

if ($sincronizacaoOk) {
    Write-Host "SUCESSO - SYNC: Concluida com sucesso" -ForegroundColor Green
} else {
    Write-Host "ERRO - SYNC: Problemas detectados" -ForegroundColor Red
}

if ($errosEncontrados -gt 0) {
    Write-Host "ERRO - ERROS: $errosEncontrados erros encontrados" -ForegroundColor Red
} else {
    Write-Host "SUCESSO - ERROS: Nenhum erro critico" -ForegroundColor Green
}

Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor Yellow
if (!$ciclo4Encontrado) {
    Write-Host "   • Execute sincronização novamente" -ForegroundColor White
    Write-Host "   • Verifique conexão com Firebase" -ForegroundColor White
}
if (!$rotaAtualizada) {
    Write-Host "   • Execute forcarRefreshDados() no app" -ForegroundColor White
    Write-Host "   • Verifique logs de RoutesViewModel" -ForegroundColor White
}
if ($errosEncontrados -gt 0) {
    Write-Host "   • Analise erros no arquivo de log" -ForegroundColor White
    Write-Host "   • Execute: .\analisar-logs-ciclo-4.bat '$logFile'" -ForegroundColor White
}

Write-Host ""
Write-Host "Para analise detalhada, execute:" -ForegroundColor Cyan
Write-Host "   .\analisar-logs-ciclo-4.bat '$logFile'" -ForegroundColor White
Write-Host ""
Write-Host "Captura e analise concluidas!" -ForegroundColor Green
