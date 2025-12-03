# Script simples para capturar logs do CICLO 4
# Versao sem emojis para evitar problemas de encoding

Write-Host "=== CAPTURA DE LOGS - CICLO 4 ===" -ForegroundColor Yellow
Write-Host "Objetivo: Analisar exibicao do ciclo 4 na interface" -ForegroundColor Cyan
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
$ciclo4Count = 0
$rotaUpdatedCount = 0
$syncOkCount = 0
$errorsCount = 0

Write-Host ""
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c
Write-Host "SUCESSO: Logs limpos" -ForegroundColor Green

Write-Host ""
Write-Host "Iniciando captura..." -ForegroundColor Cyan
Write-Host "Arquivo: $logFile" -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Gray
Write-Host ""

# Capturar logs e analisar em tempo real
& $ADB logcat -v time -s SyncRepository:* RoutesViewModel:* RoutesFragment:* | ForEach-Object {
    $line = $_

    # Salvar no arquivo
    $line | Out-File -FilePath $logFile -Append -Encoding UTF8

    # Contar eventos importantes
    if ($line -match "Ciclo ID=4|numeroCiclo=4") {
        $ciclo4Count++
        if ($ciclo4Count -le 3) {  # Mostrar apenas os primeiros
            Write-Host "CICLO 4 ENCONTRADO: $line" -ForegroundColor Green
        }
    }
    elseif ($line -match "atualizada.*ciclo 4|cicloAcertoAtual=4") {
        $rotaUpdatedCount++
        if ($rotaUpdatedCount -le 2) {
            Write-Host "ROTA ATUALIZADA: $line" -ForegroundColor Magenta
        }
    }
    elseif ($line -match "Sincroniza.*conclu.*sucesso|sync=4") {
        $syncOkCount++
        if ($syncOkCount -le 1) {
            Write-Host "SINCRONIZACAO OK: $line" -ForegroundColor Green
        }
    }
    elseif ($line -match "ERRO|ERROR|Exception") {
        $errorsCount++
        Write-Host "ERRO ENCONTRADO: $line" -ForegroundColor Red
    }
}

# Resumo final
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "           RESUMO DA CAPTURA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Arquivo salvo: $logFile" -ForegroundColor White
Write-Host ""
Write-Host "Ciclo 4 encontrado: $ciclo4Count vezes" -ForegroundColor $(if ($ciclo4Count -gt 0) { "Green" } else { "Red" })
Write-Host "Rota atualizada: $rotaUpdatedCount vezes" -ForegroundColor $(if ($rotaUpdatedCount -gt 0) { "Green" } else { "Red" })
Write-Host "Sincronizacoes OK: $syncOkCount vezes" -ForegroundColor $(if ($syncOkCount -gt 0) { "Green" } else { "Red" })
Write-Host "Erros encontrados: $errorsCount vezes" -ForegroundColor $(if ($errorsCount -eq 0) { "Green" } else { "Red" })

Write-Host ""
if ($ciclo4Count -gt 0 -and $rotaUpdatedCount -gt 0 -and $syncOkCount -gt 0 -and $errorsCount -eq 0) {
    Write-Host "RESULTADO: Ciclo 4 esta funcionando corretamente!" -ForegroundColor Green
} else {
    Write-Host "RESULTADO: Problemas detectados com o ciclo 4" -ForegroundColor Red
}

Write-Host ""
Write-Host "Para analise detalhada, execute:" -ForegroundColor Cyan
Write-Host "   .\analisar-logs-ciclo-4.bat '$logFile'" -ForegroundColor White
Write-Host ""
Write-Host "Captura concluida!" -ForegroundColor Green
