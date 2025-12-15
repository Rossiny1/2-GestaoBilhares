# Script para testar a correção da sincronização incremental de ciclos
Write-Host "=== TESTE DA CORREÇÃO - CICLO 4 ===" -ForegroundColor Yellow
Write-Host "Objetivo: Verificar se a correção da sincronização incremental funciona" -ForegroundColor Cyan
Write-Host "Correção aplicada: ensureMostRecentCiclosIncluded()" -ForegroundColor Green
Write-Host ""

# Verificar se o script de captura existe
$scriptPath = ".\capturar-e-analisar-ciclo-4.ps1"
if (!(Test-Path $scriptPath)) {
    Write-Host "ERRO: Script de captura nao encontrado: $scriptPath" -ForegroundColor Red
    exit 1
}

Write-Host "Script de captura encontrado: $scriptPath" -ForegroundColor Green
Write-Host ""

# Verificar se o ADB existe
$adbPath = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (!(Test-Path $adbPath)) {
    Write-Host "ERRO: ADB nao encontrado: $adbPath" -ForegroundColor Red
    exit 1
}

Write-Host "ADB encontrado: $adbPath" -ForegroundColor Green
Write-Host ""

Write-Host "INSTRUCOES PARA TESTE:" -ForegroundColor Yellow
Write-Host "======================" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Execute uma sincronizacao COMPLETA primeiro:" -ForegroundColor White
Write-Host "   - Abra o app" -ForegroundColor White
Write-Host "   - Va para Configuracoes > Sincronizacao" -ForegroundColor White
Write-Host "   - Execute 'Sincronizar Tudo'" -ForegroundColor White
Write-Host ""
Write-Host "2. Verifique nos logs que o ciclo 4 foi importado:" -ForegroundColor White
Write-Host "   - Procure: 'Rota ID=1 atualizada com ciclo 4'" -ForegroundColor White
Write-Host "   - Procure: 'Ciclo ID=4, numeroCiclo=4, status=EM_ANDAMENTO'" -ForegroundColor White
Write-Host ""
Write-Host "3. Execute uma sincronizacao INCREMENTAL:" -ForegroundColor White
Write-Host "   - Execute 'Sincronizar Dados' (incremental)" -ForegroundColor White
Write-Host ""
Write-Host "4. Execute este script para capturar logs:" -ForegroundColor White
Write-Host "   .\teste-correcao-ciclo-4.ps1" -ForegroundColor White
Write-Host ""
Write-Host "5. Verifique se os logs mostram:" -ForegroundColor White
Write-Host "   - 'Ciclo mais recente ID=4 incluido na sincronizacao incremental'" -ForegroundColor Green
Write-Host "   - Ciclo 4 sendo mantido como EM_ANDAMENTO" -ForegroundColor Green
Write-Host ""

Write-Host "RESULTADO ESPERADO:" -ForegroundColor Yellow
Write-Host "===================" -ForegroundColor Yellow
Write-Host ""
Write-Host "Antes da correção:" -ForegroundColor Red
Write-Host "   - Sincronizacao incremental nao trazia ciclo 4" -ForegroundColor Red
Write-Host "   - ClientListViewModel carregava ciclo 3" -ForegroundColor Red
Write-Host ""
Write-Host "Depois da correção:" -ForegroundColor Green
Write-Host "   - Sincronizacao incremental inclui ciclo 4 automaticamente" -ForegroundColor Green
Write-Host "   - ClientListViewModel carrega ciclo 4 corretamente" -ForegroundColor Green
Write-Host ""

Write-Host "EXECUTANDO CAPTURA DE LOGS AGORA..." -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan

# Executar o script de captura
try {
    & $scriptPath
} catch {
    Write-Host "ERRO ao executar script de captura: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "TESTE CONCLUIDO!" -ForegroundColor Green
Write-Host "Verifique os logs gerados acima para confirmar se a correção funcionou." -ForegroundColor Cyan
