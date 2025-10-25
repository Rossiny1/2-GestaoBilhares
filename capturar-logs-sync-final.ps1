# Script para Captura de Logs de Sincronização - Gestão Bilhares
# Autor: Assistente IA
# Data: 2025-01-07
# Descrição: Captura logs específicos de sincronização com Firebase

Write-Host "INICIANDO CAPTURA DE LOGS DE SINCRONIZAÇÃO..." -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Cyan

# Parar processos adb anteriores
Write-Host "Parando processos adb anteriores..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -eq "adb"} | Stop-Process -Force -ErrorAction SilentlyContinue

# Aguardar um momento
Start-Sleep -Seconds 2

# Verificar se adb está disponível
Write-Host "Verificando ADB..." -ForegroundColor Yellow
$ADB = "adb"
try {
    $adbCheck = & $ADB version 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ADB global não encontrado, tentando caminho específico..." -ForegroundColor Yellow
        $ADB_PATH = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
        if (Test-Path $ADB_PATH) {
            $ADB = $ADB_PATH
            Write-Host "ADB encontrado em: $ADB" -ForegroundColor Green
        } else {
            Write-Host "ADB não encontrado!" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "ADB encontrado globalmente" -ForegroundColor Green
    }
} catch {
    Write-Host "Erro ao verificar ADB: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Verificar dispositivos
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $ADB devices
$deviceCount = ($devices | Where-Object { $_ -match "device$" }).Count

if ($deviceCount -eq 0) {
    Write-Host "Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo Android ou emulador" -ForegroundColor Yellow
    exit 1
}

Write-Host "$deviceCount dispositivo(s) conectado(s)" -ForegroundColor Green

# Verificar se o app está instalado
Write-Host "Verificando se o app está instalado..." -ForegroundColor Yellow
$installedApps = & $ADB shell pm list packages com.example.gestaobilhares
if ($installedApps -match "com.example.gestaobilhares") {
    Write-Host "App GestaoBilhares encontrado" -ForegroundColor Green
} else {
    Write-Host "App GestaoBilhares não encontrado!" -ForegroundColor Red
    Write-Host "Instale o app primeiro" -ForegroundColor Yellow
    exit 1
}

# Limpar logs anteriores
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c

Write-Host ""
Write-Host "INSTRUÇÕES PARA CAPTURA:" -ForegroundColor Cyan
Write-Host "1. Abra o app GestaoBilhares" -ForegroundColor White
Write-Host "2. Faça login no app" -ForegroundColor White
Write-Host "3. Cadastre um cliente" -ForegroundColor White
Write-Host "4. Clique no botão de sincronização" -ForegroundColor White
Write-Host "5. Aguarde 30 segundos" -ForegroundColor White
Write-Host "6. Pressione Ctrl+C para parar a captura" -ForegroundColor Red
Write-Host ""

# Aguardar um pouco para o usuário executar os passos
Write-Host "Aguardando 10 segundos para você executar os passos..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Informar início da captura
Write-Host "Executando captura de logs de sincronização..." -ForegroundColor Green
Write-Host "Fonte: adb logcat (tags filtradas)" -ForegroundColor Gray
Write-Host ""
Write-Host "=== LOGS DE SINCRONIZAÇÃO CAPTURADOS ===" -ForegroundColor Cyan
Write-Host "Execute os passos acima agora!" -ForegroundColor Yellow
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Red
Write-Host ""

# Gerar nome do arquivo
$OutputFile = "logcat-sync-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

# Executar o comando e salvar em arquivo
Write-Host "Salvando logs em: $OutputFile" -ForegroundColor Green

try {
    # Capturar logs por 30 segundos (sem here-string, evitando erros de parsing)
    $job = Start-Job -ScriptBlock {
        param($adb, $file)
        & $adb logcat -v time -s "SyncManagerV2:V" "RoutesFragment:V" "AppRepository:V" "FirebaseAuth:V" "FirebaseFirestore:V" "com.example.gestaobilhares:V" "*:S" | Tee-Object -FilePath $file
    } -ArgumentList $ADB, $OutputFile
    
    # Aguardar 30 segundos
    Write-Host "Capturando logs por 30 segundos..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
    
    # Parar o job
    Stop-Job $job -Force
    Remove-Job $job -Force
    # Encerrar adb que possa estar rodando em background
    Get-Process | Where-Object { $_.ProcessName -eq "adb" } | Stop-Process -Force -ErrorAction SilentlyContinue
    
    Write-Host "Captura finalizada!" -ForegroundColor Green
    
    # Análise básica do arquivo
    if (Test-Path $OutputFile) {
        $logContent = Get-Content $OutputFile -Raw
        $syncManagerLogs = ($logContent | Select-String -Pattern "SyncManagerV2" -AllMatches).Matches.Count
        $routesFragmentLogs = ($logContent | Select-String -Pattern "RoutesFragment" -AllMatches).Matches.Count
        $firebaseLogs = ($logContent | Select-String -Pattern "Firebase" -AllMatches).Matches.Count
        $errorCount = ($logContent | Select-String -Pattern "ERROR|Exception|Failed" -AllMatches).Matches.Count

        Write-Host ""
        Write-Host "ANÁLISE DOS LOGS CAPTURADOS:" -ForegroundColor Cyan
        Write-Host "   - Logs SyncManagerV2: $syncManagerLogs" -ForegroundColor $(if ($syncManagerLogs -gt 0) { "Green" } else { "Red" })
        Write-Host "   - Logs RoutesFragment: $routesFragmentLogs" -ForegroundColor $(if ($routesFragmentLogs -gt 0) { "Green" } else { "Red" })
        Write-Host "   - Logs Firebase: $firebaseLogs" -ForegroundColor $(if ($firebaseLogs -gt 0) { "Green" } else { "Red" })
        Write-Host "   - Erros encontrados: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })

        # Verificar empresa_id
        $empresaIdLogs = $logContent | Select-String -Pattern "Empresa ID|empresa_id" -AllMatches
        if ($empresaIdLogs) {
            Write-Host "   - Empresa ID: CONFIGURADO" -ForegroundColor Green
        } else {
            Write-Host "   - Empresa ID: NÃO ENCONTRADO" -ForegroundColor Red
        }

        # Verificar fila de sincronização
        $queueLogs = $logContent | Select-String -Pattern "Fila de sincronização|operacoes|PENDING" -AllMatches
        if ($queueLogs) {
            Write-Host "   - Fila de sincronização: ENCONTRADA" -ForegroundColor Green
        } else {
            Write-Host "   - Fila de sincronização: NÃO ENCONTRADA" -ForegroundColor Red
        }

        # Verificar operações Firestore
        $firestoreLogs = $logContent | Select-String -Pattern "Firestore|SET executado|DELETE executado|Firestore Path" -AllMatches
        if ($firestoreLogs) {
            Write-Host "   - Operações Firestore: ENCONTRADAS" -ForegroundColor Green
        } else {
            Write-Host "   - Operações Firestore: NÃO ENCONTRADAS" -ForegroundColor Red
        }

        Write-Host ""
        Write-Host "DIAGNÓSTICO:" -ForegroundColor Cyan
        if ($syncManagerLogs -eq 0) {
            Write-Host "❌ PROBLEMA: SyncManagerV2 não está sendo executado" -ForegroundColor Red
        }
        if ($queueLogs -eq 0) {
            Write-Host "❌ PROBLEMA: Nenhuma operação na fila de sincronização" -ForegroundColor Red
        }
        if ($firestoreLogs -eq 0) {
            Write-Host "❌ PROBLEMA: Operações não estão chegando ao Firestore" -ForegroundColor Red
        }
        if ($syncManagerLogs -gt 0 -and $queueLogs -gt 0 -and $firestoreLogs -gt 0 -and $errorCount -eq 0) {
            Write-Host "✅ SINCRONIZAÇÃO FUNCIONANDO: Todos os componentes estão operando" -ForegroundColor Green
        }
    }
    
} catch {
    Write-Host "Erro durante a captura: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "CAPTURA DE LOGS FINALIZADA!" -ForegroundColor Green
Write-Host "Arquivo gerado: $OutputFile" -ForegroundColor White
Write-Host "Abra o arquivo para análise detalhada" -ForegroundColor White
Write-Host "=============================================" -ForegroundColor Cyan
