# ========================================
# SCRIPT DE TESTE DE ABASTECIMENTO - GESTAO BILHARES
# ========================================
#
# INSTRUÇÕES IMPORTANTES:
# 1. Execute este script em uma NOVA JANELA do PowerShell
# 2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell
# 3. Navegue até a pasta do projeto: cd "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
# 4. Execute: .\scripts\test-abastecimento.ps1
#
# CAMINHO CORRETO DO ADB:
# C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE DE ABASTECIMENTO - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Este script deve ser executado em JANELA SEPARADA" -ForegroundColor Yellow
Write-Host "2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell" -ForegroundColor Yellow
Write-Host "3. Navegue até: cd 'C:\Users\Rossiny\Desktop\2-GestaoBilhares'" -ForegroundColor Yellow
Write-Host "4. Execute: .\scripts\test-abastecimento.ps1" -ForegroundColor Yellow
Write-Host ""

Write-Host "CAMINHO ADB: C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" -ForegroundColor Green
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

Write-Host "Iniciando monitoramento de abastecimento..." -ForegroundColor Green
Write-Host "Agora você pode testar o abastecimento no app" -ForegroundColor Green
Write-Host ""

Write-Host "LOGS MONITORADOS:" -ForegroundColor Cyan
Write-Host "- ExpenseRegisterViewModel: salvamento de despesas e abastecimento" -ForegroundColor Gray
Write-Host "- HistoricoCombustivelVeiculo: logs de debug do abastecimento" -ForegroundColor Gray
Write-Host "- VehicleDetailViewModel: carregamento de dados reais" -ForegroundColor Gray
Write-Host "- Salvando abastecimento, Abastecimento salvo, Erro ao salvar" -ForegroundColor Gray
Write-Host "- Despesa salva, Histórico de veículo, Conversão de data" -ForegroundColor Gray
Write-Host ""

# Padrão focado apenas no fluxo de abastecimento
$patternAbastecimento = "ExpenseRegisterViewModel|HistoricoCombustivelVeiculo|VehicleDetailViewModel|Salvando abastecimento|Abastecimento salvo|Erro ao salvar|Despesa salva|Histórico de veículo|Conversão de data|Litros não informados|Abastecimento salvo com ID|Histórico de veículo salvo|Erro ao salvar histórico|isCombustivel|isManutencao|salvarNoHistoricoVeiculo|Carregando histórico|Abastecimentos encontrados|Forçando recarregamento|DEBUG|TOTAL de abastecimentos|Abastecimento ID|Data=|Ano=|Match=|Filtro=|Abastecimento FILTRADO"

Write-Host "Monitorando em tempo real..." -ForegroundColor Green
Write-Host "Filtros: $patternAbastecimento" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Monitorar logcat filtrando apenas logs de abastecimento
try {
    & $adbPath logcat -v time | Select-String -Pattern $patternAbastecimento
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}

Write-Host "" 
Write-Host "Monitoramento finalizado" -ForegroundColor Gray
Read-Host "Pressione Enter para sair"
