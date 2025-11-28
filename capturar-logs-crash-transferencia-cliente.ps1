# ========================================
# CAPTURADOR DE LOGS - CRASH TRANSFERENCIA CLIENTE
# ========================================
# Script especifico para capturar crash ao clicar em "Transferir Cliente"

Write-Host "========================================" -ForegroundColor Red
Write-Host "CAPTURADOR DE LOGS - CRASH TRANSFERENCIA CLIENTE" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red
Write-Host ""

# Caminho do ADB
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se ADB existe
if (-not (Test-Path $ADB)) {
    Write-Host "[ERRO] ADB nao encontrado em: $ADB" -ForegroundColor Red
    Write-Host "Verifique o caminho do Android SDK" -ForegroundColor Yellow
    exit 1
}

# Nome do pacote do app
$PACKAGE_NAME = "com.example.gestaobilhares"

# Verificar dispositivos
Write-Host "Verificando dispositivos..." -ForegroundColor Yellow
$devices = & $ADB devices 2>&1
$deviceCount = ($devices | Where-Object { $_ -match "device$" }).Count

if ($deviceCount -eq 0) {
    Write-Host "[ERRO] Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "Conecte o dispositivo via USB e habilite USB Debugging" -ForegroundColor Yellow
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "[OK] $deviceCount dispositivo(s) conectado(s)" -ForegroundColor Green
Write-Host ""

# Limpar logs antigos
Write-Host "Limpando logs antigos..." -ForegroundColor Yellow
& $ADB logcat -c

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "INSTRUCOES:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "1. Abra o app GestaoBilhares no dispositivo" -ForegroundColor White
Write-Host "2. Faca login (se necessario)" -ForegroundColor White
Write-Host "3. Va para a tela de ROTAS" -ForegroundColor White
Write-Host "4. Clique no botao 'Transferir Cliente'" -ForegroundColor White
Write-Host "5. O crash deve ocorrer - os logs serao capturados automaticamente" -ForegroundColor Yellow
Write-Host "6. Pressione Ctrl+C para parar a captura" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MONITORANDO LOGS..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Criar arquivo de log com timestamp
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$logFile = "logcat-crash-transferencia-cliente-$timestamp.txt"

# Tags para filtrar (focando no crash e componentes relacionados)
$tags = @(
    "LOG_CRASH",
    "ClientSelectionDialog",
    "TransferClientDialog",
    "RoutesFragment",
    "ClientSelectionViewModel",
    "TransferClientViewModel",
    "ClientSelectionAdapter",
    "AndroidRuntime",
    "FATAL",
    "System.err",
    "AndroidRuntime:E"
)

# Adicionar tag do pacote separadamente para evitar problema com :
$packageTag = "${PACKAGE_NAME}:*"

# Criar filtro
$tagFilter = ($tags + $packageTag) -join " "

Write-Host "Filtrando logs por:" -ForegroundColor Cyan
foreach ($tag in $tags) {
    Write-Host "  - $tag" -ForegroundColor Gray
}
Write-Host "  - $packageTag" -ForegroundColor Gray
Write-Host ""

# Variaveis para rastrear crashes
$script:crashDetected = $false
$script:crashCount = 0
$script:errorCount = 0

# Funcao para mostrar alerta de crash
function Show-CrashAlert {
    param([string]$message)
    
    $script:crashDetected = $true
    $script:crashCount++
    
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host "                    CRASH DETECTADO!" -ForegroundColor Red
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host "  $message" -ForegroundColor Red
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host ""
}

# Funcao para mostrar alerta de erro
function Show-ErrorAlert {
    param([string]$message)
    
    $script:errorCount++
    
    Write-Host ""
    Write-Host ">>> ERRO: $message" -ForegroundColor Yellow -BackgroundColor DarkRed
    Write-Host ""
}

# Funcao para colorir logs com destaque para crashes
function Write-ColoredLog {
    param([string]$line)
    
    # Detectar crashes fatais
    if ($line -match "FATAL EXCEPTION|AndroidRuntime.*FATAL|Process.*has died|Force finishing") {
        Show-CrashAlert "CRASH FATAL DETECTADO!"
        Write-Host $line -ForegroundColor White -BackgroundColor Red
        return
    }
    
    # Detectar excecoes relacionadas aos componentes de transferencia
    if ($line -match "Exception|RuntimeException|IllegalStateException|NullPointerException|ClassCastException|IllegalArgumentException") {
        if ($line -match "ClientSelectionDialog|TransferClientDialog|ClientSelectionViewModel|TransferClientViewModel") {
            Show-ErrorAlert "Excecao detectada no componente de transferencia"
            Write-Host $line -ForegroundColor Red -BackgroundColor Black
        } elseif ($line -match "LOG_CRASH.*Exception") {
            Show-ErrorAlert "Excecao detectada no codigo"
            Write-Host $line -ForegroundColor Red -BackgroundColor Black
        } else {
            Write-Host $line -ForegroundColor Red
        }
        return
    }
    
    # Detectar erros criticos relacionados
    if ($line -match "ERROR|Error|Erro|Failed|Failure") {
        if ($line -match "ClientSelectionDialog|TransferClientDialog|ClientSelectionViewModel|TransferClientViewModel") {
            Show-ErrorAlert "Erro critico no componente de transferencia"
            Write-Host $line -ForegroundColor Red -BackgroundColor DarkRed
        } elseif ($line -match "LOG_CRASH.*ERRO|LOG_CRASH.*Erro") {
            Show-ErrorAlert "Erro critico detectado"
            Write-Host $line -ForegroundColor Red
        } else {
            Write-Host $line -ForegroundColor Red
        }
        return
    }
    
    # Detectar logs de crash customizados
    if ($line -match "LOG_CRASH.*\[CLIQUE\]|LOG_CRASH.*\[ERRO\]|LOG_CRASH.*\[ERRO FATAL\]") {
        if ($line -match "\[ERRO FATAL\]") {
            Show-CrashAlert "Erro fatal no codigo"
            Write-Host $line -ForegroundColor White -BackgroundColor Red
        } elseif ($line -match "\[ERRO\]") {
            Write-Host $line -ForegroundColor Red
        } else {
            Write-Host $line -ForegroundColor Yellow
        }
        return
    }
    
    # Logs especificos dos componentes de transferencia
    if ($line -match "ClientSelectionDialog.*onCreateDialog|ClientSelectionDialog.*onViewCreated|ClientSelectionDialog.*setup") {
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    if ($line -match "TransferClientDialog.*onCreateDialog|TransferClientDialog.*setup") {
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    if ($line -match "ClientSelectionViewModel.*loadAllClients|ClientSelectionViewModel.*searchClients") {
        Write-Host $line -ForegroundColor Magenta
        return
    }
    
    if ($line -match "RoutesFragment.*showTransferClientDialog|RoutesFragment.*Transfer") {
        Write-Host $line -ForegroundColor Green
        return
    }
    
    # Logs de debug do LOG_CRASH
    if ($line -match "LOG_CRASH") {
        Write-Host $line -ForegroundColor Cyan
        return
    }
    
    # Warnings
    if ($line -match "WARN|Warning") {
        Write-Host $line -ForegroundColor Yellow
        return
    }
    
    # Logs normais
    Write-Host $line -ForegroundColor Gray
}

# Capturar logs em tempo real
Write-Host "Iniciando captura de logs..." -ForegroundColor Green
Write-Host "Execute os passos acima agora!" -ForegroundColor Yellow
Write-Host ""
Write-Host "Logs serao salvos em: $logFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "================================================================" -ForegroundColor DarkGray
Write-Host "MONITORAMENTO ATIVO - Aguardando eventos..." -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor DarkGray
Write-Host ""

# Capturar logs com filtros
try {
    & $ADB logcat -v time -s $tagFilter | ForEach-Object {
        $line = $_
        
        # Processar e exibir log colorido
        Write-ColoredLog $line
        
        # Salvar no arquivo
        Add-Content -Path $logFile -Value $line -ErrorAction SilentlyContinue
        
        # Mostrar resumo a cada 10 logs se houver crashes
        if ($script:crashCount -gt 0 -and ($script:crashCount + $script:errorCount) % 10 -eq 0) {
            Write-Host ""
            Write-Host ">>> RESUMO: $($script:crashCount) crash(es) | $($script:errorCount) erro(s)" -ForegroundColor Magenta
            Write-Host ""
        }
    }
} catch {
    Write-Host ""
    Write-Host "[ERRO] Erro ao capturar logs: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "                    CAPTURA FINALIZADA" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Mostrar resumo final destacado
if ($script:crashDetected) {
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host "                    RESUMO FINAL" -ForegroundColor Red
    Write-Host "================================================================" -ForegroundColor Red
    $crashCountStr = $script:crashCount.ToString().PadLeft(2)
    $errorCountStr = $script:errorCount.ToString().PadLeft(2)
    Write-Host "  CRASHES DETECTADOS: $crashCountStr" -ForegroundColor Red
    Write-Host "  ERROS DETECTADOS:   $errorCountStr" -ForegroundColor Yellow
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host ""
    Write-Host ">>> ATENCAO: Crash(es) detectado(s) durante a execucao!" -ForegroundColor Red -BackgroundColor Black
    Write-Host ""
} else {
    Write-Host ">>> OK: Nenhum crash detectado durante a captura" -ForegroundColor Green
    $errorColor = if ($script:errorCount -gt 0) { "Yellow" } else { "Green" }
    Write-Host "   Erros encontrados: $script:errorCount" -ForegroundColor $errorColor
    Write-Host ""
}

Write-Host "Logs salvos em: $logFile" -ForegroundColor Cyan
Write-Host ""

# Analise detalhada do arquivo
if (Test-Path $logFile) {
    $logContent = Get-Content $logFile -Raw -ErrorAction SilentlyContinue
    if ($logContent) {
        $crashLogs = ($logContent | Select-String -Pattern "FATAL|AndroidRuntime.*FATAL|Exception|CRASH" -AllMatches).Matches.Count
        $errorLogs = ($logContent | Select-String -Pattern "ERROR|Error|Erro" -AllMatches).Matches.Count
        $logCrashLogs = ($logContent | Select-String -Pattern "LOG_CRASH" -AllMatches).Matches.Count
        $clientSelectionLogs = ($logContent | Select-String -Pattern "ClientSelectionDialog|ClientSelectionViewModel" -AllMatches).Matches.Count
        $transferLogs = ($logContent | Select-String -Pattern "TransferClientDialog|TransferClientViewModel" -AllMatches).Matches.Count
        
        Write-Host "================================================================" -ForegroundColor DarkGray
        Write-Host "ANALISE DETALHADA DOS LOGS:" -ForegroundColor Cyan
        Write-Host "================================================================" -ForegroundColor DarkGray
        $crashColor = if ($crashLogs -gt 0) { "Red" } else { "Green" }
        $errorColor = if ($errorLogs -gt 0) { "Yellow" } else { "Green" }
        $logColor = if ($logCrashLogs -gt 0) { "Cyan" } else { "Red" }
        Write-Host "  Logs FATAL/CRASH:        $crashLogs" -ForegroundColor $crashColor
        Write-Host "  Logs de ERRO:            $errorLogs" -ForegroundColor $errorColor
        Write-Host "  Logs LOG_CRASH:          $logCrashLogs" -ForegroundColor $logColor
        Write-Host "  Logs ClientSelection:    $clientSelectionLogs" -ForegroundColor Cyan
        Write-Host "  Logs TransferClient:     $transferLogs" -ForegroundColor Cyan
        Write-Host ""
        
        if ($crashLogs -gt 0) {
            Write-Host "PROXIMOS PASSOS:" -ForegroundColor Yellow
            Write-Host "   1. Abra o arquivo '$logFile'" -ForegroundColor White
            Write-Host "   2. Procure por 'FATAL' ou 'Exception'" -ForegroundColor White
            Write-Host "   3. Verifique o stack trace completo" -ForegroundColor White
            Write-Host "   4. Procure por logs 'ClientSelectionDialog' ou 'TransferClientDialog'" -ForegroundColor White
            Write-Host "   5. Procure por logs 'LOG_CRASH' para ver o fluxo" -ForegroundColor White
            Write-Host ""
        }
    }
}

Write-Host "================================================================" -ForegroundColor DarkGray
Write-Host "Arquivo completo: $logFile" -ForegroundColor White
Write-Host "================================================================" -ForegroundColor DarkGray

