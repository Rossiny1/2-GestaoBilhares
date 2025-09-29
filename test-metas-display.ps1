# ========================================
# TESTE EXIBIÇÃO DE METAS - GESTAO BILHARES
# ========================================
#
# INSTRUÇÕES:
# 1. Conecte seu dispositivo Android com a depuração USB ativada.
# 2. Execute este script a partir do terminal PowerShell.
# 3. O script irá monitorar os logs relacionados à exibição de metas.
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE EXIBIÇÃO DE METAS - GESTAO BILHARES" -ForegroundColor Cyan
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

Write-Host "Dispositivo conectado. Monitorando logs de exibição de metas..." -ForegroundColor Green

# Limpar logcat anterior
Write-Host "Limpando logcat anterior..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host ""
Write-Host "INSTRUÇÕES PARA O TESTE:" -ForegroundColor Yellow
Write-Host "1. Abra o app no dispositivo" -ForegroundColor White
Write-Host "2. Vá para a tela 'Metas'" -ForegroundColor White
Write-Host "3. Observe se as metas aparecem nos cards das rotas" -ForegroundColor White
Write-Host "4. Verifique se o progresso está sendo exibido corretamente" -ForegroundColor White
Write-Host "5. Teste criar uma nova meta e verificar se aparece no card" -ForegroundColor White
Write-Host ""

Write-Host "MONITORANDO LOGS DE EXIBIÇÃO DE METAS..." -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan

# Padrões de log para monitorar
$patternMetas = @(
    "MetasAdapter",
    "MetaDetalheAdapter", 
    "MetaCadastroFragment",
    "MetaCadastroViewModel",
    "MetasViewModel",
    "refreshMetas",
    "Configurando RecyclerView",
    "metas encontradas",
    "Meta salva com sucesso",
    "MetasViewModel notificado",
    "Forçando refresh das metas",
    "Carregando metas das rotas",
    "MetaRotaResumo criado",
    "Progresso das metas",
    "Faturamento",
    "Clientes Acertados",
    "Mesas Locadas"
)

# Comando para filtrar logs
$filterPattern = $patternMetas -join "|"

try {
    & $adbPath logcat | Where-Object { $_ -match $filterPattern } | ForEach-Object {
        $timestamp = Get-Date -Format "HH:mm:ss"
        Write-Host "[$timestamp] $_" -ForegroundColor Green
    }
} catch {
    Write-Host "Erro ao monitorar logs: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Teste concluído!" -ForegroundColor Cyan
Read-Host "Pressione Enter para sair"
