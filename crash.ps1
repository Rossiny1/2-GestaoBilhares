# ========================================
# SCRIPT DE MONITORAMENTO DE CRASH - GESTAO BILHARES
# ========================================
#
# INSTRUÇÕES IMPORTANTES:
# 1. Execute este script em uma NOVA JANELA do PowerShell
# 2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell
# 3. Navegue até a pasta do projeto: cd "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
# 4. Execute: .\crash.ps1
#
# CAMINHO CORRETO DO ADB:
# C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MONITORAMENTO DE CRASH - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
param(
    [switch]$OnlyVenda,     # Mostra apenas logs do fluxo de venda (tag: VendaMesaDialog)
    [switch]$OnlyAbastecimento,  # Mostra apenas logs do fluxo de abastecimento
    [switch]$OnlyPanos,     # Mostra apenas logs do sistema de panos
    [switch]$OnlyEstoque,   # Mostra apenas logs do sistema de estoque
    [switch]$OnlyMetas,     # Mostra apenas logs do sistema de metas
    [switch]$OnlyPopulation, # Mostra apenas logs de população do banco de dados
    [switch]$OnlyNavegacao, # Mostra apenas logs de navegação após envio de contratos/aditivos
    [switch]$ToFile         # Salva a saída em arquivo (além de mostrar na tela)
)

Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Este script deve ser executado em JANELA SEPARADA" -ForegroundColor Yellow
Write-Host "2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell" -ForegroundColor Yellow
Write-Host "3. Navegue até: cd 'C:\Users\Rossiny\Desktop\2-GestaoBilhares'" -ForegroundColor Yellow
Write-Host "4. Execute: .\crash.ps1" -ForegroundColor Yellow
Write-Host ""
Write-Host "PARAMETROS DISPONIVEIS:" -ForegroundColor Cyan
Write-Host "-OnlyVenda: Monitora apenas logs de venda de mesas" -ForegroundColor Gray
Write-Host "-OnlyAbastecimento: Monitora apenas logs de abastecimento" -ForegroundColor Gray
Write-Host "-OnlyPanos: Monitora apenas logs do sistema de panos" -ForegroundColor Gray
Write-Host "-OnlyEstoque: Monitora apenas logs do sistema de estoque" -ForegroundColor Gray
Write-Host "-OnlyMetas: Monitora apenas logs do sistema de metas" -ForegroundColor Gray
Write-Host "-OnlyPopulation: Monitora apenas logs de populacao do banco de dados" -ForegroundColor Gray
Write-Host "-OnlyNavegacao: Monitora apenas logs de navegação após envio de contratos/aditivos" -ForegroundColor Gray
Write-Host "-ToFile: Salva logs em arquivo" -ForegroundColor Gray
Write-Host ""
Write-Host "EXEMPLOS:" -ForegroundColor Cyan
Write-Host ".\crash.ps1 -OnlyAbastecimento" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyAbastecimento -ToFile" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyPanos" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyPanos -ToFile" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyEstoque" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyEstoque -ToFile" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyMetas" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyMetas -ToFile" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyNavegacao" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyNavegacao -ToFile" -ForegroundColor Gray
Write-Host ""
Write-Host "CAMINHO ADB: C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" -ForegroundColor Green
Write-Host ""
Write-Host "MONITORANDO CRASHES DO APP..." -ForegroundColor Red
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se o ADB está disponível
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK está instalado corretamente" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

# Verificar se o dispositivo está conectado
Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
$devices = & $adbPath devices

Write-Host "Dispositivos encontrados:" -ForegroundColor Yellow
Write-Host $devices -ForegroundColor Gray

# Corrigir a verificação de dispositivo - verificar se há linha com "device" no final
$hasDevice = $devices | Select-String "device$"

if (-not $hasDevice) {
    Write-Host "ERRO: Nenhum dispositivo Android conectado" -ForegroundColor Red
    Write-Host "Conecte o dispositivo via USB e habilite a depuração" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Dispositivo conectado: OK" -ForegroundColor Green
Write-Host ""

# Limpar logcat anterior
Write-Host "Limpando logcat anterior..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "Iniciando monitoramento de crashes..." -ForegroundColor Green
Write-Host "Agora você pode testar o app no dispositivo" -ForegroundColor Green
Write-Host ""
Write-Host "NOVOS LOGS MONITORADOS:" -ForegroundColor Cyan
Write-Host "- VendaMesaDialog: clique vender, validacao, transacao, pos-transacao" -ForegroundColor Gray
Write-Host "- HistoricoMesasVendidasFragment: coleta e atualizacao da lista" -ForegroundColor Gray
Write-Host "- MesasDepositoFragment: fluxo de deposito" -ForegroundColor Gray
Write-Host "- Rotas/Repositorios: eventos gerais" -ForegroundColor Gray
Write-Host "- ExpenseRegisterViewModel: salvamento de despesas e abastecimento" -ForegroundColor Gray
Write-Host "- HistoricoCombustivelVeiculo: logs de debug do abastecimento" -ForegroundColor Gray
Write-Host "- VehicleDetailViewModel: carregamento de dados reais" -ForegroundColor Gray
Write-Host "- SISTEMA DE PANOS: logs de debug do gerenciamento de panos" -ForegroundColor Gray
Write-Host "- SettlementFragment: troca de panos no acerto" -ForegroundColor Gray
Write-Host "- NovaReformaFragment: seleção de panos na reforma" -ForegroundColor Gray
Write-Host "- PanoSelectionDialog: diálogo de seleção de panos" -ForegroundColor Gray
Write-Host "- PanoEstoqueRepository: operações de estoque de panos" -ForegroundColor Gray
Write-Host "- SettlementViewModel: gerenciamento de panos no acerto" -ForegroundColor Gray
Write-Host "- NovaReformaViewModel: gerenciamento de panos na reforma" -ForegroundColor Gray
Write-Host "- SISTEMA DE ESTOQUE: logs de debug do gerenciamento de itens genéricos" -ForegroundColor Gray
Write-Host "- StockFragment: exibição e atualização da lista de itens" -ForegroundColor Gray
Write-Host "- StockViewModel: carregamento e salvamento de itens" -ForegroundColor Gray
Write-Host "- AddEditStockItemDialog: diálogo de adição de itens" -ForegroundColor Gray
Write-Host "- StockItemRepository: operações de banco de dados" -ForegroundColor Gray
Write-Host "- SISTEMA DE METAS: logs de debug do gerenciamento de metas por rota" -ForegroundColor Gray
Write-Host "- MetasFragment: exibição e carregamento de metas" -ForegroundColor Gray
Write-Host "- MetasViewModel: carregamento e cálculo de progresso das metas" -ForegroundColor Gray
Write-Host "- MetasAdapter: exibição dinâmica das metas" -ForegroundColor Gray
Write-Host "- MetaCadastroFragment: cadastro de novas metas" -ForegroundColor Gray
Write-Host "- MetaCadastroViewModel: criação de ciclos e salvamento de metas" -ForegroundColor Gray
Write-Host "- AppRepository: busca de metas por rota e ciclo" -ForegroundColor Gray
Write-Host "- ColaboradorDao: operações de banco de dados de metas" -ForegroundColor Gray
Write-Host "- CicloAcertoDao: criação e gerenciamento de ciclos" -ForegroundColor Gray
Write-Host "- NAVEGAÇÃO CONTRATOS/ADITIVOS: logs de debug do fluxo de navegação" -ForegroundColor Gray
Write-Host "- SignatureCaptureFragment: navegação após envio de contratos" -ForegroundColor Gray
Write-Host "- AditivoSignatureFragment: navegação após envio de aditivos" -ForegroundColor Gray
Write-Host "- ClientDetailFragment: destino da navegação" -ForegroundColor Gray
Write-Host ""

# Padrao de filtro expandido para incluir nossos logs de diagnostico e analise de fluxo
$patternAll = "gestaobilhares|FATAL|AndroidRuntime|crash|Exception|Caused by|FileProvider|Permission Denial|AppRepository|RoutesAdapter|RoutesViewModel|RotaRepository|RoutesFragment|HistoricoMesasVendidasFragment|HistoricoMesasVendidasViewModel|MesasDepositoFragment|VendaMesaDialog|ExpenseRegisterViewModel|HistoricoCombustivelVeiculo|VehicleDetailViewModel|Salvando abastecimento|Abastecimento salvo|Erro ao salvar|Despesa salva|Histórico de veículo|Conversão de data|SettlementFragment|NovaReformaFragment|PanoSelectionDialog|PanoEstoqueRepository|SettlementViewModel|NovaReformaViewModel|PanoEstoqueDao|MesaRepository|MesaDao|Marcando pano|Pano encontrado|Pano marcado como usado|Troca de pano|Seleção de panos|Diálogo de seleção|Mostrando seleção|Pano selecionado|Registrando pano|Continuando salvamento|Salvando reforma|Pano trocado|Pano removido do estoque|Tamanho da mesa|Panos encontrados|Panos disponíveis|Pano confirmado|Diálogo cancelado|Erro ao marcar pano|Erro ao trocar pano|Erro ao atualizar pano|Erro ao carregar panos|Erro ao mostrar diálogo|StockFragment|StockViewModel|StockItemRepository|StockItemDao|AddEditStockItemDialog|StockAdapter|adicionarItemEstoque|Carregando itens do estoque|Itens recebidos do banco|Itens mapeados|Adapter configurado|RecyclerView configurado|Inserindo item|Item inserido com ID|Forçando atualização da lista|Lista atualizada|Itens genéricos recebidos|Grupos de panos recebidos|Item adicionado ao estoque|Erro ao adicionar item|Erro ao carregar itens|MetasFragment|MetasViewModel|MetasAdapter|MetaRotaResumo|MetaColaborador|buscarMetasPorRotaECiclo|criarMetaRotaResumo|calcularProgressoMetas|atualizarValorAtualMeta|carregarMetasRotas|ColaboradorDao|buscarColaboradorResponsavelPrincipal|buscarCicloAtualPorRota|buscarUltimoCicloFinalizadoPorRota|MetaCadastroFragment|MetaCadastroViewModel|carregarCiclosPorRota|carregarRotas|salvarMeta|criarCicloParaRota|buscarProximoNumeroCiclo|inserirCicloAcerto|Ciclos carregados|Ciclos encontrados|Ciclo criado com ID|Ciclo criado com sucesso|Nenhum ciclo encontrado para esta rota|Crie um ciclo primeiro|Erro ao carregar ciclos|Erro ao criar ciclo|Rotas carregadas|Selecionar Rota|Selecionar Ciclo|Nenhum Ciclo Encontrado|Criar Ciclo|Meta salva com sucesso|Erro ao salvar meta|Colaborador responsável encontrado|Nenhum colaborador responsável encontrado|Meta salva com ID|TipoMeta|FATURAMENTO|CLIENTES_ACERTADOS|MESAS_LOCADAS|TICKET_MEDIO|StatusCicloAcerto|EM_ANDAMENTO|FINALIZADO|CANCELADO|SignatureCaptureFragment|AditivoSignatureFragment|ClientDetailFragment|INÍCIO NAVEGAÇÃO|FIM NAVEGAÇÃO|DADOS DO CONTRATO|clienteId|contratoNumero|contratoId|assinaturaContexto|Fragment ativo|isAdded|isDetached|isRemoving|clienteId válido|clienteId inválido|Bundle criado|Tentando navegar|Navegação executada|Erro na navegação|Tentando navegação sem bundle|Navegação sem bundle executada|Fazendo popBackStack|CONTRATO CARREGADO|Aditivo assinado|Erro no ViewModel|ESTADO DO CONTRATO ANTES DO ENVIO|INÍCIO ENVIO|Timestamp|Fragment ativo|ESTADO DO CONTRATO ANTES DO ENVIO|Contrato|Cliente ID|Número|Contexto|INÍCIO ENVIO CONTRATO VIA WHATSAPP|INÍCIO ENVIO ADITIVO VIA WHATSAPP|CONTRATO CARREGADO|ID|Número|Cliente ID|Status|Contrato é null no observer|Aditivo assinado - botão habilitado|Erro no ViewModel"

# Padrao focado apenas no fluxo de venda (tag e mensagens chave)
$patternVenda = "VendaMesaDialog|Clique em Vender|validarCampos|realizarVenda|Transacao concluida|Pos-transacao|Venda concluida|mostrarSeletorMesa|filtrarMesas|mostrarSeletorData|onCreateDialog|onViewCreated|setupClickListeners|setupUI"

# Padrao focado apenas no fluxo de abastecimento
$patternAbastecimento = "ExpenseRegisterViewModel|HistoricoCombustivelVeiculo|VehicleDetailViewModel|Salvando abastecimento|Abastecimento salvo|Erro ao salvar|Despesa salva|Histórico de veículo|Conversão de data|Litros não informados|Abastecimento salvo com ID|Histórico de veículo salvo|Erro ao salvar histórico|isCombustivel|isManutencao|salvarNoHistoricoVeiculo|Carregando histórico|Abastecimentos encontrados|Forçando recarregamento|DEBUG|TOTAL de abastecimentos|Abastecimento ID|Data=|Ano=|Match=|Filtro=|Abastecimento FILTRADO|Carregando TODOS os dados sem filtro|filtradas de|Filtro=TODOS"

# Padrao focado apenas no sistema de panos
$patternPanos = "SettlementFragment|NovaReformaFragment|PanoSelectionDialog|PanoEstoqueRepository|SettlementViewModel|NovaReformaViewModel|PanoEstoqueDao|MesaRepository|MesaDao|Marcando pano|Pano encontrado|Pano marcado como usado|Troca de pano|Seleção de panos|Diálogo de seleção|Mostrando seleção|Pano selecionado|Registrando pano|Continuando salvamento|Salvando reforma|Pano trocado|Pano removido do estoque|Tamanho da mesa|Panos encontrados|Panos disponíveis|Pano confirmado|Diálogo cancelado|Erro ao marcar pano|Erro ao trocar pano|Erro ao atualizar pano|Erro ao carregar panos|Erro ao mostrar diálogo|Criando diálogo|Diálogo criado|Pano confirmado|Pano selecionado|Registrando pano|Continuando salvamento|Salvando reforma|Pano trocado|Pano removido do estoque|Tamanho da mesa|Panos encontrados|Panos disponíveis|Pano confirmado|Diálogo cancelado|Erro ao marcar pano|Erro ao trocar pano|Erro ao atualizar pano|Erro ao carregar panos|Erro ao mostrar diálogo"

# Padrao focado apenas no sistema de estoque
$patternEstoque = "StockFragment|StockViewModel|StockItemRepository|StockItemDao|AddEditStockItemDialog|StockAdapter|adicionarItemEstoque|Carregando itens do estoque|Itens recebidos do banco|Itens mapeados|Adapter configurado|RecyclerView configurado|Inserindo item|Item inserido com ID|Forçando atualização da lista|Lista atualizada|Itens genéricos recebidos|Grupos de panos recebidos|Item adicionado ao estoque|Erro ao adicionar item|Erro ao carregar itens|StockItem|StockAdapter|ItemStock|MaterialCardView"

# Padrao focado apenas no sistema de metas
$patternMetas = "MetasFragment|MetasViewModel|MetasAdapter|MetaRotaResumo|MetaColaborador|buscarMetasPorRotaECiclo|criarMetaRotaResumo|calcularProgressoMetas|atualizarValorAtualMeta|carregarMetasRotas|ColaboradorDao|buscarColaboradorResponsavelPrincipal|buscarCicloAtualPorRota|buscarUltimoCicloFinalizadoPorRota|Iniciando carregamento|Encontradas.*rotas ativas|Processando rota|MetaRota criada|Nenhuma meta encontrada|Colaborador responsável encontrado|Nenhum colaborador responsável|Ciclo encontrado|Nenhum ciclo encontrado|Calculando progresso|Meta atualizada|Faturamento calculado|Clientes acertados calculados|Mesas locadas calculadas|Ticket médio calculado|Progresso calculado|Meta.*atualizada no banco|Erro ao atualizar meta|MetaCadastroFragment|MetaCadastroViewModel|carregarCiclosPorRota|carregarRotas|salvarMeta|criarCicloParaRota|criarCicloFuturoParaRota|buscarProximoNumeroCiclo|inserirCicloAcerto|buscarCiclosParaMetas|Ciclos carregados|Ciclos encontrados|Ciclo criado com ID|Ciclo criado com sucesso|Ciclo futuro criado|Nenhum ciclo encontrado para esta rota|Crie um ciclo primeiro|Erro ao carregar ciclos|Erro ao criar ciclo|Erro ao criar ciclo futuro|Rotas carregadas|Selecionar Rota|Selecionar Ciclo|Nenhum Ciclo Encontrado|Criar Ciclo|Criar Ciclo Atual|Criar Ciclo Futuro|Meta salva com sucesso|Erro ao salvar meta|Colaborador responsável encontrado|Nenhum colaborador responsável encontrado|Meta salva com ID|TipoMeta|FATURAMENTO|CLIENTES_ACERTADOS|MESAS_LOCADAS|TICKET_MEDIO|StatusCicloAcerto|EM_ANDAMENTO|FINALIZADO|CANCELADO|PLANEJADO|CicloAcertoEntity"

# Padrao focado apenas na populacao do banco de dados
$patternPopulation = "DB_POPULATION|INSERINDO ROTA|INSERINDO CLIENTE|INSERINDO ACERTO|INSERINDO MESA|INSERINDO COLABORADOR|INSERINDO DESPESA|INSERINDO CICLO|INSERINDO CONTRATO|INSERINDO ADITIVO|INSERINDO PROCURAÇÃO|LOGIN ONLINE CONCLUÍDO|LOGIN OFFLINE CONCLUÍDO|CRIANDO COLABORADOR ADMIN AUTOMATICAMENTE|CRIANDO COLABORADOR LOCAL A PARTIR DO LOGIN ONLINE|COLABORADOR LOCAL CRIADO VIA SYNC ONLINE"

# Padrao focado apenas na navegação após envio de contratos/aditivos
$patternNavegacao = "SignatureCaptureFragment|AditivoSignatureFragment|ClientDetailFragment|INÍCIO NAVEGAÇÃO|FIM NAVEGAÇÃO|DADOS DO CONTRATO|clienteId|contratoNumero|contratoId|assinaturaContexto|Fragment ativo|isAdded|isDetached|isRemoving|clienteId válido|clienteId inválido|Bundle criado|Tentando navegar|Navegação executada|Erro na navegação|Tentando navegação sem bundle|Navegação sem bundle executada|Fazendo popBackStack|CONTRATO CARREGADO|Aditivo assinado|Erro no ViewModel|ESTADO DO CONTRATO ANTES DO ENVIO|INÍCIO ENVIO|Timestamp|Fragment ativo|ESTADO DO CONTRATO ANTES DO ENVIO|Contrato|Cliente ID|Número|Contexto|INÍCIO ENVIO CONTRATO VIA WHATSAPP|INÍCIO ENVIO ADITIVO VIA WHATSAPP|CONTRATO CARREGADO|ID|Número|Cliente ID|Status|Contrato é null no observer|Aditivo assinado - botão habilitado|Erro no ViewModel"

$pattern = if ($OnlyVenda) { $patternVenda } elseif ($OnlyAbastecimento) { $patternAbastecimento } elseif ($OnlyPanos) { $patternPanos } elseif ($OnlyEstoque) { $patternEstoque } elseif ($OnlyMetas) { $patternMetas } elseif ($OnlyPopulation) { $patternPopulation } elseif ($OnlyNavegacao) { $patternNavegacao } else { $patternAll }

# Opcional: salvar em arquivo
$logDir = Join-Path $PSScriptRoot "logs"
if ($ToFile) {
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $logFile = Join-Path $logDir (if ($OnlyVenda) { "logcat-venda-$timestamp.txt" } elseif ($OnlyAbastecimento) { "logcat-abastecimento-$timestamp.txt" } elseif ($OnlyPanos) { "logcat-panos-$timestamp.txt" } elseif ($OnlyEstoque) { "logcat-estoque-$timestamp.txt" } elseif ($OnlyMetas) { "logcat-metas-$timestamp.txt" } elseif ($OnlyPopulation) { "logcat-population-$timestamp.txt" } elseif ($OnlyNavegacao) { "logcat-navegacao-$timestamp.txt" } else { "logcat-all-$timestamp.txt" })
    Write-Host "Salvando saida tambem em arquivo: $logFile" -ForegroundColor Yellow
}

# Monitorar logcat filtrando apenas erros e crashes
try {
    Write-Host "Monitorando em tempo real..." -ForegroundColor Green
    Write-Host "Filtros: $pattern" -ForegroundColor Gray
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    if ($ToFile) {
        & $adbPath logcat -v time | Tee-Object -Variable raw | Select-String -Pattern $pattern | Tee-Object -FilePath $logFile
    } else {
        & $adbPath logcat -v time | Select-String -Pattern $pattern
    }
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}

Write-Host "" 
Write-Host "Monitoramento finalizado" -ForegroundColor Gray
Read-Host "Pressione Enter para sair" 