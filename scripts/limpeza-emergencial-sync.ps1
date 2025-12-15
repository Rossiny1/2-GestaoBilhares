# LIMPEZA EMERGENCIAL DE SINCRONIZAÇÃO
# Reset completo baseado em melhores práticas de recuperação de sync

Write-Host "=== LIMPEZA EMERGENCIAL DE SINCRONIZACAO ===" -ForegroundColor Red
Write-Host "ATENCAO: Esta operacao ira remover TODOS os dados locais!" -ForegroundColor Red
Write-Host "Data/Hora: $(Get-Date)" -ForegroundColor Gray
Write-Host ""

$confirm = Read-Host "Digite 'SIM' para confirmar a limpeza completa"
if ($confirm -ne "SIM") {
    Write-Host "Operacao cancelada." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "INICIANDO LIMPEZA EMERGENCIAL..." -ForegroundColor Red

# ========== CONFIGURAÇÃO ==========

$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$PACKAGE = "com.example.gestaobilhares"

# ========== VERIFICAÇÕES ==========

if (!(Test-Path $ADB)) {
    Write-Host "ERRO: ADB nao encontrado" -ForegroundColor Red
    exit 1
}

& $ADB devices | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Dispositivo nao conectado" -ForegroundColor Red
    exit 1
}

Write-Host "OK: Verificacoes passaram" -ForegroundColor Green

# ========== LIMPEZA COMPLETA ==========

Write-Host ""
Write-Host "1. PARANDO APP" -ForegroundColor Yellow
& $ADB shell am force-stop $PACKAGE | Out-Null
Write-Host "   App parado" -ForegroundColor Green

Write-Host ""
Write-Host "2. LIMPANDO DADOS DO APP" -ForegroundColor Yellow
& $ADB shell pm clear $PACKAGE | Out-Null
Write-Host "   Dados do app removidos" -ForegroundColor Green

Write-Host ""
Write-Host "3. LIMPANDO CACHE E DADOS RESIDUAIS" -ForegroundColor Yellow
& $ADB shell "rm -rf /data/data/$PACKAGE/cache/*" 2>$null | Out-Null
& $ADB shell "rm -rf /data/data/$PACKAGE/files/*" 2>$null | Out-Null
& $ADB shell "rm -rf /data/data/$PACKAGE/shared_prefs/*" 2>$null | Out-Null
Write-Host "   Cache e arquivos residuais removidos" -ForegroundColor Green

Write-Host ""
Write-Host "4. LIMPANDO LOGS DO SISTEMA" -ForegroundColor Yellow
& $ADB logcat -c | Out-Null
Write-Host "   Logs do sistema limpos" -ForegroundColor Green

Write-Host ""
Write-Host "5. REINICIANDO SERVICOS" -ForegroundColor Yellow
& $ADB shell "am broadcast -a android.intent.action.BOOT_COMPLETED" 2>$null | Out-Null
Write-Host "   Servicos reiniciados" -ForegroundColor Green

Write-Host ""
Write-Host "6. VERIFICACAO FINAL" -ForegroundColor Yellow

# Verificar se banco foi removido
$dbCheck = & $ADB shell "run-as $PACKAGE ls /data/data/$PACKAGE/databases/ 2>/dev/null | wc -l" 2>$null
if ($dbCheck -match "0") {
    Write-Host "   Banco de dados removido com sucesso" -ForegroundColor Green
} else {
    Write-Host "   AVISO: Possivel resíduo de banco de dados" -ForegroundColor Yellow
}

# Verificar processos
$processCheck = & $ADB shell ps | Select-String -Pattern $PACKAGE | Measure-Object | Select-Object -ExpandProperty Count
if ($processCheck -eq 0) {
    Write-Host "   Nenhum processo do app em execucao" -ForegroundColor Green
} else {
    Write-Host "   AVISO: $processCheck processo(s) ainda ativo(s)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "LIMPEZA CONCLUIDA!" -ForegroundColor Green
Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "================"
Write-Host "1. Reinicie o dispositivo (opcional mas recomendado)" -ForegroundColor White
Write-Host "2. Abra o app novamente" -ForegroundColor White
Write-Host "3. Execute sincronizacao completa" -ForegroundColor White
Write-Host "4. Execute diagnostico avancado:" -ForegroundColor White
Write-Host "   .\diagnostico-avancado-ciclo-4.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "IMPORTANTE: Esta limpeza resetou TODO o estado local." -ForegroundColor Yellow
Write-Host "O app ira se comportar como se fosse a primeira instalacao." -ForegroundColor Yellow
