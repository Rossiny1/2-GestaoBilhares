# Script para monitorar correção do status do ciclo
# Monitora logs relacionados ao cálculo do ciclo atual e status

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

Write-Host "🔍 Monitorando correção do status do ciclo..."
Write-Host "📱 Filtros: AppRepository, RoutesAdapter, obterCicloAtualRota, calcularCicloAtualReal"
Write-Host "⏹️  Pressione Ctrl+C para parar"
Write-Host ""

# Padrão de busca para logs de correção do ciclo
$pattern = "AppRepository|RoutesAdapter|obterCicloAtualRota|calcularCicloAtualReal|CALCULANDO CICLO ATUAL REAL|Usando ciclo|Ciclo atual encontrado|DEBUG CICLO|TEXTO CICLO|Status.*Finalizado|Status.*Em andamento"

try {
    & $adbPath logcat -c
    & $adbPath logcat | Select-String -Pattern $pattern
} catch {
    Write-Host "❌ Erro ao executar logcat: $($_.Exception.Message)"
}
