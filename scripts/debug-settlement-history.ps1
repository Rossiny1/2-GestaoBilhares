# ========================================
# SCRIPT DE DEBUG DO HISTÓRICO DE ACERTOS - GESTAO BILHARES
# Filtra logs relacionados ao problema do histórico de acertos
# ========================================
#
# INSTRUCOES IMPORTANTES:
# 1. Execute este script em uma NOVA JANELA do PowerShell
# 2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell
# 3. Navegue ate a pasta do projeto: cd "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
# 4. Execute: .\scripts\debug-settlement-history.ps1
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DEBUG DO HISTÓRICO DE ACERTOS - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Execute este script com: .\scripts\debug-settlement-history.ps1" -ForegroundColor Yellow
Write-Host "2. Ou navegue para a pasta scripts: cd scripts" -ForegroundColor Yellow
Write-Host "3. E execute: .\debug-settlement-history.ps1" -ForegroundColor Yellow
Write-Host ""

# Caminho correto do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB está disponível
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK está instalado corretamente" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "ADB encontrado: OK" -ForegroundColor Green

# Verificar se há dispositivos conectados
Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
$devices = & $adbPath devices

Write-Host "Dispositivos encontrados:" -ForegroundColor Yellow
Write-Host $devices -ForegroundColor Gray

# Verificar se há linha com "device" no final
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

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "INSTRUÇÕES DE TESTE:" -ForegroundColor Yellow
Write-Host "1. Vá para um cliente na tela de detalhes" -ForegroundColor White
Write-Host "2. Faça um novo acerto" -ForegroundColor White
Write-Host "3. Volte para a tela de detalhes do cliente" -ForegroundColor White
Write-Host "4. Verifique se o histórico aparece" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Iniciando monitoramento de logs do histórico de acertos..." -ForegroundColor Green
Write-Host ""

Write-Host "LOGS MONITORADOS:" -ForegroundColor Cyan
Write-Host "- ClientDetailFragment: Logs do fragmento de detalhes do cliente" -ForegroundColor Gray
Write-Host "- ClientDetailViewModel: Logs do ViewModel de detalhes do cliente" -ForegroundColor Gray
Write-Host "- SettlementHistoryAdapter: Logs do adapter do histórico" -ForegroundColor Gray
Write-Host "- SettlementViewModel: Logs do ViewModel de acertos" -ForegroundColor Gray
Write-Host "- AppRepository: Logs do repositório principal" -ForegroundColor Gray
Write-Host ""

# Padrão para capturar logs específicos do histórico de acertos
$pattern = "ClientDetailFragment|ClientDetailViewModel|SettlementHistoryAdapter|SettlementViewModel|AppRepository|loadSettlementHistory|obterAcertosPorCliente|settlementHistory|historicoAdapter|submitList|getItemCount|onBindViewHolder|onCreateViewHolder|HISTORICO RECEBIDO|Carregando historico|Acertos encontrados|Adapter inicializado|Lista enviada para o adapter"

Write-Host "Monitorando em tempo real..." -ForegroundColor Green
Write-Host "Filtros: $pattern" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Monitorar logcat filtrando apenas os logs relevantes
try {
    & $adbPath logcat -v time | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}

Write-Host "" 
Write-Host "Monitoramento finalizado" -ForegroundColor Gray
Read-Host "Pressione Enter para sair"
