# Script para monitorar carregamento de dados do resumo na tela Gerenciar Ciclo
# Monitora logs relacionados ao cálculo de estatísticas financeiras

$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se ADB existe
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath"
    Write-Host "Verifique se o Android SDK está instalado corretamente"
    exit 1
}

# Verificar se há dispositivos conectados
$devices = & $adbPath devices
if ($devices -match "device$") {
    Write-Host "✅ Dispositivo Android conectado"
} else {
    Write-Host "❌ Nenhum dispositivo Android conectado"
    Write-Host "Conecte um dispositivo ou inicie um emulador"
    exit 1
}

Write-Host "🔍 Monitorando carregamento de dados do resumo na tela Gerenciar Ciclo..."
Write-Host "📱 Filtros: CycleManagementViewModel, FinancialCalculator, calcularEstatisticasFinanceiras"
Write-Host "⏹️  Pressione Ctrl+C para parar"
Write-Host ""

# Padrão de busca para logs de resumo
$pattern = "CycleManagementViewModel|FinancialCalculator|calcularEstatisticasFinanceiras|EstatisticasCiclo|totalRecebido|despesasViagem|subtotal|comissaoMotorista|comissaoIltair|somaPix|somaCartao|somaDespesas|cheques|totalGeral|buscarAcertosPorCiclo|buscarDespesasPorCiclo"

try {
    & $adbPath logcat -c
    & $adbPath logcat | Select-String -Pattern $pattern
} catch {
    Write-Host "❌ Erro ao executar logcat: $($_.Exception.Message)"
}
