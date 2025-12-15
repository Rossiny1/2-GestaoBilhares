# ========================================
# SCRIPT PARA DEBUG DO SISTEMA DE METAS - CICLOS
# ========================================
#
# INSTRUÇÕES:
# 1. Conecte seu dispositivo Android com a depuração USB ativada.
# 2. Execute este script a partir do terminal PowerShell.
# 3. O script irá monitorar especificamente os logs de busca de ciclos.
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DEBUG SISTEMA DE METAS - CICLOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Caminho para o ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Nome do pacote do aplicativo
$packageName = "com.example.gestaobilhares"

# Verificar se o ADB está disponível
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK está instalado corretamente" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

# Verificar se há um dispositivo conectado
Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
$devices = & $adbPath devices
$hasDevice = $devices | Select-String "device$"

if (-not $hasDevice) {
    Write-Host "ERRO: Nenhum dispositivo Android conectado." -ForegroundColor Red
    Write-Host "Conecte o dispositivo via USB e habilite a depuração." -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Dispositivo conectado. Iniciando debug de ciclos..." -ForegroundColor Green
Write-Host ""

# Limpar logcat anterior
Write-Host "Limpando logcat anterior..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "INSTRUÇÕES PARA O DEBUG:" -ForegroundColor Cyan
Write-Host "1. Abra o app no dispositivo" -ForegroundColor White
Write-Host "2. Vá para a tela 'Metas'" -ForegroundColor White
Write-Host "3. Clique em 'Ver Detalhes' de uma rota" -ForegroundColor White
Write-Host "4. Observe se os ciclos aparecem para seleção" -ForegroundColor White
Write-Host "5. Tente criar uma meta" -ForegroundColor White
Write-Host ""
Write-Host "MONITORANDO LOGS DE CICLOS..." -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Padrão específico para debug de ciclos
$patternCiclos = "MetaCadastroViewModel|Carregando ciclos|Total de ciclos encontrados|Ciclos filtrados para metas|Ciclos encontrados para metas|buscarCiclosPorRota|EM_ANDAMENTO|PLANEJADO|Nenhum ciclo disponível|Erro ao carregar ciclos|Ciclo encontrado|Ciclo criado|Ciclo futuro criado"

# Monitorar logs
try {
    & $adbPath logcat | Where-Object { $_ -match $patternCiclos } | ForEach-Object {
        $timestamp = Get-Date -Format "HH:mm:ss"
        Write-Host "[$timestamp] $_" -ForegroundColor Green
    }
} catch {
    Write-Host "Erro ao monitorar logs: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Debug finalizado." -ForegroundColor Yellow
Read-Host "Pressione Enter para sair"
