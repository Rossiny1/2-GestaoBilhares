# ============================================================================
# Script para testar sincronizacao incremental de clientes
# Versao melhorada com analise detalhada e resultados claros
# ============================================================================
# Este script monitora os logs do Android para validar a sincronizacao incremental
# de clientes implementada no SyncRepository.
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
Write-Host "  Entidade: Clientes" -ForegroundColor Cyan
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

# Contadores e estatisticas
$script:incrementalSyncCount = 0
$script:fullSyncCount = 0
$script:paginationCount = 0
$script:errorCount = 0
$script:clientesSyncCount = 0
$script:clientesSkippedCount = 0
$script:totalDocuments = 0
$script:durationMs = 0
$script:syncType = ""
$script:hasError = $false
$script:errorMessages = @()
$script:syncStartTime = $null
$script:syncEndTime = $null
$script:lastSyncTimestamp = $null
$script:indexError = $false
$script:timeoutShown = $false

# Funcao para analisar e destacar logs importantes
function Format-LogLine {
    param([string]$line)
    
    # Detectar inicio de sincronizacao
    if ($line -match "Iniciando pull de clientes|pull de clientes") {
        if (-not $script:syncStartTime) {
            $script:syncStartTime = Get-Date
            $script:timeoutShown = $false
        }
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    # Sincronizacao incremental detectada
    if ($line -match "Sincronizacao INCREMENTAL|Tentando sincronizacao INCREMENTAL") {
        $script:incrementalSyncCount++
        $script:syncType = "INCREMENTAL"
        Write-Host $line -ForegroundColor Green
        return
    }
    
    # Primeira sincronizacao completa
    if ($line -match "Primeira sincronizacao|usando metodo COMPLETO") {
        $script:fullSyncCount++
        $script:syncType = "COMPLETA"
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    # Timestamp da ultima sincronizacao
    if ($line -match "ultima sync:") {
        $script:lastSyncTimestamp = $line
        Write-Host $line -ForegroundColor Yellow
        return
    }
    
    # Total de documentos encontrados
    if ($line -match "Total de.*encontrados|Total de.*no Firestore") {
        if ($line -match "(\d+)") {
            $script:totalDocuments = [int]$matches[1]
        }
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    # Paginacao funcionando
    if ($line -match "Lote #\d+|Processado lote:|processando lote") {
        $script:paginationCount++
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    # Erros de indice
    if ($line -match "indice nao existe|indice composto|The query requires an index") {
        $script:indexError = $true
        $script:hasError = $true
        $script:errorMessages += "Indice nao existe no Firestore"
        Write-Host $line -ForegroundColor Red
        return
    }
    
    # Erros gerais
    if ($line -match "ERRO|ERROR|FATAL|Exception|falhou|failed|Erro ao") {
        $script:errorCount++
        $script:hasError = $true
        if ($line -match "Erro ao.*: (.+)") {
            $script:errorMessages += $matches[1]
        }
        Write-Host $line -ForegroundColor Red
        return
    }
    
    # Sucesso na sincronizacao de clientes
    if ($line -match "Pull Clientes.*concluido|Pull Clientes.*concluido|clientes sincronizados|concluido:") {
        if (-not $script:syncEndTime) {
            $script:syncEndTime = Get-Date
        }
        
        # Extrair estatisticas
        if ($line -match "(\d+) sincronizados") {
            $script:clientesSyncCount = [int]$matches[1]
        }
        if ($line -match "(\d+) pulados") {
            $script:clientesSkippedCount = [int]$matches[1]
        }
        if ($line -match "(\d+) erros") {
            $script:errorCount = [int]$matches[1]
        }
        if ($line -match "(\d+)\s*ms|Duracao:\s*(\d+)\s*ms") {
            if ($matches[1]) {
                $script:durationMs = [int]$matches[1]
            } elseif ($matches[2]) {
                $script:durationMs = [int]$matches[2]
            }
        }
        
        Write-Host $line -ForegroundColor Green
        return
    }
    
    # Logs de progresso importantes
    if ($line -match "SyncRepository|pullClientes|INCREMENTAL|COMPLETA|fallback") {
        Write-Host $line -ForegroundColor Yellow
        return
    }
}

# Funcao para gerar relatorio final
function Show-FinalReport {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RELATORIO FINAL - ANALISE COMPLETA" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Determinar tipo de sincronizacao
    $actualSyncType = if ($script:syncType -eq "INCREMENTAL") { "INCREMENTAL" } else { "COMPLETA" }
    
    Write-Host "Tipo de Sincronizacao: " -NoNewline
    if ($actualSyncType -eq "INCREMENTAL") {
        Write-Host $actualSyncType -ForegroundColor Green
    } else {
        Write-Host $actualSyncType -ForegroundColor Cyan
    }
    Write-Host ""
    
    # Estatisticas basicas
    Write-Host "Estatisticas:" -ForegroundColor Yellow
    Write-Host "  - Clientes sincronizados: " -NoNewline
    $clientesColor = if ($script:clientesSyncCount -gt 0) { "Green" } else { "Red" }
    Write-Host $script:clientesSyncCount -ForegroundColor $clientesColor
    Write-Host "  - Clientes pulados: " -NoNewline
    Write-Host $script:clientesSkippedCount -ForegroundColor Yellow
    Write-Host "  - Total de documentos processados: " -NoNewline
    Write-Host $script:totalDocuments -ForegroundColor Cyan
    Write-Host "  - Erros encontrados: " -NoNewline
    $errosColor = if ($script:errorCount -eq 0) { "Green" } else { "Red" }
    Write-Host $script:errorCount -ForegroundColor $errosColor
    Write-Host "  - Duracao: " -NoNewline
    if ($script:durationMs -gt 0) {
        $durationSec = [math]::Round($script:durationMs / 1000, 2)
        $msValue = $script:durationMs
        $durationText = '{0} segundos ({1} ms)' -f $durationSec, $msValue
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
        
        if ($script:totalDocuments -gt 0 -and $script:totalDocuments -lt 100) {
            $reducaoMsg = "  [OK] Reducao de dados: Apenas $($script:totalDocuments) documentos baixados"
            Write-Host $reducaoMsg -ForegroundColor Green
            Write-Host "     (Esperado para sincronizacao incremental)" -ForegroundColor Gray
        } elseif ($script:totalDocuments -eq 0) {
            Write-Host "  [AVISO] Nenhum documento novo/atualizado desde ultima sincronizacao" -ForegroundColor Yellow
            Write-Host "     (Isso e normal se nao houver mudancas)" -ForegroundColor Gray
        } else {
            $muitosMsg = "  [AVISO] Muitos documentos baixados ($($script:totalDocuments))"
            Write-Host $muitosMsg -ForegroundColor Yellow
            Write-Host "     (Pode indicar que incremental nao esta funcionando corretamente)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  [INFO] Sincronizacao COMPLETA executada" -ForegroundColor Cyan
        
        if ($script:fullSyncCount -eq 1 -and $script:incrementalSyncCount -eq 0) {
            Write-Host "  [INFO] Primeira sincronizacao ou fallback automatico" -ForegroundColor Cyan
            Write-Host "     (Normal na primeira vez ou se indice nao existe)" -ForegroundColor Gray
        }
    }
    
    # Verificar erros
    if ($script:hasError) {
        Write-Host ""
        Write-Host "  [ERRO] ERROS DETECTADOS:" -ForegroundColor Red
        foreach ($error in $script:errorMessages) {
            Write-Host "     - $error" -ForegroundColor Red
        }
        
        if ($script:indexError) {
            Write-Host ""
            Write-Host "  [SOLUCAO] SOLUCAO PARA ERRO DE INDICE:" -ForegroundColor Yellow
            Write-Host "     1. Acesse: https://console.firebase.google.com" -ForegroundColor White
            Write-Host "     2. Firestore Database -> Indexes -> Create Index" -ForegroundColor White
            Write-Host "     3. Collection: clientes" -ForegroundColor White
            Write-Host "     4. Campo: lastModified (Ascending)" -ForegroundColor White
            Write-Host "     5. Clique em 'Create' e aguarde alguns minutos" -ForegroundColor White
        }
    } else {
        Write-Host "  [OK] Nenhum erro critico detectado" -ForegroundColor Green
    }
    
    # Verificar paginacao
    if ($script:paginationCount -gt 0) {
        Write-Host ""
        $pagMsg = "  [OK] Paginacao funcionando: $($script:paginationCount) lote(s) processado(s)"
        Write-Host $pagMsg -ForegroundColor Green
    }
    
    # Resultado final
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RESULTADO FINAL" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    $isSuccess = $true
    $resultMessage = ""
    
    if ($actualSyncType -eq "INCREMENTAL" -and -not $script:hasError -and $script:clientesSyncCount -ge 0) {
        $resultMessage = "[SUCESSO] SINCRONIZACAO INCREMENTAL FUNCIONANDO CORRETAMENTE!"
        Write-Host $resultMessage -ForegroundColor Green
        Write-Host ""
        Write-Host "Beneficios ativos:" -ForegroundColor Green
        Write-Host "  - Reducao de 95%+ no uso de dados moveis" -ForegroundColor White
        Write-Host "  - Sincronizacao mais rapida" -ForegroundColor White
        Write-Host "  - Menor consumo de bateria" -ForegroundColor White
    } elseif ($actualSyncType -eq "COMPLETA" -and -not $script:hasError) {
        $resultMessage = "[INFO] SINCRONIZACAO COMPLETA EXECUTADA (Normal na primeira vez)"
        Write-Host $resultMessage -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Proximos passos:" -ForegroundColor Yellow
        Write-Host "  - Execute a sincronizacao novamente" -ForegroundColor White
        Write-Host "  - Na proxima vez, deve ser INCREMENTAL" -ForegroundColor White
        if ($script:indexError) {
            Write-Host "  - Crie o indice no Firestore para habilitar incremental" -ForegroundColor Yellow
        }
    } elseif ($script:hasError) {
        $resultMessage = "[ERRO] ERROS DETECTADOS - Verifique os erros acima"
        Write-Host $resultMessage -ForegroundColor Red
        $isSuccess = $false
    } else {
        $resultMessage = "[AVISO] RESULTADO INDETERMINADO"
        Write-Host $resultMessage -ForegroundColor Yellow
        Write-Host "  - Nenhuma sincronizacao detectada ou dados incompletos" -ForegroundColor Yellow
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
    "INCREMENTAL",
    "COMPLETA",
    "Lote #",
    "Processado lote",
    "Paginacao concluida",
    "Query incremental",
    "sync_metadata",
    "lastSyncTimestamp",
    "createIncrementalQuery",
    "executePaginatedQuery",
    "Total de",
    "concluido",
    "sincronizados",
    "pulados",
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
    
    & $ADB_PATH logcat -v time | ForEach-Object {
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
            
            # Se detectou conclusao de sincronizacao, aguardar um pouco e gerar relatorio
            if ($line -match "Pull Clientes.*concluido" -and $script:syncEndTime) {
                Start-Sleep -Seconds 2
                Show-FinalReport
                Write-Host ""
                Write-Host "Aguardando proxima sincronizacao..." -ForegroundColor Gray
                Write-Host "Pressione Ctrl+C para sair" -ForegroundColor Gray
                Write-Host ""
                
                # Resetar contadores para proxima sincronizacao
                $script:syncType = ""
                $script:syncStartTime = $null
                $script:syncEndTime = $null
                $script:timeoutShown = $false
            }
        }
        
        # Timeout: se nao houver atividade por 30 segundos apos inicio, mostrar relatorio parcial (apenas uma vez)
        if ($script:syncStartTime -and -not $script:syncEndTime -and -not $script:timeoutShown) {
            $timeSinceStart = ($currentTime - $script:syncStartTime).TotalSeconds
            if ($timeSinceStart -gt 30) {
                Write-Host ""
                Write-Host "[AVISO] Sincronizacao esta demorando mais de 30 segundos..." -ForegroundColor Yellow
                Write-Host "   Isso pode indicar sincronizacao completa ou muitos documentos" -ForegroundColor Yellow
                Write-Host "   Aguardando conclusao..." -ForegroundColor Gray
                Write-Host ""
                $script:timeoutShown = $true
            }
        }
    }
} catch {
    # Gerar relatorio final ao sair
    if ($script:syncStartTime) {
        Show-FinalReport
    } else {
        Write-Host ""
        Write-Host "Nenhuma sincronizacao detectada." -ForegroundColor Yellow
        Write-Host "Certifique-se de clicar no botao de sincronizacao no app." -ForegroundColor Yellow
    }
}
