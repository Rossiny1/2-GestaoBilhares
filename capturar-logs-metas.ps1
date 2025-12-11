# Script para capturar logs de metas
# Foco: Debugar problemas de carregamento, exibicao e criacao de metas
# Versao: 1.0 - Baseado em capturar-logs-sincronizacao.ps1

Write-Host "=== CAPTURA DE LOGS DE METAS ===" -ForegroundColor Yellow
Write-Host "Objetivo: Analisar o fluxo de carregamento, exibicao e criacao de metas" -ForegroundColor Cyan
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
Write-Host "  CAPTURANDO LOGS DE METAS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtros ativos:" -ForegroundColor Yellow
Write-Host "  - MetasViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - MetasFragment (todos os niveis)" -ForegroundColor White
Write-Host "  - MetaCadastroFragment (todos os niveis)" -ForegroundColor White
Write-Host "  - MetasAdapter (todos os niveis)" -ForegroundColor White
Write-Host "  - MetaCadastroViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - AppRepository (busca de metas e ciclos)" -ForegroundColor White
Write-Host ""
Write-Host "Logs especificos capturados:" -ForegroundColor Cyan
Write-Host "  - Carregamento de metas por rota" -ForegroundColor White
Write-Host "  - Busca de ciclos (ultimo ciclo, ciclo ativo, ciclo atual)" -ForegroundColor White
Write-Host "  - Busca de metas por rota e ciclo" -ForegroundColor White
Write-Host "  - Criacao de MetaRotaResumo" -ForegroundColor White
Write-Host "  - Calculo de progresso das metas" -ForegroundColor White
Write-Host "  - Salvamento de novas metas" -ForegroundColor White
Write-Host "  - Navegacao e cliques nos cards" -ForegroundColor White
Write-Host "  - Erros e excecoes" -ForegroundColor White
Write-Host ""
Write-Host "Aguardando eventos de metas..." -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Gray
Write-Host ""

# Capturar logs com filtros especificos (usando o mesmo padrao do script que funcionava)
# Timber usa o nome da classe como tag, então precisamos filtrar por MetasViewModel, etc.
& $ADB logcat -v time -s MetasViewModel:* MetasFragment:* MetaCadastroFragment:* MetasAdapter:* MetaCadastroViewModel:* AppRepository:* | ForEach-Object {
    $line = $_
    
    # Cores para diferentes tipos de logs
    
    # Logs de MetasViewModel - Carregamento
    if ($line -match "MetasViewModel.*Carregando metas|MetasViewModel.*carregarMetasRotas") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "MetasViewModel.*Rotas ativas|MetasViewModel.*Processando rota") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "MetasViewModel.*Criando MetaRotaResumo|MetasViewModel.*criarMetaRotaResumo") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "MetasViewModel.*MetaRota criada|MetasViewModel.*Total de MetaRotas") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "MetasViewModel.*MetaRota nao criada") {
        Write-Host $line -ForegroundColor Yellow
    }
    
    # Logs de MetasViewModel - Busca de Ciclos
    elseif ($line -match "MetasViewModel.*Buscando ultimo ciclo|MetasViewModel.*buscarUltimoCicloPorRota|MetasViewModel.*buscarCicloAtivo|MetasViewModel.*buscarCicloAtualPorRota") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "MetasViewModel.*Ciclo encontrado|MetasViewModel.*Resultado buscarUltimoCicloPorRota|MetasViewModel.*Resultado buscarCicloAtivo|MetasViewModel.*Resultado buscarCicloAtualPorRota") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "MetasViewModel.*Nenhum ciclo encontrado|MetasViewModel.*Tentando buscarCiclo") {
        Write-Host $line -ForegroundColor Yellow
    }
    
    # Logs de MetasViewModel - Busca de Metas
    elseif ($line -match "MetasViewModel.*Buscando metas|MetasViewModel.*buscarMetasPorRotaECiclo") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "MetasViewModel.*Metas encontradas|MetasViewModel.*Metas encontradas para ciclo") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "MetasViewModel.*Nenhuma meta encontrada|MetasViewModel.*buscando em todos os ciclos") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "MetasViewModel.*Total de ciclos encontrados|MetasViewModel.*Ciclo.*metas") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "MetasViewModel.*Encontradas.*metas no ciclo|MetasViewModel.*usando este ciclo") {
        Write-Host $line -ForegroundColor Green
    }
    
    # Logs de MetasViewModel - Calculo de Progresso
    elseif ($line -match "MetasViewModel.*Calculando progresso|MetasViewModel.*calcularProgressoMetas") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "MetasViewModel.*Faturamento calculado|MetasViewModel.*Clientes acertados|MetasViewModel.*Novas mesas no ciclo|MetasViewModel.*Ticket medio") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "MetasViewModel.*Meta atualizada|MetasViewModel.*Meta.*persistida") {
        Write-Host $line -ForegroundColor Green
    }
    
    # Logs de MetasViewModel - Colaborador Responsavel
    elseif ($line -match "MetasViewModel.*Buscando colaborador responsavel|MetasViewModel.*Colaborador responsavel encontrado|MetasViewModel.*Nenhum colaborador responsavel") {
        Write-Host $line -ForegroundColor DarkYellow
    }
    
    # Logs de MetasViewModel - Erros
    elseif ($line -match "MetasViewModel.*ERRO|MetasViewModel.*Erro|MetasViewModel.*ERROR|MetasViewModel.*Exception") {
        Write-Host $line -ForegroundColor Red
    }
    
    # Logs de MetasFragment - Navegacao e Cliques
    elseif ($line -match "MetasFragment.*onViewCreated|MetasFragment.*onResume|MetasFragment.*carregarMetasRotas") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "MetasFragment.*onDetailsClick|MetasFragment.*mostrarDialogHistoricoMetas|MetasFragment.*Erro ao abrir historico") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "MetasFragment.*ERRO|MetasFragment.*Erro|MetasFragment.*ERROR|MetasFragment.*Exception") {
        Write-Host $line -ForegroundColor Red
    }
    
    # Logs de MetasAdapter - Configuracao de RecyclerView
    elseif ($line -match "MetasAdapter.*Configurando RecyclerView|MetasAdapter.*metas para rota") {
        Write-Host $line -ForegroundColor Green
    }
    
    # Logs de MetaCadastroFragment - Salvamento
    elseif ($line -match "MetaCadastroFragment.*salvarMeta|MetaCadastroFragment.*Salvando meta") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "MetaCadastroFragment.*Meta salva com sucesso|MetaCadastroFragment.*Navegando de volta") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "MetaCadastroFragment.*Ciclo selecionado|MetaCadastroFragment.*carregarCiclosPorRota") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "MetaCadastroFragment.*ERRO|MetaCadastroFragment.*Erro|MetaCadastroFragment.*ERROR|MetaCadastroFragment.*Exception") {
        Write-Host $line -ForegroundColor Red
    }
    
    # Logs de MetaCadastroViewModel - Salvamento
    elseif ($line -match "MetaCadastroViewModel.*Salvando meta|MetaCadastroViewModel.*Meta salva com ID") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "MetaCadastroViewModel.*existeMetaDuplicada|MetaCadastroViewModel.*Já existe uma meta") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "MetaCadastroViewModel.*Criando ciclo|MetaCadastroViewModel.*Ciclo criado com ID") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "MetaCadastroViewModel.*ERRO|MetaCadastroViewModel.*Erro|MetaCadastroViewModel.*ERROR|MetaCadastroViewModel.*Exception") {
        Write-Host $line -ForegroundColor Red
    }
    
    # Logs de AppRepository - Busca de Metas e Ciclos
    elseif ($line -match "AppRepository.*buscarMetasPorRotaECiclo|AppRepository.*buscarUltimoCicloPorRota|AppRepository.*buscarCicloAtivo|AppRepository.*buscarCiclosPorRota") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "AppRepository.*inserirMeta|AppRepository.*atualizarValorAtualMeta") {
        Write-Host $line -ForegroundColor Green
    }
    
    # Logs de separadores e marcadores especiais
    elseif ($line -match "════════════════════════════════════════") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "MetasViewModel.*✅") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "MetasViewModel.*⚠️") {
        Write-Host $line -ForegroundColor Yellow
    }
    
    # Logs de stack traces e excecoes gerais
    elseif ($line -match ".*Stack trace:|.*Exception:|.*ERROR:|.*CRITICO:|.*FATAL:") {
        Write-Host $line -ForegroundColor Red
    }
    else {
        Write-Host $line
    }
}

