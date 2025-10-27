# Script para testar sincronizacao de atualizacoes de Cliente e Rota
# Como desenvolvedor Android senior, este script valida se as operacoes UPDATE estao sendo sincronizadas

Write-Host "TESTE DE SINCRONIZACAO - ATUALIZACOES DE CLIENTE E ROTA" -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan

# Verificar se o app esta rodando
Write-Host "`nVerificando se o app esta rodando..." -ForegroundColor Yellow
$appProcess = Get-Process -Name "com.example.gestaobilhares" -ErrorAction SilentlyContinue
if ($appProcess) {
    Write-Host "App esta rodando (PID: $($appProcess.Id))" -ForegroundColor Green
} else {
    Write-Host "App nao esta rodando. Instale e execute o app primeiro." -ForegroundColor Red
    exit 1
}

# Capturar logs de sincronizacao
Write-Host "`nCapturando logs de sincronizacao..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = "logcat-update-cliente-rota-$timestamp.txt"

# Tentar diferentes caminhos do ADB
$adbPaths = @(
    "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "C:\Android\Sdk\platform-tools\adb.exe",
    "C:\Program Files\Android\Android Studio\platform-tools\adb.exe",
    "adb.exe"
)

$adbPath = $null
foreach ($path in $adbPaths) {
    if (Test-Path $path) {
        $adbPath = $path
        break
    }
}

if ($adbPath) {
    Write-Host "ADB encontrado: $adbPath" -ForegroundColor Green
    
    # Capturar logs com filtros especificos para UPDATE
    Write-Host "Capturando logs de operacoes UPDATE..." -ForegroundColor Yellow
    & $adbPath logcat -c  # Limpar logs anteriores
    
    Write-Host "`nINSTRUCOES PARA O TESTE:" -ForegroundColor Cyan
    Write-Host "1. No app, edite o nome de um cliente (ex: Joao -> Joao Alves Dias)" -ForegroundColor White
    Write-Host "2. Edite o nome de uma rota (ex: Noite -> Norte)" -ForegroundColor White
    Write-Host "3. Aguarde alguns segundos para sincronizacao" -ForegroundColor White
    Write-Host "4. Pressione Ctrl+C para parar a captura" -ForegroundColor White
    
    Write-Host "`nCapturando logs... (Pressione Ctrl+C para parar)" -ForegroundColor Yellow
    
    try {
        & $adbPath logcat | Select-String -Pattern "UPDATE|DB_UPDATE|SyncManagerV2|adicionarOperacaoSync|CLIENTE|ROTA" | Tee-Object -FilePath $logFile
    } catch {
        Write-Host "`nCaptura de logs interrompida pelo usuario" -ForegroundColor Green
    }
    
    # Analisar logs capturados
    if (Test-Path $logFile) {
        Write-Host "`nANALISE DOS LOGS CAPTURADOS:" -ForegroundColor Cyan
        Write-Host "=================================" -ForegroundColor Cyan
        
        $logContent = Get-Content $logFile -ErrorAction SilentlyContinue
        if ($logContent) {
            Write-Host "Total de linhas capturadas: $($logContent.Count)" -ForegroundColor White
            
            # Contar operacoes UPDATE por entidade
            $clienteUpdates = ($logContent | Select-String -Pattern "CLIENTE.*UPDATE|Cliente.*UPDATE").Count
            $rotaUpdates = ($logContent | Select-String -Pattern "ROTA.*UPDATE|Rota.*UPDATE").Count
            $colaboradorUpdates = ($logContent | Select-String -Pattern "COLABORADOR.*UPDATE|Colaborador.*UPDATE").Count
            
            Write-Host "`nOPERACOES UPDATE ENCONTRADAS:" -ForegroundColor Yellow
            Write-Host "   Cliente: $clienteUpdates operacoes" -ForegroundColor White
            Write-Host "   Rota: $rotaUpdates operacoes" -ForegroundColor White
            Write-Host "   Colaborador: $colaboradorUpdates operacoes" -ForegroundColor White
            
            # Verificar se ha operacoes na fila de sincronizacao
            $syncQueueOps = ($logContent | Select-String -Pattern "adicionarOperacaoSync.*UPDATE").Count
            Write-Host "`nOPERACOES ADICIONADAS A FILA DE SYNC:" -ForegroundColor Yellow
            Write-Host "   Total: $syncQueueOps operacoes UPDATE" -ForegroundColor White
            
            # Verificar processamento pelo SyncManagerV2
            $syncManagerOps = ($logContent | Select-String -Pattern "SyncManagerV2.*UPDATE").Count
            Write-Host "`nOPERACOES PROCESSADAS PELO SYNC MANAGER:" -ForegroundColor Yellow
            Write-Host "   Total: $syncManagerOps operacoes UPDATE" -ForegroundColor White
            
            # Verificar logs especificos de atualizacao
            $dbUpdateLogs = ($logContent | Select-String -Pattern "DB_UPDATE").Count
            Write-Host "`nLOGS DE ATUALIZACAO NO BANCO:" -ForegroundColor Yellow
            Write-Host "   Total: $dbUpdateLogs logs de UPDATE" -ForegroundColor White
            
            # Resultado do teste
            Write-Host "`nRESULTADO DO TESTE:" -ForegroundColor Cyan
            Write-Host "=====================" -ForegroundColor Cyan
            
            if ($syncQueueOps -gt 0) {
                Write-Host "SUCESSO: Operacoes UPDATE estao sendo adicionadas a fila de sincronizacao!" -ForegroundColor Green
                Write-Host "CORRECAO IMPLEMENTADA: Metodos de atualizacao agora sincronizam dados editados" -ForegroundColor Green
                
                if ($clienteUpdates -gt 0) {
                    Write-Host "Cliente: Atualizacoes sendo sincronizadas" -ForegroundColor Green
                } else {
                    Write-Host "Cliente: Nenhuma atualizacao encontrada - verifique se editou um cliente" -ForegroundColor Yellow
                }
                
                if ($rotaUpdates -gt 0) {
                    Write-Host "Rota: Atualizacoes sendo sincronizadas" -ForegroundColor Green
                } else {
                    Write-Host "Rota: Nenhuma atualizacao encontrada - verifique se editou uma rota" -ForegroundColor Yellow
                }
                
            } else {
                Write-Host "PROBLEMA: Nenhuma operacao UPDATE foi encontrada na fila de sincronizacao" -ForegroundColor Red
                Write-Host "Verifique se os dados foram realmente editados no app" -ForegroundColor Red
            }
            
            if ($syncManagerOps -gt 0) {
                Write-Host "SUCESSO: SyncManagerV2 esta processando operacoes UPDATE!" -ForegroundColor Green
            } else {
                Write-Host "ATENCAO: SyncManagerV2 pode nao estar processando operacoes UPDATE" -ForegroundColor Yellow
                Write-Host "Verifique se a sincronizacao automatica esta ativa" -ForegroundColor Yellow
            }
            
            # Mostrar ultimas linhas dos logs
            Write-Host "`nULTIMAS LINHAS DOS LOGS:" -ForegroundColor Cyan
            Write-Host "===========================" -ForegroundColor Cyan
            $logContent | Select-Object -Last 10 | ForEach-Object {
                Write-Host "   $_" -ForegroundColor White
            }
            
        } else {
            Write-Host "Nenhum log foi capturado" -ForegroundColor Red
        }
        
        Write-Host "`nLog completo salvo em: $logFile" -ForegroundColor Cyan
        
    } else {
        Write-Host "Arquivo de log nao foi criado" -ForegroundColor Red
    }
    
} else {
    Write-Host "ADB nao encontrado em nenhum dos caminhos testados:" -ForegroundColor Red
    foreach ($path in $adbPaths) {
        Write-Host "  - $path" -ForegroundColor Red
    }
    Write-Host "Verifique se o Android SDK esta instalado corretamente" -ForegroundColor Red
}

Write-Host "`nTeste concluido!" -ForegroundColor Cyan
