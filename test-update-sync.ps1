# Script para testar sincronização de dados editados
# Como desenvolvedor Android sênior, este script valida se as operações UPDATE estão sendo sincronizadas

Write-Host "🧪 TESTE DE SINCRONIZAÇÃO DE DADOS EDITADOS" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Verificar se o app está rodando
Write-Host "`n📱 Verificando se o app está rodando..." -ForegroundColor Yellow
$appProcess = Get-Process -Name "com.example.gestaobilhares" -ErrorAction SilentlyContinue
if ($appProcess) {
    Write-Host "✅ App está rodando (PID: $($appProcess.Id))" -ForegroundColor Green
} else {
    Write-Host "❌ App não está rodando. Instale e execute o app primeiro." -ForegroundColor Red
    exit 1
}

# Capturar logs de sincronização
Write-Host "`n📋 Capturando logs de sincronização..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = "logcat-update-sync-$timestamp.txt"

# Usar o caminho do ADB do projeto
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if (Test-Path $adbPath) {
    Write-Host "✅ ADB encontrado: $adbPath" -ForegroundColor Green
    
    # Capturar logs com filtros específicos para UPDATE
    Write-Host "🔍 Capturando logs de operações UPDATE..." -ForegroundColor Yellow
    & $adbPath logcat -c  # Limpar logs anteriores
    
    Write-Host "`n📝 INSTRUÇÕES PARA O TESTE:" -ForegroundColor Cyan
    Write-Host "1. No app, edite um cliente (nome, telefone, endereço)" -ForegroundColor White
    Write-Host "2. Edite uma mesa (relógio inicial/final, estado)" -ForegroundColor White
    Write-Host "3. Edite um acerto (observações, valores)" -ForegroundColor White
    Write-Host "4. Aguarde alguns segundos para sincronização" -ForegroundColor White
    Write-Host "5. Pressione Ctrl+C para parar a captura" -ForegroundColor White
    
    Write-Host "`n⏳ Capturando logs... (Pressione Ctrl+C para parar)" -ForegroundColor Yellow
    
    try {
        & $adbPath logcat | Select-String -Pattern "UPDATE|DB_UPDATE|SyncManagerV2|adicionarOperacaoSync" | Tee-Object -FilePath $logFile
    } catch {
        Write-Host "`n✅ Captura de logs interrompida pelo usuário" -ForegroundColor Green
    }
    
    # Analisar logs capturados
    if (Test-Path $logFile) {
        Write-Host "`n📊 ANÁLISE DOS LOGS CAPTURADOS:" -ForegroundColor Cyan
        Write-Host "=================================" -ForegroundColor Cyan
        
        $logContent = Get-Content $logFile -ErrorAction SilentlyContinue
        if ($logContent) {
            Write-Host "📄 Total de linhas capturadas: $($logContent.Count)" -ForegroundColor White
            
            # Contar operações UPDATE por entidade
            $clienteUpdates = ($logContent | Select-String -Pattern "CLIENTE.*UPDATE|Cliente.*UPDATE").Count
            $mesaUpdates = ($logContent | Select-String -Pattern "MESA.*UPDATE|Mesa.*UPDATE").Count
            $acertoUpdates = ($logContent | Select-String -Pattern "ACERTO.*UPDATE|Acerto.*UPDATE").Count
            
            Write-Host "`n📈 OPERAÇÕES UPDATE ENCONTRADAS:" -ForegroundColor Yellow
            Write-Host "   Cliente: $clienteUpdates operações" -ForegroundColor White
            Write-Host "   Mesa: $mesaUpdates operações" -ForegroundColor White
            Write-Host "   Acerto: $acertoUpdates operações" -ForegroundColor White
            
            # Verificar se há operações na fila de sincronização
            $syncQueueOps = ($logContent | Select-String -Pattern "adicionarOperacaoSync.*UPDATE").Count
            Write-Host "`n🔄 OPERAÇÕES ADICIONADAS À FILA DE SYNC:" -ForegroundColor Yellow
            Write-Host "   Total: $syncQueueOps operações UPDATE" -ForegroundColor White
            
            # Verificar processamento pelo SyncManagerV2
            $syncManagerOps = ($logContent | Select-String -Pattern "SyncManagerV2.*UPDATE").Count
            Write-Host "`n⚙️ OPERAÇÕES PROCESSADAS PELO SYNC MANAGER:" -ForegroundColor Yellow
            Write-Host "   Total: $syncManagerOps operações UPDATE" -ForegroundColor White
            
            # Resultado do teste
            Write-Host "`n🎯 RESULTADO DO TESTE:" -ForegroundColor Cyan
            Write-Host "=====================" -ForegroundColor Cyan
            
            if ($syncQueueOps -gt 0) {
                Write-Host "✅ SUCESSO: Operações UPDATE estão sendo adicionadas à fila de sincronização!" -ForegroundColor Green
                Write-Host "✅ CORREÇÃO IMPLEMENTADA: Métodos de atualização agora sincronizam dados editados" -ForegroundColor Green
            } else {
                Write-Host "❌ PROBLEMA: Nenhuma operação UPDATE foi encontrada na fila de sincronização" -ForegroundColor Red
                Write-Host "❌ Verifique se os dados foram realmente editados no app" -ForegroundColor Red
            }
            
            if ($syncManagerOps -gt 0) {
                Write-Host "✅ SUCESSO: SyncManagerV2 está processando operações UPDATE!" -ForegroundColor Green
            } else {
                Write-Host "⚠️ ATENÇÃO: SyncManagerV2 pode não estar processando operações UPDATE" -ForegroundColor Yellow
                Write-Host "⚠️ Verifique se a sincronização automática está ativa" -ForegroundColor Yellow
            }
            
            # Mostrar últimas linhas dos logs
            Write-Host "`n📋 ÚLTIMAS LINHAS DOS LOGS:" -ForegroundColor Cyan
            Write-Host "===========================" -ForegroundColor Cyan
            $logContent | Select-Object -Last 10 | ForEach-Object {
                Write-Host "   $_" -ForegroundColor White
            }
            
        } else {
            Write-Host "❌ Nenhum log foi capturado" -ForegroundColor Red
        }
        
        Write-Host "`n📁 Log completo salvo em: $logFile" -ForegroundColor Cyan
        
    } else {
        Write-Host "❌ Arquivo de log não foi criado" -ForegroundColor Red
    }
    
} else {
    Write-Host "❌ ADB não encontrado em: $adbPath" -ForegroundColor Red
    Write-Host "❌ Verifique se o Android SDK está instalado corretamente" -ForegroundColor Red
}

Write-Host "`n🏁 Teste concluído!" -ForegroundColor Cyan
