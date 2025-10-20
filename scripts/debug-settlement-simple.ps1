# ========================================
# SCRIPT SIMPLES DE DEBUG DO HISTÓRICO DE ACERTOS
# Baseado no crash.ps1 que funciona
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DEBUG SIMPLES - HISTÓRICO DE ACERTOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Caminho correto do ADB (mesmo do script crash.ps1)
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
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Red
Write-Host ""

# Padrão simplificado (baseado no crash.ps1)
$pattern = "ClientDetailFragment|ClientDetailViewModel|SettlementHistoryAdapter|SettlementViewModel|AppRepository|loadSettlementHistory|obterAcertosPorCliente|settlementHistory|historicoAdapter|submitList|getItemCount|onBindViewHolder|onCreateViewHolder|HISTORICO RECEBIDO|Carregando historico|Acertos encontrados|Adapter inicializado|Lista enviada para o adapter|RecyclerViews configurados"

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
