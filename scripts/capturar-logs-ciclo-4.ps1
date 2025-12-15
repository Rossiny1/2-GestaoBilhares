# Script para capturar logs específicos do CICLO 4
# Baseado no script capturar-logs-sincronizacao.ps1 que funciona
# Foco: Debugar problemas de exibição do ciclo 4 após sincronização

Write-Host "=== CAPTURA DE LOGS - CICLO 4 ===" -ForegroundColor Yellow
Write-Host "Objetivo: Analisar exibição do ciclo 4 na interface" -ForegroundColor Cyan
Write-Host "Data/Hora: $(Get-Date)" -ForegroundColor Gray
Write-Host ""

# Caminho do ADB (mesmo padrão dos outros scripts)
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (!(Test-Path $ADB)) {
    Write-Host "❌ ADB não encontrado em: $ADB" -ForegroundColor Red
    Write-Host "Certifique-se de que o Android SDK está instalado corretamente" -ForegroundColor Yellow
    exit 1
}

# Verificar se há dispositivo conectado
Write-Host "🔍 Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $ADB devices
if ($devices -match "device$") {
    Write-Host "✅ Dispositivo encontrado!" -ForegroundColor Green
} else {
    Write-Host "❌ Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "💡 Conecte um dispositivo USB ou inicie um emulador" -ForegroundColor Yellow
    exit 1
}

# Limpar logs anteriores
Write-Host ""
Write-Host "🧹 Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c
Write-Host "✅ Logs limpos" -ForegroundColor Green

# Iniciar captura de logs
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "     CAPTURANDO LOGS DO CICLO 4" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "🎯 Filtros ativos para CICLO 4:" -ForegroundColor Yellow
Write-Host "  • SyncRepository (todos os logs)" -ForegroundColor White
Write-Host "  • RoutesViewModel (todos os logs)" -ForegroundColor White
Write-Host "  • RoutesFragment (todos os logs)" -ForegroundColor White
Write-Host "  • Ciclo ID=4 específico" -ForegroundColor Green
Write-Host "  • numeroCiclo=4 específico" -ForegroundColor Green
Write-Host "  • rotaId=1 (rota padrão)" -ForegroundColor Green
Write-Host "  • cicloAcertoAtual=4" -ForegroundColor Green
Write-Host ""
Write-Host "⏳ Aguardando eventos de sincronização..." -ForegroundColor Gray
Write-Host "💡 Pressione Ctrl+C para parar a captura" -ForegroundColor Gray
Write-Host ""

# Capturar logs com filtros específicos para o ciclo 4
& $ADB logcat -v time -s SyncRepository:* RoutesViewModel:* RoutesFragment:* | ForEach-Object {
    $line = $_

    # ========== LOGS DO CICLO 4 ESPECÍFICOS ==========

    # Ciclo 4 sendo processado/criado
    if ($line -match "Ciclo ID=4|Ciclo.*ID=4") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "numeroCiclo=4|ciclo.*4") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "Inserindo novo ciclo.*ID=4|➕.*ciclo.*ID=4") {
        Write-Host $line -ForegroundColor Green
    }

    # Rota sendo atualizada com ciclo 4
    elseif ($line -match "Rota.*atualizada.*ciclo 4|cicloAcertoAtual=4") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "Rota ID=1.*ciclo.*4|rotaId=1.*ciclo.*4") {
        Write-Host $line -ForegroundColor Magenta
    }

    # Verificação da rota após atualização
    elseif ($line -match "Rota verificada.*cicloAcertoAtual|cicloAcertoAtual.*4") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "DEBUG.*Rota.*cicloAcertoAtual") {
        Write-Host $line -ForegroundColor Yellow
    }

    # Status do ciclo 4
    elseif ($line -match "status=EM_ANDAMENTO.*ID=4|EM_ANDAMENTO.*numeroCiclo=4") {
        Write-Host $line -ForegroundColor Blue
    }
    elseif ($line -match "FINALIZADO.*ID=4|numeroCiclo=4.*FINALIZADO") {
        Write-Host $line -ForegroundColor Red
    }

    # ========== LOGS DE SINCRONIZAÇÃO GERAIS ==========

    # Sincronização concluída
    elseif ($line -match "Sincroniza.*conclu.*sucesso|Pull.*conclu.*do|Sync.*conclu") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "synchronized.*4.*sync|sync=4") {
        Write-Host $line -ForegroundColor Green
    }

    # Processamento de ciclos
    elseif ($line -match "Processando ciclo|processCiclosDocuments") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "Total de ciclos.*4|ciclos.*4") {
        Write-Host $line -ForegroundColor Cyan
    }

    # Atualização de rotas após sync
    elseif ($line -match "atualizada.*ciclo|atualizarRota|cicloParaRota") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "Timestamp.*atualizado.*refresh|forçar.*refresh") {
        Write-Host $line -ForegroundColor DarkMagenta
    }

    # ========== LOGS DE DEBUG E ERROS ==========

    # Refresh forçado
    elseif ($line -match "forcarRefreshDados|verificarCiclosNaoExibidos") {
        Write-Host $line -ForegroundColor DarkYellow
    }
    elseif ($line -match "Mecanismo de fallback|ciclo.*nao.*exibido") {
        Write-Host $line -ForegroundColor DarkYellow
    }

    # Erros críticos
    elseif ($line -match "ERRO.*ciclo|ERROR.*ciclo|Exception.*ciclo") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "Falha.*ciclo|falhou.*ciclo") {
        Write-Host $line -ForegroundColor Red
    }

    # ========== LOGS DO ROUTESVIEWMODEL ==========

    # Filtros e ciclos
    elseif ($line -match "RoutesViewModel.*ciclo|ciclo.*RoutesViewModel") {
        Write-Host $line -ForegroundColor DarkGreen
    }
    elseif ($line -match "RoutesViewModel.*filtr|filtr.*RoutesViewModel") {
        Write-Host $line -ForegroundColor DarkGreen
    }

    # ========== OUTROS LOGS IMPORTANTES ==========

    # Ciclos em geral
    elseif ($line -match "buscarCicloEmAndamento|obterCicloAtualRota") {
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "cicloAtivo|ciclo.*ativo") {
        Write-Host $line -ForegroundColor Gray
    }

    # Só mostra outros logs se forem muito relevantes
    elseif ($line -match "SyncRepository.*DEBUG|RoutesViewModel.*DEBUG") {
        Write-Host $line -ForegroundColor DarkGray
    }

    # ========== LOGS DE INÍCIO/FIM ==========

    elseif ($line -match "Iniciando.*pull.*ciclos|pull.*ciclos.*iniciando") {
        Write-Host $line -ForegroundColor Blue
    }
    elseif ($line -match "Pull.*Ciclos.*sincronizados|pull.*ciclos.*conclu") {
        Write-Host $line -ForegroundColor Blue
    }

    # ========== MOSTRAR TUDO SE FOR MUITO ESPECÍFICO ==========

    # Se não entrou em nenhuma categoria específica, mas contém palavras-chave importantes
    elseif ($line -match "4.*ciclo|ciclo.*4|rota.*1|cicloAcertoAtual") {
        Write-Host $line -ForegroundColor White
    }

    # ========== LOGS DE WARNING/INFO GERAIS ==========

    elseif ($line -match "WARNING|WARN") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "INFO.*ciclo|INFO.*rota") {
        Write-Host $line -ForegroundColor DarkCyan
    }
}
