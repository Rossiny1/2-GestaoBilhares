# Script para testar carregamento de metas
# Executa o app e monitora logs relacionados a metas

Write-Host "🔍 TESTE DE CARREGAMENTO DE METAS" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

# Parâmetros do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o dispositivo está conectado
Write-Host "📱 Verificando dispositivo Android..." -ForegroundColor Yellow
$devices = & $adbPath devices
if ($devices -match "device$") {
    Write-Host "✅ Dispositivo conectado" -ForegroundColor Green
} else {
    Write-Host "❌ Nenhum dispositivo encontrado" -ForegroundColor Red
    exit 1
}

# Limpar logs anteriores
Write-Host "🧹 Limpando logs anteriores..." -ForegroundColor Yellow
& $adbPath logcat -c

# Iniciar monitoramento de logs específicos para metas
Write-Host "📊 Iniciando monitoramento de logs de metas..." -ForegroundColor Yellow
Write-Host "🔍 Filtros aplicados: MetasViewModel, AppRepository, ColaboradorDao" -ForegroundColor Cyan
Write-Host "📝 Logs serão salvos em: logcat-metas-test.txt" -ForegroundColor Cyan
Write-Host ""
Write-Host "🚀 INSTRUÇÕES:" -ForegroundColor Green
Write-Host "1. Abra o app GestaoBilhares" -ForegroundColor White
Write-Host "2. Navegue para: Menu Principal > Metas" -ForegroundColor White
Write-Host "3. Observe se as metas aparecem na tela" -ForegroundColor White
Write-Host "4. Pressione Ctrl+C para parar o monitoramento" -ForegroundColor White
Write-Host ""

# Monitorar logs com filtros específicos para metas
$filters = @(
    "MetasViewModel",
    "AppRepository", 
    "ColaboradorDao",
    "MetaRotaResumo",
    "MetaColaborador",
    "buscarMetasPorRotaECiclo",
    "criarMetaRotaResumo",
    "calcularProgressoMetas",
    "atualizarValorAtualMeta"
)

$filterPattern = $filters -join "|"

try {
    & $adbPath logcat | Where-Object { $_ -match $filterPattern } | Tee-Object -FilePath "logcat-metas-test.txt"
} catch {
    Write-Host "❌ Erro ao monitorar logs: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "📋 Análise dos logs:" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan

if (Test-Path "logcat-metas-test.txt") {
    $logContent = Get-Content "logcat-metas-test.txt" -Raw
    
    # Análise dos logs
    $totalLogs = ($logContent -split "`n").Count
    $errorLogs = ($logContent -split "`n" | Where-Object { $_ -match "ERROR|❌" }).Count
    $warningLogs = ($logContent -split "`n" | Where-Object { $_ -match "WARN|⚠️" }).Count
    $successLogs = ($logContent -split "`n" | Where-Object { $_ -match "SUCCESS|✅" }).Count
    
    Write-Host "📊 Estatísticas dos logs:" -ForegroundColor Yellow
    Write-Host "   Total de logs: $totalLogs" -ForegroundColor White
    Write-Host "   Erros: $errorLogs" -ForegroundColor Red
    Write-Host "   Avisos: $warningLogs" -ForegroundColor Yellow
    Write-Host "   Sucessos: $successLogs" -ForegroundColor Green
    
    # Verificar problemas específicos
    if ($logContent -match "Nenhuma meta encontrada") {
        Write-Host "⚠️ PROBLEMA: Nenhuma meta encontrada no banco de dados" -ForegroundColor Yellow
    }
    
    if ($logContent -match "Nenhum colaborador responsável encontrado") {
        Write-Host "⚠️ PROBLEMA: Colaborador responsável não encontrado" -ForegroundColor Yellow
    }
    
    if ($logContent -match "Nenhum ciclo encontrado") {
        Write-Host "⚠️ PROBLEMA: Nenhum ciclo encontrado para a rota" -ForegroundColor Yellow
    }
    
    if ($logContent -match "MetaRotaResumo criado com sucesso") {
        Write-Host "✅ SUCESSO: MetaRotaResumo criado corretamente" -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "📄 Logs salvos em: logcat-metas-test.txt" -ForegroundColor Cyan
} else {
    Write-Host "❌ Arquivo de log não encontrado" -ForegroundColor Red
}
