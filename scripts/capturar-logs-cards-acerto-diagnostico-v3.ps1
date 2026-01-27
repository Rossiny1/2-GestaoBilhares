# Script para capturar logs de diagnostico do problema de Cards do Acerto nao aparecerem
# Foco: Registrar TODAS as atividades do fluxo de Acerto, Nova Reforma e carregamento de Cards
# Versao: 3.0 - Corrigido encoding e sintaxe PowerShell

Write-Host "=== CAPTURA DE LOGS - DIAGNOSTICO CARDS ACERTO ===" -ForegroundColor Yellow
Write-Host "Objetivo: Capturar TODAS as atividades para descobrir por que Cards do Acerto nao aparecem" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB (mesmo padrao dos outros scripts)
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (!(Test-Path $ADB)) {
    Write-Host "ADB nao encontrado em: $ADB" -ForegroundColor Red
    Write-Host "Certifique-se de que o Android SDK esta instalado corretamente" -ForegroundColor Red
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

# Limpar logs anteriores
Write-Host ""
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c

# Iniciar captura de logs
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CAPTURANDO LOGS DO DIAGNOSTICO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtros ativos:" -ForegroundColor Yellow
Write-Host "  - DEBUG_CARDS (todos os niveis)" -ForegroundColor White
Write-Host "  - RegistrarTrocaPanoUseCase (todos os niveis)" -ForegroundColor White
Write-Host "  - SettlementViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - NovaReformaViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - MesasReformadasViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - AppRepository (todos os niveis)" -ForegroundColor White
Write-Host "  - BaseViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - AndroidRuntime (erros)" -ForegroundColor White
Write-Host ""
Write-Host "Eventos monitorados:" -ForegroundColor Cyan
Write-Host "  - Acesso as telas (Nova Reforma, Acerto, Reforma de Mesas)" -ForegroundColor White
Write-Host "  - Cliques em botoes (Salvar, Trocar Pano, Confirmar)" -ForegroundColor White
Write-Host "  - Execucao do Use Case (RegistrarTrocaPanoUseCase)" -ForegroundColor White
Write-Host "  - Insercoes no banco Room (mesas_reformadas, historico_manutencao_mesa)" -ForegroundColor White
Write-Host "  - Carregamento dos cards (MesasReformadasViewModel)" -ForegroundColor White
Write-Host "  - Erros e excecoes" -ForegroundColor White
Write-Host ""
Write-Host "INSTRUCOES EXATAS:" -ForegroundColor Green
Write-Host "  1. Execute os passos abaixo ENQUANTO o script captura:" -ForegroundColor White
Write-Host "  2. Nova Reforma:" -ForegroundColor Yellow
Write-Host "     - Abra o app > Mesas > Nova Reforma" -ForegroundColor Gray
Write-Host "     - Selecione uma mesa (ex: M01)" -ForegroundColor Gray
Write-Host "     - Marque 'Panos' e escolha um pano" -ForegroundColor Gray
Write-Host "     - Salve a reforma" -ForegroundColor Gray
Write-Host "  3. Acerto:" -ForegroundColor Yellow
Write-Host "     - Va para Acerto" -ForegroundColor Gray
Write-Host "     - Selecione um cliente" -ForegroundColor Gray
Write-Host "     - Adicione uma mesa (ex: M02)" -ForegroundColor Gray
Write-Host "     - MARQUE 'Trocar Pano' e informe o pano" -ForegroundColor Gray
Write-Host "     - Salve o acerto" -ForegroundColor Gray
Write-Host "  4. Verificacao:" -ForegroundColor Yellow
Write-Host "     - Abra a tela 'Reforma de Mesas'" -ForegroundColor Gray
Write-Host "     - Verifique se ambos os cards aparecem" -ForegroundColor Gray
Write-Host "  5. Pressione Ctrl+C para parar a captura" -ForegroundColor Red
Write-Host ""

# Iniciar captura usando Start-Process e redirecionar saida
Write-Host "Iniciando captura de logs..." -ForegroundColor Cyan

# Criar processo para capturar logs
$process = Start-Process -FilePath $ADB -ArgumentList "logcat", "-v", "time", "-s", "DEBUG_CARDS:*", "RegistrarTrocaPanoUseCase:*", "SettlementViewModel:*", "NovaReformaViewModel:*", "MesasReformadasViewModel:*", "AppRepository:*", "BaseViewModel:*", "AndroidRuntime:*" -PassThru -NoNewWindow

# Capturar saida do processo
while (!$process.HasExited) {
    $line = $process.StandardOutput.ReadLine()
    if ($line -ne $null) {
        # Logs DEBUG_CARDS - Use Case
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
        elseif ($line -match "DEBUG_CARDS") {
            Write-Host $line -ForegroundColor White
        }
        
        # Logs do RegistrarTrocaPanoUseCase
        elseif ($line -match "RegistrarTrocaPanoUseCase.*invoke|RegistrarTrocaPanoUseCase.*INICIADO") {
            Write-Host $line -ForegroundColor Cyan -BackgroundColor DarkBlue
        }
        elseif ($line -match "RegistrarTrocaPanoUseCase.*Mesa encontrada|RegistrarTrocaPanoUseCase.*mesaId") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "RegistrarTrocaPanoUseCase.*Inserindo MesaReformada|RegistrarTrocaPanoUseCase.*MesaReformada inserida") {
            Write-Host $line -ForegroundColor Yellow
        }
        elseif ($line -match "RegistrarTrocaPanoUseCase.*Inserindo HistoricoManutencaoMesa|RegistrarTrocaPanoUseCase.*HistoricoManutencaoMesa inserido") {
            Write-Host $line -ForegroundColor Yellow
        }
        elseif ($line -match "RegistrarTrocaPanoUseCase.*Atualizando mesa|RegistrarTrocaPanoUseCase.*Mesa atualizada") {
            Write-Host $line -ForegroundColor DarkGreen
        }
        elseif ($line -match "RegistrarTrocaPanoUseCase.*CONCLUIDO|RegistrarTrocaPanoUseCase.*SUCESSO") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "RegistrarTrocaPanoUseCase.*ERRO|RegistrarTrocaPanoUseCase.*Exception") {
            Write-Host $line -ForegroundColor Red -BackgroundColor White
        }
        
        # Logs do SettlementViewModel
        elseif ($line -match "SettlementViewModel.*registrarTrocaPanoNoHistorico|SettlementViewModel.*Chamando use case") {
            Write-Host $line -ForegroundColor Magenta
        }
        elseif ($line -match "SettlementViewModel.*INICIADO|SettlementViewModel.*PROCESSANDO") {
            Write-Host $line -ForegroundColor Cyan
        }
        elseif ($line -match "SettlementViewModel.*SUCESSO|SettlementViewModel.*CONCLUIDO") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "SettlementViewModel.*ERRO|SettlementViewModel.*Exception") {
            Write-Host $line -ForegroundColor Red
        }
        elseif ($line -match "SettlementViewModel.*mesas.*processadas|SettlementViewModel.*pano") {
            Write-Host $line -ForegroundColor Yellow
        }
        
        # Logs do NovaReformaViewModel
        elseif ($line -match "NovaReformaViewModel.*salvarReforma|NovaReformaViewModel.*SALVANDO") {
            Write-Host $line -ForegroundColor Cyan
        }
        elseif ($line -match "NovaReformaViewModel.*MesaReformada inserida|NovaReformaViewModel.*SUCESSO") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "NovaReformaViewModel.*ERRO|NovaReformaViewModel.*Exception") {
            Write-Host $line -ForegroundColor Red
        }
        
        # Logs do MesasReformadasViewModel
        elseif ($line -match "MesasReformadasViewModel.*carregarMesasReformadas|MesasReformadasViewModel.*CARREGANDO") {
            Write-Host $line -ForegroundColor Cyan -BackgroundColor DarkCyan
        }
        elseif ($line -match "MesasReformadasViewModel.*Dados recebidos|MesasReformadasViewModel.*Total") {
            Write-Host $line -ForegroundColor Cyan
        }
        elseif ($line -match "MesasReformadasViewModel.*Reformas do ACERTO|MesasReformadasViewModel.*Historicos do ACERTO") {
            Write-Host $line -ForegroundColor Magenta -BackgroundColor Black
        }
        elseif ($line -match "MesasReformadasViewModel.*Cards gerados|MesasReformadasViewModel.*CONCLUIDO") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "MesasReformadasViewModel.*ERRO|MesasReformadasViewModel.*Exception") {
            Write-Host $line -ForegroundColor Red -BackgroundColor White
        }
        
        # Logs do AppRepository
        elseif ($line -match "AppRepository.*inserirMesaReformada|AppRepository.*MesaReformada.*inserida") {
            Write-Host $line -ForegroundColor Yellow
        }
        elseif ($line -match "AppRepository.*inserirHistoricoManutencaoMesa|AppRepository.*HistoricoManutencaoMesa.*inserido") {
            Write-Host $line -ForegroundColor Yellow
        }
        elseif ($line -match "AppRepository.*atualizarMesa|AppRepository.*Mesa.*atualizada") {
            Write-Host $line -ForegroundColor DarkGreen
        }
        elseif ($line -match "AppRepository.*obterTodasMesasReformadas|AppRepository.*obterTodosHistoricoManutencaoMesa") {
            Write-Host $line -ForegroundColor Cyan
        }
        elseif ($line -match "AppRepository.*ERRO|AppRepository.*Exception") {
            Write-Host $line -ForegroundColor Red
        }
        
        # Logs do BaseViewModel
        elseif ($line -match "BaseViewModel.*showLoading|BaseViewModel.*hideLoading") {
            Write-Host $line -ForegroundColor Gray
        }
        elseif ($line -match "BaseViewModel.*ERRO|BaseViewModel.*Exception") {
            Write-Host $line -ForegroundColor Red
        }
        
        # Logs de erros do AndroidRuntime
        elseif ($line -match "AndroidRuntime.*FATAL|AndroidRuntime.*Exception") {
            Write-Host $line -ForegroundColor Red -BackgroundColor White
        }
        elseif ($line -match ".*at .*\(.*\)|.*Caused by:|.*Suppressed:") {
            Write-Host $line -ForegroundColor DarkRed
        }
        
        # Logs de Fragmentos e Activities
        elseif ($line -match "Fragment.*onCreate|Fragment.*onViewCreated|Fragment.*onResume") {
            Write-Host $line -ForegroundColor DarkGray
        }
        elseif ($line -match "Activity.*onCreate|Activity.*onResume") {
            Write-Host $line -ForegroundColor DarkGray
        }
        
        # Logs de cliques e interacoes
        elseif ($line -match "onClick|onClickListener|Button.*clicked") {
            Write-Host $line -ForegroundColor White
        }
        
        # Logs de banco de dados Room
        elseif ($line -match "Room|SQL|INSERT|UPDATE|SELECT") {
            Write-Host $line -ForegroundColor DarkCyan
        }
        
        else {
            Write-Host $line
        }
    }
    Start-Sleep -Milliseconds 10
}

Write-Host ""
Write-Host "Captura finalizada." -ForegroundColor Green
