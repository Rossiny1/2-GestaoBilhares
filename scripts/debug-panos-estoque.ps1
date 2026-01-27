# Script para depurar criação de panos no estoque
# Foco: Analisar por que os cards de panos não aparecem após criação
# Versão: 2.0 - V10 Fix - @Transaction + loop individual

Write-Host "=== DEBUG DE PANOS NO ESTOQUE ===" -ForegroundColor Yellow
Write-Host "Objetivo: Identificar por que os cards de panos não aparecem após criação" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB (mesmo padrão dos outros scripts)
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (!(Test-Path $ADB)) {
    Write-Host "ADB não encontrado em: $ADB" -ForegroundColor Red
    Write-Host "Certifique-se de que o Android SDK está instalado corretamente" -ForegroundColor Red
    exit 1
}

# Verificar se há dispositivo conectado
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $ADB devices
if ($devices -match "device$") {
    Write-Host "Dispositivo encontrado!" -ForegroundColor Green
}
else {
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
Write-Host "  CAPTURANDO LOGS DE PANOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtros ativos:" -ForegroundColor Yellow
Write-Host "  - AddPanosLoteDialog (criação)" -ForegroundColor White
Write-Host "  - StockViewModel (inserção)" -ForegroundColor White
Write-Host "  - PanoRepository (repositório)" -ForegroundColor White
Write-Host "  - PanoEstoqueDao (banco)" -ForegroundColor White
Write-Host "  - StockFragment (UI)" -ForegroundColor White
Write-Host "  - AppRepository (facade)" -ForegroundColor White
Write-Host ""
Write-Host "Logs específicos capturados:" -ForegroundColor Cyan
Write-Host "  - Criação de panos no Dialog" -ForegroundColor White
Write-Host "  - Inserção individual no banco" -ForegroundColor White
Write-Host "  - Notificação do Flow do Room" -ForegroundColor White
Write-Host "  - Agrupamento de panos" -ForegroundColor White
Write-Host "  - Atualização do adapter da UI" -ForegroundColor White
Write-Host "  - Erros e exceções" -ForegroundColor White
Write-Host ""
Write-Host "Aguardando criação de panos..." -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Gray
Write-Host ""
Write-Host "PASSOS PARA TESTAR:" -ForegroundColor Yellow
Write-Host "1. Abra o app" -ForegroundColor White
Write-Host "2. Vá para a tela de Estoque" -ForegroundColor White
Write-Host "3. Clique em 'Adicionar Panos em Lote'" -ForegroundColor White
Write-Host "4. Preencha: Tamanho=Grande, Quantidade=3" -ForegroundColor White
Write-Host "5. Clique em 'Criar Panos'" -ForegroundColor White
Write-Host "6. Observe se os cards aparecem" -ForegroundColor White
Write-Host ""

# Capturar logs com filtros específicos para panos (V10)
& $ADB logcat -v time -s AddPanosLoteDialog:* StockViewModel:* PanoRepository:* PanoEstoqueDao:* StockFragment:* AppRepository:* | ForEach-Object {
    $line = $_
    
    # ========== V10: LOGS DO DAO (PRIORIDADE ALTA) ==========
    # PanoEstoqueDao - @Transaction e loop individual
    if ($line -match "PanoEstoqueDao.*=== INÍCIO inserirLote @Transaction") {
        Write-Host ""
        Write-Host ">>> DAO @TRANSACTION INICIOU <<<" -ForegroundColor Black -BackgroundColor Green
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "PanoEstoqueDao.*Inserindo.*panos individualmente") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "PanoEstoqueDao.*Pano.*inserido:") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "PanoEstoqueDao.*=== FIM inserirLote") {
        Write-Host $line -ForegroundColor Green
        Write-Host ">>> DAO @TRANSACTION CONCLUIU <<<" -ForegroundColor Black -BackgroundColor Green
        Write-Host ""
    }
    
    # ========== V10: LOGS DO REPOSITORY ==========
    elseif ($line -match "PanoRepository.*=== INÍCIO inserirLote") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "PanoRepository.*Recebidos.*panos para inserir") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "PanoRepository.*=== FIM inserirLote.*DAO concluído") {
        Write-Host $line -ForegroundColor Yellow
    }
    
    # ========== V10: LOGS DO DIALOG (SIMPLIFICADO) ==========
    elseif ($line -match "AddPanosLoteDialog.*Total de panos criados|AddPanosLoteDialog.*criando pano") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "AddPanosLoteDialog.*Chamando viewModel.adicionarPanosLote") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "AddPanosLoteDialog.*panos criados") {
        Write-Host $line -ForegroundColor Green -BackgroundColor DarkBlue
    }
    elseif ($line -match "AddPanosLoteDialog.*ERRO|AddPanosLoteDialog.*Erro") {
        Write-Host $line -ForegroundColor Red
    }
    
    # ========== V10: LOGS DO VIEWMODEL ==========
    elseif ($line -match "StockViewModel.*=== INÍCIO ADIÇÃO PANOS") {
        Write-Host ""
        Write-Host ">>> VIEWMODEL INICIOU <<<" -ForegroundColor Black -BackgroundColor Cyan
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "StockViewModel.*Recebidos.*panos para inserir") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "StockViewModel.*Validando duplicidade|StockViewModel.*Validação OK") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "StockViewModel.*Inserindo panos individualmente") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "StockViewModel.*Pano.*inserido individualmente") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "StockViewModel.*=== FIM ADIÇÃO PANOS") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "StockViewModel.*=== AGUARDANDO FLOW ATUALIZAR UI") {
        Write-Host $line -ForegroundColor Magenta
        Write-Host ">>> VIEWMODEL CONCLUIU - AGUARDANDO FLOW <<<" -ForegroundColor Black -BackgroundColor Magenta
        Write-Host ""
    }
    elseif ($line -match "StockViewModel.*=== ERRO AO ADICIONAR PANOS") {
        Write-Host $line -ForegroundColor Red -BackgroundColor Yellow
    }
    elseif ($line -match "StockViewModel.*Mensagem:") {
        Write-Host $line -ForegroundColor Red
    }
    
    # ========== V10: LOGS DE AGRUPAMENTO (FLOW) ==========
    elseif ($line -match "StockViewModel.*Agrupando.*panos") {
        Write-Host ""
        Write-Host ">>> FLOW NOTIFICOU - AGRUPANDO <<<" -ForegroundColor Black -BackgroundColor Blue
        Write-Host $line -ForegroundColor Blue
    }
    elseif ($line -match "StockViewModel.*Mapeando.*itens do banco") {
        Write-Host $line -ForegroundColor Blue
    }
    elseif ($line -match "StockViewModel.*Total de grupos criados") {
        Write-Host $line -ForegroundColor Green
        Write-Host ">>> UI DEVE ATUALIZAR AGORA <<<" -ForegroundColor Black -BackgroundColor Green
        Write-Host ""
    }
    elseif ($line -match "StockViewModel.*Grupo.*disponíveis") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "StockViewModel.*Pano.*disponivel=") {
        Write-Host $line -ForegroundColor DarkGray
    }
    
    # ========== LOGS DO FRAGMENT ==========
    elseif ($line -match "StockFragment.*observeData|StockFragment.*Grupos de panos recebidos") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "StockFragment.*submitList") {
        Write-Host $line -ForegroundColor Green -BackgroundColor DarkBlue
    }
    elseif ($line -match "StockFragment.*ERRO|StockFragment.*Erro") {
        Write-Host $line -ForegroundColor Red
    }
    
    # ========== LOGS DE ERRO ==========
    elseif ($line -match "Exception:|ERROR:|FATAL:|CancellationException") {
        Write-Host $line -ForegroundColor Red -BackgroundColor Yellow
    }
    
    # ========== OUTROS LOGS ==========
    else {
        Write-Host $line -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Captura de logs finalizada" -ForegroundColor Yellow
Write-Host "Analise os logs para identificar onde o fluxo está quebrado" -ForegroundColor Cyan
