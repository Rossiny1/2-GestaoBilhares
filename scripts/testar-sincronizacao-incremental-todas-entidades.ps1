# ============================================================================
# Script para testar sincronizacao incremental de TODAS as entidades
# Versao completa: Clientes, Acertos, Despesas, Mesas
# ============================================================================
# Este script monitora os logs do Android para validar a sincronizacao incremental
# de todas as entidades implementadas no SyncRepository.
#
# Como usar:
#   1. Conecte o dispositivo Android via USB
#   2. Execute este script no PowerShell
#   3. No app, clique no botao de sincronizacao (icone de sincronizacao na tela de Rotas)
#   4. Observe os resultados em tempo real
# ============================================================================

$ErrorActionPreference = "Stop"

# Configuracoes
$PACKAGE_NAME = "com.example.gestaobilhares"
$ADB_PATH = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se ADB esta disponivel
if (-not (Test-Path $ADB_PATH)) {
    Write-Host "ERRO: ADB nao encontrado em $ADB_PATH" -ForegroundColor Red
    Write-Host "Por favor, ajuste a variavel ADB_PATH no script" -ForegroundColor Yellow
    exit 1
}

# Verificar se dispositivo esta conectado
$devices = & $ADB_PATH devices | Select-String -Pattern "device$"
if ($devices.Count -eq 0) {
    Write-Host "ERRO: Nenhum dispositivo Android conectado" -ForegroundColor Red
    Write-Host "Conecte o dispositivo via USB e habilite a depuracao USB" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TESTE: Sincronizacao Incremental" -ForegroundColor Cyan
Write-Host "  Entidades: Clientes, Acertos, Despesas, Mesas" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Instrucoes:" -ForegroundColor Yellow
Write-Host "  1. Este script esta monitorando os logs em tempo real" -ForegroundColor White
Write-Host "  2. No app, clique no botao de sincronizacao (icone na tela de Rotas)" -ForegroundColor White
Write-Host "  3. Aguarde a sincronizacao concluir" -ForegroundColor White
Write-Host "  4. O script mostrara analise automatica dos resultados" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Ctrl+C para parar o monitoramento" -ForegroundColor Gray
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Estrutura de dados para cada entidade
$script:entidades = @{
    "clientes" = @{
        Name = "Clientes"
        IncrementalCount = 0
        FullSyncCount = 0
        SyncType = ""
        SyncCount = 0
        SkippedCount = 0
        ErrorCount = 0
        TotalDocuments = 0
        DurationMs = 0
        HasError = $false
        ErrorMessages = @()
        SyncStartTime = $null
        SyncEndTime = $null
        LastSyncTimestamp = $null
        IndexError = $false
        Completed = $false
    }
    "acertos" = @{
        Name = "Acertos"
        IncrementalCount = 0
        FullSyncCount = 0
        SyncType = ""
        SyncCount = 0
        SkippedCount = 0
        ErrorCount = 0
        TotalDocuments = 0
        DurationMs = 0
        HasError = $false
        ErrorMessages = @()
        SyncStartTime = $null
        SyncEndTime = $null
        LastSyncTimestamp = $null
        IndexError = $false
        Completed = $false
    }
    "despesas" = @{
        Name = "Despesas"
        IncrementalCount = 0
        FullSyncCount = 0
        SyncType = ""
        SyncCount = 0
        SkippedCount = 0
        ErrorCount = 0
        TotalDocuments = 0
        DurationMs = 0
        HasError = $false
        ErrorMessages = @()
        SyncStartTime = $null
        SyncEndTime = $null
        LastSyncTimestamp = $null
        IndexError = $false
        Completed = $false
    }
    "mesas" = @{
        Name = "Mesas"
        IncrementalCount = 0
        FullSyncCount = 0
        SyncType = ""
        SyncCount = 0
        SkippedCount = 0
        ErrorCount = 0
        TotalDocuments = 0
        DurationMs = 0
        HasError = $false
        ErrorMessages = @()
        SyncStartTime = $null
        SyncEndTime = $null
        LastSyncTimestamp = $null
        IndexError = $false
        Completed = $false
    }
}

# Funcao para detectar qual entidade esta sendo processada
function Get-EntityFromLog {
    param([string]$line)
    
    if ($line -match "pull de clientes|pullClientes|Clientes") { return "clientes" }
    if ($line -match "pull de acertos|pullAcertos|Acertos") { return "acertos" }
    if ($line -match "pull de despesas|pullDespesas|Despesas") { return "despesas" }
    if ($line -match "pull de mesas|pullMesas|Mesas") { return "mesas" }
    
    return $null
}

# Funcao para analisar e destacar logs importantes
function Format-LogLine {
    param([string]$line)
    
    $entity = Get-EntityFromLog -line $line
    if (-not $entity) { return }
    
    $ent = $script:entidades[$entity]
    
    # Detectar inicio de sincronizacao
    if ($line -match "Iniciando pull de|pull de") {
        if (-not $ent.SyncStartTime) {
            $ent.SyncStartTime = Get-Date
        }
        Write-Host "[$($ent.Name)] " -NoNewline -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    # Sincronizacao incremental detectada
    if ($line -match "Sincronizacao INCREMENTAL|Tentando sincronizacao INCREMENTAL") {
        $ent.IncrementalCount++
        $ent.SyncType = "INCREMENTAL"
        Write-Host "[$($ent.Name)] " -NoNewline -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Green
        return
    }
    
    # Primeira sincronizacao completa
    if ($line -match "Primeira sincronizacao|usando metodo COMPLETO") {
        $ent.FullSyncCount++
        $ent.SyncType = "COMPLETA"
        Write-Host "[$($ent.Name)] " -NoNewline -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    # Timestamp da ultima sincronizacao
    if ($line -match "ultima sync:") {
        $ent.LastSyncTimestamp = $line
        Write-Host "[$($ent.Name)] " -NoNewline -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Yellow
        return
    }
    
    # Total de documentos encontrados
    if ($line -match "Total de.*encontrados|Total de.*no Firestore|documentos encontrados") {
        if ($line -match "(\d+)") {
            $ent.TotalDocuments = [int]$matches[1]
        }
        Write-Host "[$($ent.Name)] " -NoNewline -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    # Erros de indice
    if ($line -match "indice nao existe|indice composto|The query requires an index|indice nao encontrado") {
        $ent.IndexError = $true
        $ent.HasError = $true
        $ent.ErrorMessages += "Indice nao existe no Firestore"
        Write-Host "[$($ent.Name)] " -NoNewline -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Red
        return
    }
    
    # Erros gerais
    if ($line -match "ERRO|ERROR|FATAL|Exception|falhou|failed|Erro ao") {
        $ent.ErrorCount++
        $ent.HasError = $true
        if ($line -match "Erro ao.*: (.+)") {
            $ent.ErrorMessages += $matches[1]
        }
        Write-Host "[$($ent.Name)] " -NoNewline -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Red
        return
    }
    
    # Sucesso na sincronizacao
    if ($line -match "Pull.*concluido|concluido:") {
        if (-not $ent.SyncEndTime) {
            $ent.SyncEndTime = Get-Date
            $ent.Completed = $true
        }
        
        # Extrair estatisticas
        if ($line -match "(\d+) sincronizados|(\d+) sincronizadas") {
            if ($matches[1]) {
                $ent.SyncCount = [int]$matches[1]
            } elseif ($matches[2]) {
                $ent.SyncCount = [int]$matches[2]
            }
        }
        if ($line -match "(\d+) pulados|(\d+) ignoradas") {
            if ($matches[1]) {
                $ent.SkippedCount = [int]$matches[1]
            } elseif ($matches[2]) {
                $ent.SkippedCount = [int]$matches[2]
            }
        }
        if ($line -match "(\d+) erros") {
            $ent.ErrorCount = [int]$matches[1]
        }
        if ($line -match "(\d+)\s*ms|Duracao:\s*(\d+)\s*ms") {
            if ($matches[1]) {
                $ent.DurationMs = [int]$matches[1]
            } elseif ($matches[2]) {
                $ent.DurationMs = [int]$matches[2]
            }
        }
        
        Write-Host "[$($ent.Name)] " -NoNewline -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Green
        return
    }
    
    # Logs de progresso importantes
    if ($line -match "SyncRepository|INCREMENTAL|COMPLETA|fallback") {
        Write-Host "[$($ent.Name)] " -NoNewline -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Yellow
        return
    }
}

# Funcao para gerar relatorio de uma entidade
function Show-EntityReport {
    param([string]$entityKey)
    
    $ent = $script:entidades[$entityKey]
    
    if (-not $ent.Completed -and -not $ent.SyncStartTime) {
        return
    }
    
    Write-Host ""
    Write-Host "----------------------------------------" -ForegroundColor Cyan
    Write-Host "  $($ent.Name.ToUpper())" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Cyan
    Write-Host ""
    
    # Determinar tipo de sincronizacao
    $actualSyncType = if ($ent.SyncType -eq "INCREMENTAL") { "INCREMENTAL" } else { "COMPLETA" }
    
    Write-Host "Tipo de Sincronizacao: " -NoNewline
    if ($actualSyncType -eq "INCREMENTAL") {
        Write-Host $actualSyncType -ForegroundColor Green
    } else {
        Write-Host $actualSyncType -ForegroundColor Cyan
    }
    Write-Host ""
    
    # Estatisticas basicas
    Write-Host "Estatisticas:" -ForegroundColor Yellow
    Write-Host "  - Registros sincronizados: " -NoNewline
    $syncColor = if ($ent.SyncCount -gt 0) { "Green" } else { "Red" }
    Write-Host $ent.SyncCount -ForegroundColor $syncColor
    Write-Host "  - Registros pulados: " -NoNewline
    Write-Host $ent.SkippedCount -ForegroundColor Yellow
    Write-Host "  - Total de documentos processados: " -NoNewline
    Write-Host $ent.TotalDocuments -ForegroundColor Cyan
    Write-Host "  - Erros encontrados: " -NoNewline
    $errosColor = if ($ent.ErrorCount -eq 0) { "Green" } else { "Red" }
    Write-Host $ent.ErrorCount -ForegroundColor $errosColor
    Write-Host "  - Duracao: " -NoNewline
    if ($ent.DurationMs -gt 0) {
        $durationSec = [math]::Round($ent.DurationMs / 1000, 2)
        $durationText = '{0} segundos ({1} ms)' -f $durationSec, $ent.DurationMs
        Write-Host $durationText -ForegroundColor Cyan
    } else {
        Write-Host "N/A" -ForegroundColor Gray
    }
    Write-Host ""
    
    # Analise de resultado
    Write-Host "Analise:" -ForegroundColor Yellow
    
    # Verificar se sincronizacao incremental funcionou
    if ($actualSyncType -eq "INCREMENTAL") {
        Write-Host "  [OK] Sincronizacao INCREMENTAL detectada!" -ForegroundColor Green
        
        if ($ent.TotalDocuments -gt 0 -and $ent.TotalDocuments -lt 100) {
            $reducaoMsg = "  [OK] Reducao de dados: Apenas $($ent.TotalDocuments) documentos baixados"
            Write-Host $reducaoMsg -ForegroundColor Green
            Write-Host "     (Esperado para sincronizacao incremental)" -ForegroundColor Gray
        } elseif ($ent.TotalDocuments -eq 0) {
            Write-Host "  [AVISO] Nenhum documento novo/atualizado desde ultima sincronizacao" -ForegroundColor Yellow
            Write-Host "     (Isso e normal se nao houver mudancas)" -ForegroundColor Gray
        } else {
            $muitosMsg = "  [AVISO] Muitos documentos baixados ($($ent.TotalDocuments))"
            Write-Host $muitosMsg -ForegroundColor Yellow
            Write-Host "     (Pode indicar que incremental nao esta funcionando corretamente)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  [INFO] Sincronizacao COMPLETA executada" -ForegroundColor Cyan
        
        if ($ent.FullSyncCount -eq 1 -and $ent.IncrementalCount -eq 0) {
            Write-Host "  [INFO] Primeira sincronizacao ou fallback automatico" -ForegroundColor Cyan
            Write-Host "     (Normal na primeira vez ou se indice nao existe)" -ForegroundColor Gray
        }
    }
    
    # Verificar erros
    if ($ent.HasError) {
        Write-Host ""
        Write-Host "  [ERRO] ERROS DETECTADOS:" -ForegroundColor Red
        foreach ($error in $ent.ErrorMessages) {
            Write-Host "     - $error" -ForegroundColor Red
        }
        
        if ($ent.IndexError) {
            Write-Host ""
            Write-Host "  [SOLUCAO] SOLUCAO PARA ERRO DE INDICE:" -ForegroundColor Yellow
            Write-Host "     1. Acesse: https://console.firebase.google.com" -ForegroundColor White
            Write-Host "     2. Firestore Database -> Indexes -> Create Index" -ForegroundColor White
            Write-Host "     3. Collection: $entityKey" -ForegroundColor White
            Write-Host "     4. Campo: lastModified (Ascending)" -ForegroundColor White
            Write-Host "     5. Clique em 'Create' e aguarde alguns minutos" -ForegroundColor White
        }
    } else {
        Write-Host "  [OK] Nenhum erro critico detectado" -ForegroundColor Green
    }
}

# Funcao para gerar relatorio final consolidado
function Show-FinalReport {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RELATORIO FINAL - ANALISE COMPLETA" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    $totalIncremental = 0
    $totalComplete = 0
    $totalSync = 0
    $totalErrors = 0
    $entitiesWithIncremental = 0
    $entitiesWithErrors = 0
    
    # Gerar relatorio individual de cada entidade
    foreach ($entityKey in $script:entidades.Keys) {
        $ent = $script:entidades[$entityKey]
        
        if ($ent.Completed -or $ent.SyncStartTime) {
            Show-EntityReport -entityKey $entityKey
            
            if ($ent.SyncType -eq "INCREMENTAL") {
                $entitiesWithIncremental++
            }
            if ($ent.SyncType -eq "COMPLETA") {
                $totalComplete++
            }
            $totalIncremental += $ent.IncrementalCount
            $totalSync += $ent.SyncCount
            $totalErrors += $ent.ErrorCount
            if ($ent.HasError) {
                $entitiesWithErrors++
            }
        }
    }
    
    # Resumo consolidado
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RESUMO CONSOLIDADO" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "Entidades com sincronizacao incremental: " -NoNewline
    $incColor = if ($entitiesWithIncremental -gt 0) { "Green" } else { "Yellow" }
    Write-Host "$entitiesWithIncremental de 4" -ForegroundColor $incColor
    
    Write-Host "Entidades com sincronizacao completa: " -NoNewline
    Write-Host "$totalComplete" -ForegroundColor Cyan
    
    Write-Host "Total de registros sincronizados: " -NoNewline
    Write-Host "$totalSync" -ForegroundColor Cyan
    
    Write-Host "Total de erros: " -NoNewline
    $errColor = if ($totalErrors -eq 0) { "Green" } else { "Red" }
    Write-Host "$totalErrors" -ForegroundColor $errColor
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RESULTADO FINAL" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    $isSuccess = $true
    $resultMessage = ""
    
    if ($entitiesWithIncremental -eq 4 -and $totalErrors -eq 0) {
        $resultMessage = "[SUCESSO] TODAS AS ENTIDADES COM SINCRONIZACAO INCREMENTAL FUNCIONANDO!"
        Write-Host $resultMessage -ForegroundColor Green
        Write-Host ""
        Write-Host "Beneficios ativos:" -ForegroundColor Green
        Write-Host "  - Reducao de 95%+ no uso de dados moveis" -ForegroundColor White
        Write-Host "  - Sincronizacao mais rapida" -ForegroundColor White
        Write-Host "  - Menor consumo de bateria" -ForegroundColor White
    } elseif ($entitiesWithIncremental -gt 0 -and $totalErrors -eq 0) {
        $resultMessage = "[SUCESSO PARCIAL] $entitiesWithIncremental de 4 entidades com incremental funcionando"
        Write-Host $resultMessage -ForegroundColor Green
        Write-Host ""
        Write-Host "Proximos passos:" -ForegroundColor Yellow
        Write-Host "  - Verifique as entidades que ainda usam sincronizacao completa" -ForegroundColor White
        Write-Host "  - Pode ser primeira sincronizacao ou falta de indice no Firestore" -ForegroundColor White
    } elseif ($totalErrors -gt 0) {
        $resultMessage = "[ERRO] ERROS DETECTADOS - Verifique os relatorios individuais acima"
        Write-Host $resultMessage -ForegroundColor Red
        $isSuccess = $false
    } else {
        $resultMessage = "[INFO] SINCRONIZACAO COMPLETA EXECUTADA (Normal na primeira vez)"
        Write-Host $resultMessage -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Proximos passos:" -ForegroundColor Yellow
        Write-Host "  - Execute a sincronizacao novamente" -ForegroundColor White
        Write-Host "  - Na proxima vez, deve ser INCREMENTAL" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    return $isSuccess
}

# Filtrar logs relevantes
$filters = @(
    "SyncRepository",
    "pullClientes",
    "pullAcertos",
    "pullDespesas",
    "pullMesas",
    "INCREMENTAL",
    "COMPLETA",
    "Query incremental",
    "sync_metadata",
    "lastSyncTimestamp",
    "Total de",
    "concluido",
    "sincronizados",
    "sincronizadas",
    "pulados",
    "ignoradas",
    "erros",
    "Duracao",
    "fallback",
    "indice"
)

# Capturar logs em tempo real
try {
    & $ADB_PATH logcat -c
    
    Write-Host "Iniciando captura de logs..." -ForegroundColor Green
    Write-Host ""
    
    $logBuffer = @()
    $lastActivityTime = Get-Date
    $syncCompletedCount = 0
    
    & $ADB_PATH logcat -v time SyncRepository:D *:S | ForEach-Object {
        $line = $_
        $currentTime = Get-Date
        
        # Filtrar apenas logs relevantes
        $isRelevant = $false
        foreach ($filter in $filters) {
            if ($line -match $filter) {
                $isRelevant = $true
                break
            }
        }
        
        # Tambem mostrar logs do package
        if ($line -match $PACKAGE_NAME) {
            $isRelevant = $true
        }
        
        if ($isRelevant) {
            Format-LogLine -line $line
            $logBuffer += $line
            $lastActivityTime = $currentTime
            
            # Verificar se todas as entidades foram sincronizadas
            $completedEntities = 0
            foreach ($entityKey in $script:entidades.Keys) {
                if ($script:entidades[$entityKey].Completed) {
                    $completedEntities++
                }
            }
            
            # Se detectou conclusao de todas as sincronizacoes, aguardar um pouco e gerar relatorio
            if ($completedEntities -eq 4 -and $syncCompletedCount -eq 0) {
                $syncCompletedCount = 1
                Start-Sleep -Seconds 3
                Show-FinalReport
                Write-Host ""
                Write-Host "Aguardando proxima sincronizacao..." -ForegroundColor Gray
                Write-Host "Pressione Ctrl+C para sair" -ForegroundColor Gray
                Write-Host ""
                
                # Resetar contadores para proxima sincronizacao
                foreach ($entityKey in $script:entidades.Keys) {
                    $ent = $script:entidades[$entityKey]
                    $ent.SyncType = ""
                    $ent.SyncStartTime = $null
                    $ent.SyncEndTime = $null
                    $ent.Completed = $false
                }
                $syncCompletedCount = 0
            }
        }
    }
} catch {
    # Gerar relatorio final ao sair
    $hasAnySync = $false
    foreach ($entityKey in $script:entidades.Keys) {
        if ($script:entidades[$entityKey].SyncStartTime) {
            $hasAnySync = $true
            break
        }
    }
    
    if ($hasAnySync) {
        Show-FinalReport
    } else {
        Write-Host ""
        Write-Host "Nenhuma sincronizacao detectada." -ForegroundColor Yellow
        Write-Host "Certifique-se de clicar no botao de sincronizacao no app." -ForegroundColor Yellow
    }
}

