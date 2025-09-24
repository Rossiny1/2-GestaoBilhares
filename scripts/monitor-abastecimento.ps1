# ========================================
# SCRIPT DE MONITORAMENTO DE ABASTECIMENTO - GESTAO BILHARES
# ========================================
#
# INSTRUÇÕES IMPORTANTES:
# 1. Execute este script em uma NOVA JANELA do PowerShell
# 2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell
# 3. Navegue até a pasta do projeto: cd "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
# 4. Execute: .\scripts\monitor-abastecimento.ps1
#
# CAMINHO CORRETO DO ADB:
# C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MONITORAMENTO DE ABASTECIMENTO - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

param(
    [switch]$ToFile         # Salva a saída em arquivo (além de mostrar na tela)
)

Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Este script deve ser executado em JANELA SEPARADA" -ForegroundColor Yellow
Write-Host "2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell" -ForegroundColor Yellow
Write-Host "3. Navegue até: cd 'C:\Users\Rossiny\Desktop\2-GestaoBilhares'" -ForegroundColor Yellow
Write-Host "4. Execute: .\scripts\monitor-abastecimento.ps1" -ForegroundColor Yellow
Write-Host ""
Write-Host "PARAMETROS DISPONIVEIS:" -ForegroundColor Cyan
Write-Host "-ToFile: Salva logs em arquivo" -ForegroundColor Gray
Write-Host ""
Write-Host "EXEMPLOS:" -ForegroundColor Cyan
Write-Host ".\scripts\monitor-abastecimento.ps1" -ForegroundColor Gray
Write-Host ".\scripts\monitor-abastecimento.ps1 -ToFile" -ForegroundColor Gray
Write-Host ""

Write-Host "CAMINHO ADB: C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" -ForegroundColor Green
Write-Host ""
Write-Host "MONITORANDO ABASTECIMENTO..." -ForegroundColor Red
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

Write-Host "Iniciando monitoramento de abastecimento..." -ForegroundColor Green
Write-Host "Agora você pode testar o salvamento de abastecimento no app" -ForegroundColor Green
Write-Host ""
Write-Host "LOGS MONITORADOS:" -ForegroundColor Cyan
Write-Host "- ExpenseRegisterViewModel: salvamento de despesas" -ForegroundColor Gray
Write-Host "- HistoricoCombustivelVeiculo: logs de debug do abastecimento" -ForegroundColor Gray
Write-Host "- VehicleDetailViewModel: carregamento de dados reais" -ForegroundColor Gray
Write-Host "- Conversão de data: logs de erro na conversão" -ForegroundColor Gray
Write-Host "- Salvamento: logs de sucesso e erro" -ForegroundColor Gray
Write-Host ""

# Padrao focado apenas no fluxo de abastecimento
$patternAbastecimento = "ExpenseRegisterViewModel|HistoricoCombustivelVeiculo|VehicleDetailViewModel|Salvando abastecimento|Abastecimento salvo|Erro ao salvar|Despesa salva|Histórico de veículo|Conversão de data|Litros não informados|Abastecimento salvo com ID|Histórico de veículo salvo|Erro ao salvar histórico|isCombustivel|isManutencao|salvarNoHistoricoVeiculo|FATAL|AndroidRuntime|crash|Exception|Caused by"

# Opcional: salvar em arquivo
$logDir = Join-Path $PSScriptRoot "logs"
if ($ToFile) {
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $logFile = Join-Path $logDir "logcat-abastecimento-$timestamp.txt"
    Write-Host "Salvando saida tambem em arquivo: $logFile" -ForegroundColor Yellow
}

# Monitorar logcat filtrando apenas logs de abastecimento
try {
    Write-Host "Monitorando em tempo real..." -ForegroundColor Green
    Write-Host "Filtros: $patternAbastecimento" -ForegroundColor Gray
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    if ($ToFile) {
        & $adbPath logcat -v time | Tee-Object -Variable raw | Select-String -Pattern $patternAbastecimento | Tee-Object -FilePath $logFile
    } else {
        & $adbPath logcat -v time | Select-String -Pattern $patternAbastecimento
    }
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}

Write-Host "" 
Write-Host "Monitoramento finalizado" -ForegroundColor Gray
Read-Host "Pressione Enter para sair"
