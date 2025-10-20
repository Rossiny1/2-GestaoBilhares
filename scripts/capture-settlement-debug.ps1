# ========================================
# SCRIPT DE CAPTURA DE LOGS DO HISTÓRICO DE ACERTOS - GESTAO BILHARES
# Captura logs específicos e salva em arquivo para análise
# ========================================
#
# INSTRUCOES IMPORTANTES:
# 1. Execute este script em uma NOVA JANELA do PowerShell
# 2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell
# 3. Navegue ate a pasta do projeto: cd "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
# 4. Execute: .\scripts\capture-settlement-debug.ps1
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE LOGS DO HISTÓRICO DE ACERTOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Execute este script com: .\scripts\capture-settlement-debug.ps1" -ForegroundColor Yellow
Write-Host "2. Ou navegue para a pasta scripts: cd scripts" -ForegroundColor Yellow
Write-Host "3. E execute: .\capture-settlement-debug.ps1" -ForegroundColor Yellow
Write-Host ""

# Caminho correto do ADB (mesmo do script crash.ps1)
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB está disponível
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK está instalado corretamente" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "ADB encontrado: OK" -ForegroundColor Green

# Verificar se há dispositivos conectados
Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
$devices = & $adbPath devices

# Verificar se há linha com "device" no final
$hasDevice = $devices | Select-String "device$"

if (-not $hasDevice) {
    Write-Host "ERRO: Nenhum dispositivo Android conectado" -ForegroundColor Red
    Write-Host "Conecte o dispositivo via USB e habilite a depuração" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Dispositivo conectado: OK" -ForegroundColor Green
Write-Host ""

# Criar diretório de logs se não existir
$logDir = "logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
    Write-Host "Diretório de logs criado: $logDir" -ForegroundColor Green
}

# Nome do arquivo de log com timestamp
$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$logFile = "$logDir\settlement-history-debug_$timestamp.log"

Write-Host "Arquivo de log: $logFile" -ForegroundColor Yellow
Write-Host ""

# Limpar logcat anterior
Write-Host "Limpando logcat anterior..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "INSTRUÇÕES DE TESTE:" -ForegroundColor Yellow
Write-Host "1. Vá para um cliente na tela de detalhes" -ForegroundColor White
Write-Host "2. Faça um novo acerto" -ForegroundColor White
Write-Host "3. Volte para a tela de detalhes do cliente" -ForegroundColor White
Write-Host "4. Verifique se o histórico aparece" -ForegroundColor White
Write-Host "5. Pressione Ctrl+C para parar a captura" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Iniciando captura de logs..." -ForegroundColor Green
Write-Host "Logs serão salvos em: $logFile" -ForegroundColor Green
Write-Host ""

# Padrão para capturar logs específicos do histórico de acertos
$pattern = "ClientDetailFragment|ClientDetailViewModel|SettlementHistoryAdapter|SettlementViewModel|AppRepository|loadSettlementHistory|obterAcertosPorCliente|settlementHistory|historicoAdapter|submitList|getItemCount|onBindViewHolder|onCreateViewHolder|HISTORICO RECEBIDO|Carregando historico|Acertos encontrados|Adapter inicializado|Lista enviada para o adapter|RecyclerViews configurados"

# Capturar logs e salvar em arquivo
try {
    Write-Host "Capturando logs... (Pressione Ctrl+C para parar)" -ForegroundColor Green
    & $adbPath logcat -v time | Select-String -Pattern $pattern | Tee-Object -FilePath $logFile
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}

Write-Host "" 
Write-Host "Captura finalizada" -ForegroundColor Gray
Write-Host "Logs salvos em: $logFile" -ForegroundColor Green
Write-Host ""

# Mostrar resumo do arquivo
if (Test-Path $logFile) {
    $lineCount = (Get-Content $logFile | Measure-Object -Line).Lines
    Write-Host "Total de linhas capturadas: $lineCount" -ForegroundColor Yellow
    
    if ($lineCount -gt 0) {
        Write-Host "Primeiras 10 linhas do log:" -ForegroundColor Cyan
        Get-Content $logFile | Select-Object -First 10 | ForEach-Object { Write-Host $_ -ForegroundColor Gray }
        
        if ($lineCount -gt 10) {
            Write-Host "..." -ForegroundColor Gray
            Write-Host "Últimas 5 linhas do log:" -ForegroundColor Cyan
            Get-Content $logFile | Select-Object -Last 5 | ForEach-Object { Write-Host $_ -ForegroundColor Gray }
        }
    }
}

Read-Host "Pressione Enter para sair"
