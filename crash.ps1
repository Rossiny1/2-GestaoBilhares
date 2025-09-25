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
Write-Host "-ToFile: Salva logs em arquivo" -ForegroundColor Gray
Write-Host ""
Write-Host "EXEMPLOS:" -ForegroundColor Cyan
Write-Host ".\crash.ps1 -OnlyAbastecimento" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyAbastecimento -ToFile" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyPanos" -ForegroundColor Gray
Write-Host ".\crash.ps1 -OnlyPanos -ToFile" -ForegroundColor Gray
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
Write-Host ""

# Padrao de filtro expandido para incluir nossos logs de diagnostico e analise de fluxo
$patternAll = "gestaobilhares|FATAL|AndroidRuntime|crash|Exception|Caused by|FileProvider|Permission Denial|AppRepository|RoutesAdapter|RoutesViewModel|RotaRepository|RoutesFragment|HistoricoMesasVendidasFragment|HistoricoMesasVendidasViewModel|MesasDepositoFragment|VendaMesaDialog|ExpenseRegisterViewModel|HistoricoCombustivelVeiculo|VehicleDetailViewModel|Salvando abastecimento|Abastecimento salvo|Erro ao salvar|Despesa salva|Histórico de veículo|Conversão de data|SettlementFragment|NovaReformaFragment|PanoSelectionDialog|PanoEstoqueRepository|SettlementViewModel|NovaReformaViewModel|PanoEstoqueDao|MesaRepository|MesaDao|Marcando pano|Pano encontrado|Pano marcado como usado|Troca de pano|Seleção de panos|Diálogo de seleção|Mostrando seleção|Pano selecionado|Registrando pano|Continuando salvamento|Salvando reforma|Pano trocado|Pano removido do estoque|Tamanho da mesa|Panos encontrados|Panos disponíveis|Pano confirmado|Diálogo cancelado|Erro ao marcar pano|Erro ao trocar pano|Erro ao atualizar pano|Erro ao carregar panos|Erro ao mostrar diálogo"

# Padrao focado apenas no fluxo de venda (tag e mensagens chave)
$patternVenda = "VendaMesaDialog|Clique em Vender|validarCampos|realizarVenda|Transacao concluida|Pos-transacao|Venda concluida|mostrarSeletorMesa|filtrarMesas|mostrarSeletorData|onCreateDialog|onViewCreated|setupClickListeners|setupUI"

# Padrao focado apenas no fluxo de abastecimento
$patternAbastecimento = "ExpenseRegisterViewModel|HistoricoCombustivelVeiculo|VehicleDetailViewModel|Salvando abastecimento|Abastecimento salvo|Erro ao salvar|Despesa salva|Histórico de veículo|Conversão de data|Litros não informados|Abastecimento salvo com ID|Histórico de veículo salvo|Erro ao salvar histórico|isCombustivel|isManutencao|salvarNoHistoricoVeiculo|Carregando histórico|Abastecimentos encontrados|Forçando recarregamento|DEBUG|TOTAL de abastecimentos|Abastecimento ID|Data=|Ano=|Match=|Filtro=|Abastecimento FILTRADO|Carregando TODOS os dados sem filtro|filtradas de|Filtro=TODOS"

# Padrao focado apenas no sistema de panos
$patternPanos = "SettlementFragment|NovaReformaFragment|PanoSelectionDialog|PanoEstoqueRepository|SettlementViewModel|NovaReformaViewModel|PanoEstoqueDao|MesaRepository|MesaDao|Marcando pano|Pano encontrado|Pano marcado como usado|Troca de pano|Seleção de panos|Diálogo de seleção|Mostrando seleção|Pano selecionado|Registrando pano|Continuando salvamento|Salvando reforma|Pano trocado|Pano removido do estoque|Tamanho da mesa|Panos encontrados|Panos disponíveis|Pano confirmado|Diálogo cancelado|Erro ao marcar pano|Erro ao trocar pano|Erro ao atualizar pano|Erro ao carregar panos|Erro ao mostrar diálogo|Criando diálogo|Diálogo criado|Pano confirmado|Pano selecionado|Registrando pano|Continuando salvamento|Salvando reforma|Pano trocado|Pano removido do estoque|Tamanho da mesa|Panos encontrados|Panos disponíveis|Pano confirmado|Diálogo cancelado|Erro ao marcar pano|Erro ao trocar pano|Erro ao atualizar pano|Erro ao carregar panos|Erro ao mostrar diálogo"

$pattern = if ($OnlyVenda) { $patternVenda } elseif ($OnlyAbastecimento) { $patternAbastecimento } elseif ($OnlyPanos) { $patternPanos } else { $patternAll }

# Opcional: salvar em arquivo
$logDir = Join-Path $PSScriptRoot "logs"
if ($ToFile) {
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $logFile = Join-Path $logDir (if ($OnlyVenda) { "logcat-venda-$timestamp.txt" } elseif ($OnlyAbastecimento) { "logcat-abastecimento-$timestamp.txt" } elseif ($OnlyPanos) { "logcat-panos-$timestamp.txt" } else { "logcat-all-$timestamp.txt" })
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