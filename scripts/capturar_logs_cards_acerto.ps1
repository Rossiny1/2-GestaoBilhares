# Script PowerShell para capturar logs do fluxo de troca de pano - Cards Acerto V14
# Projeto: Gestão de Bilhares (com.example.gestaobilhares)
# Objetivo: Identificar por que cards de Acerto não aparecem em "Reforma de Mesas"

# ========================================================================
# COMO USAR:
# 1. Configure ADBPATH abaixo ou como variável de ambiente
# 2. Execute: .\capturar_logs_cards_acerto.ps1
# 3. Siga as instruções no app durante a captura
# ========================================================================

Write-Host "=== CAPTURA DE LOGS - CARDS ACERTO V14 ===" -ForegroundColor Yellow
Write-Host ""

# Configuração ADBPATH - Tenta configurar automaticamente se não estiver definido
if ($env:ADBPATH -eq $null) {
    Write-Host "ADBPATH não configurado. Tentando configurar automaticamente..." -ForegroundColor Yellow
    
    # Tentar caminho padrão do Android SDK
    $adbPath1 = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $adbPath1) {
        $env:ADBPATH = $adbPath1
        Write-Host "ADBPATH configurado automaticamente: $adbPath1" -ForegroundColor Green
    }
    # Tentar caminho alternativo do projeto
    elseif (Test-Path "c:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe") {
        $env:ADBPATH = "c:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe"
        Write-Host "ADBPATH configurado automaticamente: c:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe" -ForegroundColor Green
    }
    else {
        Write-Host "ERRO: ADB não encontrado nos caminhos padrão!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Configure ADBPATH manualmente:" -ForegroundColor Yellow
        Write-Host '  $env:ADBPATH = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"' -ForegroundColor White
        Write-Host "  ou" -ForegroundColor White
        Write-Host '$env:ADBPATH = "c:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe"' -ForegroundColor White
        Write-Host ""
        Read-Host "Pressione Enter para sair"
        exit 1
    }
}

$ADB = $env:ADBPATH
Write-Host "ADBPATH: $ADB" -ForegroundColor Green
Write-Host ""

# 1. Validar dispositivo conectado
Write-Host "[1/6] Verificando dispositivo conectado..." -ForegroundColor Cyan
$devices = & $ADB devices
if ($devices -match "device$") {
    Write-Host "Dispositivo encontrado! OK" -ForegroundColor Green
} else {
    Write-Host "ERRO: Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo USB ou inicie um emulador." -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Pressione Enter para sair"
    exit 1
}
Write-Host ""

# 2. Limpar logs anteriores
Write-Host "[2/6] Limpando logs anteriores..." -ForegroundColor Cyan
& $ADB logcat -c
Write-Host "Logs limpos: OK" -ForegroundColor Green
Write-Host ""

# 3. Gerar timestamp
Write-Host "[3/6] Gerando timestamp para arquivos..." -ForegroundColor Cyan
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
Write-Host "Timestamp gerado: $timestamp" -ForegroundColor Green
Write-Host ""

# 4. Iniciar captura de logs em tempo real
Write-Host "[4/6] Iniciando captura de logs em tempo real..." -ForegroundColor Cyan
Write-Host ""
Write-Host "=== FILTROS ATIVOS ===" -ForegroundColor Yellow
Write-Host "  - DEBUG_CARDS (logs do fluxo de troca de pano)" -ForegroundColor White
Write-Host "  - Erros do app (BaseViewModel, AppRepository, SettlementViewModel)" -ForegroundColor White
Write-Host "  - Erros gerais (AndroidRuntime)" -ForegroundColor White
Write-Host ""
Write-Host "=== INSTRUÇÕES ===" -ForegroundColor Magenta
Write-Host "Execute os seguintes passos no app:" -ForegroundColor White
Write-Host ""
Write-Host "1. Nova Reforma (baseline):" -ForegroundColor Cyan
Write-Host "   - Abra o app" -ForegroundColor Gray
Write-Host "   - Vá em Mesas > Nova Reforma" -ForegroundColor Gray
Write-Host "   - Selecione uma mesa (ex: M01)" -ForegroundColor Gray
Write-Host "   - Marque 'Panos' e escolha um pano" -ForegroundColor Gray
Write-Host "   - Salve a reforma" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Acerto (problema):" -ForegroundColor Red
Write-Host "   - Vá em Acerto" -ForegroundColor Gray
Write-Host "   - Selecione um cliente" -ForegroundColor Gray
Write-Host "   - Adicione uma mesa (ex: M02)" -ForegroundColor Gray
Write-Host "   - Marque 'Trocar Pano' e informe o pano" -ForegroundColor Gray
Write-Host "   - Salve o acerto" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Verificar resultado:" -ForegroundColor Yellow
Write-Host "   - Abra a tela 'Reforma de Mesas'" -ForegroundColor Gray
Write-Host "   - Verifique se ambos os cards aparecem" -ForegroundColor Gray
Write-Host ""
Write-Host "Pressione Ctrl+C para parar a captura quando terminar." -ForegroundColor White
Write-Host ""
Write-Host "=== CAPTURANDO LOGS (aguarde) ===" -ForegroundColor Green
Write-Host ""

# Capturar logs em tempo real até Ctrl+C
try {
    Write-Host "Tentando capturar logs somente do app..." -ForegroundColor Cyan
    $pid = & $ADB shell pidof -s com.example.gestaobilhares

    if (![string]::IsNullOrWhiteSpace($pid)) {
        Write-Host "PID do app encontrado: $pid" -ForegroundColor Green
        & $ADB logcat -v time --pid=$pid -s DEBUG_CARDS SettlementViewModel BaseViewModel AppRepository AndroidRuntime
    } else {
        Write-Host "PID do app não encontrado. Usando filtro por tags..." -ForegroundColor Yellow
        & $ADB logcat -v time -s DEBUG_CARDS SettlementViewModel BaseViewModel AppRepository AndroidRuntime
    }
}
catch {
    # Captura interrompida pelo usuário (Ctrl+C)
}

Write-Host ""
Write-Host "[5/6] Captura interrompida. Salvando logs em arquivos..." -ForegroundColor Cyan

# 5. Salvar logs em arquivos com timestamp
Write-Host "Salvando DEBUG_CARDS..." -ForegroundColor Yellow
# Capturar todos os logs do buffer e filtrar apenas DEBUG_CARDS
& $ADB logcat -d | Select-String "DEBUG_CARDS" | Out-File -FilePath "logs_debug_cards_$timestamp.txt" -Encoding UTF8

Write-Host "Salvando erros gerais..." -ForegroundColor Yellow
# Capturar todos os logs de erro do buffer
& $ADB logcat -d | Select-String "E/" | Out-File -FilePath "logs_errors_$timestamp.txt" -Encoding UTF8

Write-Host "Salvando dump completo de logs..." -ForegroundColor Yellow
# Salvar dump completo para análise futura se necessário
& $ADB logcat -d | Out-File -FilePath "logs_completos_$timestamp.txt" -Encoding UTF8

Write-Host "Logs salvos:" -ForegroundColor Green
Write-Host "  - logs_debug_cards_$timestamp.txt" -ForegroundColor White
Write-Host "  - logs_errors_$timestamp.txt" -ForegroundColor White
Write-Host "  - logs_completos_$timestamp.txt" -ForegroundColor White
Write-Host ""

# 6. (Opcional) Dump do banco Room
$dump_choice = Read-Host "[6/6] Deseja fazer dump do banco Room? (S/N)"
if ($dump_choice -eq "S" -or $dump_choice -eq "s") {
    Write-Host ""
    Write-Host "Fazendo dump do banco Room..." -ForegroundColor Cyan
    
    Write-Host "Exportando mesas_reformadas..." -ForegroundColor Yellow
    $query1 = "SELECT mesa_id, numero_mesa, observacoes, numero_panos, data_reforma FROM mesas_reformadas ORDER BY data_reforma DESC LIMIT 20;"
    & $ADB shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db `"$query1`"" | Out-File -FilePath "query_mesas_reformadas_$timestamp.txt" -Encoding UTF8
    
    Write-Host "Exportando historico_manutencao_mesa..." -ForegroundColor Yellow
    $query2 = "SELECT mesa_id, numero_mesa, responsavel, descricao, data_manutencao FROM historico_manutencao_mesa ORDER BY data_manutencao DESC LIMIT 20;"
    & $ADB shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db `"$query2`"" | Out-File -FilePath "query_historico_manutencao_$timestamp.txt" -Encoding UTF8
    
    Write-Host "Queries do banco salvas:" -ForegroundColor Green
    Write-Host "  - query_mesas_reformadas_$timestamp.txt" -ForegroundColor White
    Write-Host "  - query_historico_manutencao_$timestamp.txt" -ForegroundColor White
}

Write-Host ""
Write-Host "=== CAPTURA CONCLUÍDA ===" -ForegroundColor Green
Write-Host ""
Write-Host "Arquivos gerados:" -ForegroundColor Yellow
Write-Host "  - logs_debug_cards_$timestamp.txt" -ForegroundColor White
Write-Host "  - logs_errors_$timestamp.txt" -ForegroundColor White
Write-Host "  - logs_completos_$timestamp.txt" -ForegroundColor White
if ($dump_choice -eq "S" -or $dump_choice -eq "s") {
    Write-Host "  - query_mesas_reformadas_$timestamp.txt" -ForegroundColor White
    Write-Host "  - query_historico_manutencao_$timestamp.txt" -ForegroundColor White
}
Write-Host ""
Write-Host "Analise os arquivos para identificar o problema dos cards de Acerto." -ForegroundColor Cyan
Write-Host ""
Read-Host "Pressione Enter para sair"
