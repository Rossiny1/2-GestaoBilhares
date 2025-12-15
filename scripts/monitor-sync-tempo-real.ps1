# MONITOR DE SINCRONIZAÇÃO EM TEMPO REAL
# Baseado em técnicas de observabilidade de sistemas

Write-Host "=== MONITOR DE SINCRONIZACAO EM TEMPO REAL ===" -ForegroundColor Cyan
Write-Host "Monitorando fluxo de sincronizacao de ciclos" -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow
Write-Host ""

# ========== CONFIGURAÇÃO ==========

$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$PACKAGE = "com.example.gestaobilhares"

# ========== INICIALIZAÇÃO ==========

if (!(Test-Path $ADB)) {
    Write-Host "ERRO: ADB nao encontrado" -ForegroundColor Red
    exit 1
}

# ========== MONITORAMENTO ==========

Write-Host "Iniciando monitoramento..." -ForegroundColor Green
Write-Host "Filtros ativos: SyncRepository, RoutesViewModel, ClientListViewModel" -ForegroundColor Gray
Write-Host ""

# Contadores em tempo real
$stats = @{
    CiclosProcessados = 0
    CiclosInseridos = 0
    CiclosAtualizados = 0
    RotasAtualizadas = 0
    CiclosCarregados = 0
    Erros = 0
    SyncIniciada = $false
    SyncConcluida = $false
}

# Timestamp de início
$startTime = Get-Date

# Monitoramento contínuo
& $ADB logcat -v time -s SyncRepository:* RoutesViewModel:* ClientListViewModel:* | ForEach-Object {
    $line = $_
    $currentTime = Get-Date

    # ========== PROCESSAMENTO DE LOGS ==========

    # Sincronização iniciada
    if ($line -match "Iniciando.*sincroniza|pull.*iniciando" -and !$stats.SyncIniciada) {
        Write-Host ""
        Write-Host "[$($currentTime.ToString('HH:mm:ss'))] SINCRONIZACAO INICIADA" -ForegroundColor Green
        $stats.SyncIniciada = $true
    }

    # Ciclos sendo processados
    if ($line -match "Processando ciclo") {
        $stats.CiclosProcessados++
        Write-Host "[$($currentTime.ToString('HH:mm:ss'))] Ciclo processado ($($stats.CiclosProcessados) total)" -ForegroundColor Blue
    }

    # Ciclos inseridos
    if ($line -match "Inserindo novo ciclo") {
        $stats.CiclosInseridos++
        $cicloMatch = $line | Select-String -Pattern "numeroCiclo=(\d+)"
        if ($cicloMatch) {
            $numeroCiclo = $cicloMatch.Matches.Groups[1].Value
            Write-Host "[$($currentTime.ToString('HH:mm:ss'))] Ciclo $numeroCiclo INSERIDO" -ForegroundColor Green
        }
    }

    # Ciclos atualizados
    if ($line -match "Atualizando ciclo") {
        $stats.CiclosAtualizados++
        Write-Host "[$($currentTime.ToString('HH:mm:ss'))] Ciclo atualizado" -ForegroundColor Yellow
    }

    # Rotas atualizadas
    if ($line -match "Rota.*atualizada.*ciclo") {
        $stats.RotasAtualizadas++
        $rotaMatch = $line | Select-String -Pattern "Rota ID=(\d+).*ciclo (\d+)"
        if ($rotaMatch) {
            $rotaId = $rotaMatch.Matches.Groups[1].Value
            $cicloNum = $rotaMatch.Matches.Groups[2].Value
            Write-Host "[$($currentTime.ToString('HH:mm:ss'))] Rota $rotaId -> Ciclo $cicloNum" -ForegroundColor Magenta
        }
    }

    # Ciclos carregados na UI
    if ($line -match "cicloAtivo atualizado") {
        $stats.CiclosCarregados++
        $uiMatch = $line | Select-String -Pattern "numero=(\d+)"
        if ($uiMatch) {
            $cicloUI = $uiMatch.Matches.Groups[1].Value
            Write-Host "[$($currentTime.ToString('HH:mm:ss'))] UI: Ciclo $cicloUI carregado" -ForegroundColor Cyan
        }
    }

    # Ciclo encontrado
    if ($line -match "Ciclo em andamento encontrado") {
        $encontradoMatch = $line | Select-String -Pattern "id=(\d+).*numero=(\d+)"
        if ($encontradoMatch) {
            $id = $encontradoMatch.Matches.Groups[1].Value
            $numero = $encontradoMatch.Matches.Groups[2].Value
            Write-Host "[$($currentTime.ToString('HH:mm:ss'))] ENCONTRADO: Ciclo $numero (ID: $id)" -ForegroundColor Blue
        }
    }

    # Erros
    if ($line -match "ERRO|ERROR|Exception") {
        $stats.Erros++
        Write-Host "[$($currentTime.ToString('HH:mm:ss'))] ERRO DETECTADO" -ForegroundColor Red
    }

    # Sincronização concluída
    if ($line -match "Sincroniza.*conclu.*sucesso|synchronized.*4" -and !$stats.SyncConcluida) {
        $stats.SyncConcluida = $true
        $duration = [math]::Round((($currentTime - $startTime).TotalSeconds), 1)

        Write-Host ""
        Write-Host "[$($currentTime.ToString('HH:mm:ss'))] SINCRONIZACAO CONCLUIDA" -ForegroundColor Green
        Write-Host "Duracao: ${duration}s" -ForegroundColor Gray
    }

    # ========== DASHBOARD EM TEMPO REAL ==========

    # Atualizar dashboard a cada 10 eventos
    $totalEventos = $stats.CiclosProcessados + $stats.CiclosInseridos + $stats.CiclosAtualizados +
                   $stats.RotasAtualizadas + $stats.CiclosCarregados + $stats.Erros

    if ($totalEventos % 5 -eq 0 -and $totalEventos -gt 0) {
        Write-Host ""
        Write-Host "=== DASHBOARD ATUAL ===" -ForegroundColor DarkGray
        Write-Host "Ciclos processados: $($stats.CiclosProcessados)" -ForegroundColor Blue
        Write-Host "Ciclos inseridos: $($stats.CiclosInseridos)" -ForegroundColor Green
        Write-Host "Ciclos atualizados: $($stats.CiclosAtualizados)" -ForegroundColor Yellow
        Write-Host "Rotas atualizadas: $($stats.RotasAtualizadas)" -ForegroundColor Magenta
        Write-Host "Ciclos na UI: $($stats.CiclosCarregados)" -ForegroundColor Cyan
        Write-Host "Erros: $($stats.Erros)" -ForegroundColor $(if ($stats.Erros -eq 0) { "Green" } else { "Red" })
        Write-Host "=======================" -ForegroundColor DarkGray
        Write-Host ""
    }
}

# ========== RELATÓRIO FINAL ==========

$endTime = Get-Date
$totalDuration = [math]::Round((($endTime - $startTime).TotalSeconds), 1)

Write-Host ""
Write-Host "=== RELATORIO FINAL ===" -ForegroundColor Cyan
Write-Host "Monitoramento concluido apos ${totalDuration}s" -ForegroundColor Gray
Write-Host ""
Write-Host "ESTATISTICAS FINAIS:" -ForegroundColor Yellow
Write-Host "==================="
$stats.GetEnumerator() | Sort-Object Name | ForEach-Object {
    $color = switch {
        ($_.Key -match "Erro" -and $_.Value -gt 0) { "Red" }
        ($_.Value -gt 0) { "Green" }
        default { "Gray" }
    }
    Write-Host "   $($_.Key): $($_.Value)" -ForegroundColor $color
}

Write-Host ""
Write-Host "ANALISE DO FLUXO:" -ForegroundColor Yellow
Write-Host "================="

$analise = if ($stats.SyncIniciada -and $stats.SyncConcluida) {
    if ($stats.CiclosInseridos -gt 0 -and $stats.RotasAtualizadas -gt 0 -and $stats.CiclosCarregados -gt 0 -and $stats.Erros -eq 0) {
        "FLUXO COMPLETO FUNCIONANDO" } else { "FLUXO COM PROBLEMAS" }
} elseif ($stats.SyncIniciada -and !$stats.SyncConcluida) {
    "SINCRONIZACAO INICIADA MAS NAO CONCLUIDA"
} else {
    "SINCRONIZACAO NAO DETECTADA"
}

Write-Host "Status: $analise" -ForegroundColor $(if ($analise -match "FUNCIONANDO") { "Green" } else { "Red" })

Write-Host ""
Write-Host "MONITORAMENTO CONCLUIDO" -ForegroundColor Green
