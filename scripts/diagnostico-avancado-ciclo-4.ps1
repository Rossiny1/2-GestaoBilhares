# DIAGNÓSTICO AVANÇADO - CICLO 4
# Baseado em melhores práticas de debugging de sincronização

Write-Host "=== DIAGNÓSTICO AVANÇADO - CICLO 4 ===" -ForegroundColor Yellow
Write-Host "Analisando problema de sincronização baseado em melhores práticas" -ForegroundColor Cyan
Write-Host "Data/Hora: $(Get-Date)" -ForegroundColor Gray
Write-Host ""

# ========== CONFIGURAÇÃO ==========

$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$PACKAGE = "com.example.gestaobilhares"

# ========== VERIFICAÇÕES PRÉVIAS ==========

Write-Host "1. VERIFICACOES PREVIAS" -ForegroundColor Yellow
Write-Host "======================" -ForegroundColor Yellow

# ADB
if (!(Test-Path $ADB)) {
    Write-Host "ERRO: ADB nao encontrado" -ForegroundColor Red
    exit 1
}
Write-Host "OK: ADB encontrado" -ForegroundColor Green

# Dispositivo
& $ADB devices | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Dispositivo nao conectado" -ForegroundColor Red
    exit 1
}
Write-Host "OK: Dispositivo conectado" -ForegroundColor Green

# ========== LIMPEZA COMPLETA ==========

Write-Host ""
Write-Host "2. LIMPEZA COMPLETA" -ForegroundColor Yellow
Write-Host "==================" -ForegroundColor Yellow

Write-Host "Limpando dados do app..." -ForegroundColor Yellow
& $ADB shell pm clear $PACKAGE | Out-Null

Write-Host "Limpando logs..." -ForegroundColor Yellow
& $ADB logcat -c | Out-Null

Write-Host "OK: Limpeza concluida" -ForegroundColor Green

# ========== TESTE CONTROLADO ==========

Write-Host ""
Write-Host "3. TESTE CONTROLADO" -ForegroundColor Yellow
Write-Host "==================" -ForegroundColor Yellow

$logFile = "diagnostico_ciclo_4_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"

Write-Host "Iniciando captura de logs..." -ForegroundColor Cyan
$logJob = Start-Job -ScriptBlock {
    param($adb, $logFile)
    & $adb logcat -v time -s SyncRepository:* RoutesViewModel:* ClientListViewModel:* | Out-File -FilePath $logFile -Encoding UTF8
} -ArgumentList $ADB, $logFile

Start-Sleep -Seconds 2

Write-Host "Executando app..." -ForegroundColor Green
& $ADB shell am start -n "$PACKAGE/.ui.auth.AuthActivity" | Out-Null

Write-Host "Aguardando sincronizacao (20s)..." -ForegroundColor Yellow
for ($i = 20; $i -gt 0; $i--) {
    Write-Host "   $i segundos restantes..." -ForegroundColor Gray
    Start-Sleep -Seconds 1
}

# Parar captura
Stop-Job -Job $logJob -ErrorAction SilentlyContinue
Remove-Job -Job $logJob -ErrorAction SilentlyContinue

# ========== ANÁLISE AUTOMATIZADA ==========

Write-Host ""
Write-Host "4. ANALISE AUTOMATIZADA" -ForegroundColor Yellow
Write-Host "======================" -ForegroundColor Yellow

if (!(Test-Path $logFile)) {
    Write-Host "ERRO: Arquivo de log nao encontrado" -ForegroundColor Red
    exit 1
}

Write-Host "Analisando arquivo: $logFile" -ForegroundColor Cyan

$logs = Get-Content $logFile -ErrorAction SilentlyContinue

# ========== MÉTRICAS ==========

$metricas = @{
    "CiclosProcessados" = ($logs | Select-String -Pattern "Processando ciclo" | Measure-Object).Count
    "CiclosInseridos" = ($logs | Select-String -Pattern "Inserindo novo ciclo" | Measure-Object).Count
    "CiclosAtualizados" = ($logs | Select-String -Pattern "Atualizando ciclo" | Measure-Object).Count
    "CiclosIgnorados" = ($logs | Select-String -Pattern "Preservando ciclo|Mantendo ciclo" | Measure-Object).Count
    "RotasAtualizadas" = ($logs | Select-String -Pattern "Rota.*atualizada.*ciclo" | Measure-Object).Count
    "Erros" = ($logs | Select-String -Pattern "ERRO|ERROR|Exception" | Measure-Object).Count
    "CiclosAtivosEncontrados" = ($logs | Select-String -Pattern "Ciclo em andamento encontrado" | Measure-Object).Count
    "CiclosCarregados" = ($logs | Select-String -Pattern "cicloAtivo atualizado" | Measure-Object).Count
}

Write-Host ""
Write-Host "METRICAS CAPTURADAS:" -ForegroundColor Cyan
Write-Host "==================="
$metricas.GetEnumerator() | ForEach-Object {
    $color = if ($_.Value -gt 0) { "Green" } else { "Gray" }
    Write-Host "   $($_.Key): $($_.Value)" -ForegroundColor $color
}

# ========== ANÁLISE DE CICLOS ==========

Write-Host ""
Write-Host "ANALISE DE CICLOS:" -ForegroundColor Cyan
Write-Host "=================="

# Buscar ciclos no banco
Write-Host ""
Write-Host "Ciclos no banco de dados:" -ForegroundColor Yellow
$dbCiclos = & $ADB shell "run-as $PACKAGE sqlite3 -header -column /data/data/$PACKAGE/databases/gestao_bilhares.db 'SELECT id, numero_ciclo, status FROM ciclos_acerto WHERE rota_id = 1 ORDER BY numero_ciclo;'" 2>$null
if ($dbCiclos) {
    Write-Host $dbCiclos -ForegroundColor White
} else {
    Write-Host "   Nenhum ciclo encontrado no banco" -ForegroundColor Red
}

# ========== DIAGNÓSTICO ==========

Write-Host ""
Write-Host "DIAGNOSTICO:" -ForegroundColor Cyan
Write-Host "==========="

$diagnostico = @{
    "Status" = "DESCONHECIDA"
    "ProblemaIdentificado" = ""
    "SolucaoSugerida" = ""
}

# Análise baseada nos logs
if ($metricas.CiclosProcessados -eq 0) {
    $diagnostico.Status = "CRITICO"
    $diagnostico.ProblemaIdentificado = "Nenhum ciclo foi processado"
    $diagnostico.SolucaoSugerida = "Verificar conexao com Firebase e permissoes"
} elseif ($metricas.Erros -gt 0) {
    $diagnostico.Status = "ERRO"
    $diagnostico.ProblemaIdentificado = "$($metricas.Erros) erros encontrados durante sincronizacao"
    $diagnostico.SolucaoSugerida = "Analisar logs de erro detalhadamente"
} elseif ($metricas.CiclosInseridos -gt 1) {
    $diagnostico.Status = "ALERTA"
    $diagnostico.ProblemaIdentificado = "Multiplos ciclos inseridos - possivel duplicacao"
    $diagnostico.SolucaoSugerida = "Verificar logica de deteccao de ciclos existentes"
} elseif ($metricas.CiclosAtivosEncontrados -eq 0) {
    $diagnostico.Status = "PROBLEMA"
    $diagnostico.ProblemaIdentificado = "Nenhum ciclo ativo encontrado"
    $diagnostico.SolucaoSugerida = "Verificar se existe ciclo EM_ANDAMENTO no Firebase"
} elseif ($metricas.CiclosCarregados -eq 0) {
    $diagnostico.Status = "PROBLEMA"
    $diagnostico.ProblemaIdentificado = "Ciclos encontrados mas nao carregados na UI"
    $diagnostico.SolucaoSugerida = "Verificar ClientListViewModel e queries do banco"
} elseif ($metricas.CiclosProcessados -gt 0 -and $metricas.RotasAtualizadas -gt 0 -and $metricas.CiclosCarregados -gt 0) {
    $diagnostico.Status = "OK"
    $diagnostico.ProblemaIdentificado = "Sincronizacao funcionando corretamente"
    $diagnostico.SolucaoSugerida = "Monitorar para garantir estabilidade"
} else {
    $diagnostico.Status = "INCONSISTENTE"
    $diagnostico.ProblemaIdentificado = "Padrao de sincronizacao inconsistente"
    $diagnostico.SolucaoSugerida = "Revisar logica de merge de ciclos"
}

$colorStatus = switch ($diagnostico.Status) {
    "OK" { "Green" }
    "ALERTA" { "Yellow" }
    "PROBLEMA" { "Magenta" }
    "ERRO" { "Red" }
    "CRITICO" { "Red" }
    default { "Gray" }
}

Write-Host "Status: $($diagnostico.Status)" -ForegroundColor $colorStatus
Write-Host "Problema: $($diagnostico.ProblemaIdentificado)" -ForegroundColor White
Write-Host "Solucao: $($diagnostico.SolucaoSugerida)" -ForegroundColor Cyan

# ========== RECOMENDAÇÕES ==========

Write-Host ""
Write-Host "RECOMENDACOES:" -ForegroundColor Cyan
Write-Host "=============="

if ($diagnostico.Status -ne "OK") {
    Write-Host "1. Execute limpeza completa:" -ForegroundColor White
    Write-Host "   .\limpar-dados-app.ps1" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. Teste sincronizacao completa:" -ForegroundColor White
    Write-Host "   Abra o app > Config > Sincronizar Tudo" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. Verifique dados no Firebase:" -ForegroundColor White
    Write-Host "   Console Firebase > Database > ciclos_acerto" -ForegroundColor Gray
    Write-Host ""
    Write-Host "4. Execute novamente este diagnostico:" -ForegroundColor White
    Write-Host "   .\diagnostico-avancado-ciclo-4.ps1" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Log salvo em: $logFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "DIAGNOSTICO CONCLUIDO" -ForegroundColor Green
