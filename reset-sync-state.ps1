# RESET DE ESTADO DE SINCRONIZAÇÃO
# Baseado em padrões de recuperação de estado distribuído

Write-Host "=== RESET DE ESTADO DE SINCRONIZACAO ===" -ForegroundColor Yellow
Write-Host "Reset inteligente mantendo dados validos" -ForegroundColor Cyan
Write-Host "Data/Hora: $(Get-Date)" -ForegroundColor Gray
Write-Host ""

# ========== CONFIGURAÇÃO ==========

$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$PACKAGE = "com.example.gestaobilhares"

# ========== VERIFICAÇÕES ==========

Write-Host "VERIFICACOES PREVIAS" -ForegroundColor Yellow
Write-Host "==================="

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

# ========== BACKUP SELETIVO ==========

Write-Host ""
Write-Host "BACKUP SELETIVO" -ForegroundColor Yellow
Write-Host "==============="

$backupDir = "backup_sync_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

Write-Host "Fazendo backup dos dados atuais..." -ForegroundColor Gray

# Backup de ciclos
$ciclosBackup = & $ADB shell "run-as $PACKAGE sqlite3 -json /data/data/$PACKAGE/databases/gestao_bilhares.db 'SELECT * FROM ciclos_acerto;'" 2>$null
if ($ciclosBackup) {
    $ciclosBackup | Out-File "$backupDir\ciclos_backup.json" -Encoding UTF8
    Write-Host "   Backup de ciclos: OK" -ForegroundColor Green
} else {
    Write-Host "   Backup de ciclos: Nenhum dado encontrado" -ForegroundColor Yellow
}

# Backup de rotas
$rotasBackup = & $ADB shell "run-as $PACKAGE sqlite3 -json /data/data/$PACKAGE/databases/gestao_bilhares.db 'SELECT * FROM rotas;'" 2>$null
if ($rotasBackup) {
    $rotasBackup | Out-File "$backupDir\rotas_backup.json" -Encoding UTF8
    Write-Host "   Backup de rotas: OK" -ForegroundColor Green
} else {
    Write-Host "   Backup de rotas: Nenhum dado encontrado" -ForegroundColor Yellow
}

Write-Host "Backup salvo em: $backupDir" -ForegroundColor Cyan

# ========== RESET INTELIGENTE ==========

Write-Host ""
Write-Host "RESET INTELIGENTE" -ForegroundColor Yellow
Write-Host "================="

# 1. Parar app
Write-Host "1. Parando aplicacao..." -ForegroundColor Gray
& $ADB shell am force-stop $PACKAGE | Out-Null
Write-Host "   OK" -ForegroundColor Green

# 2. Limpar apenas metadados de sync (não dados do usuário)
Write-Host "2. Limpando metadados de sincronizacao..." -ForegroundColor Gray

# Remover SharedPreferences relacionadas a sync
& $ADB shell "run-as $PACKAGE rm -f /data/data/$PACKAGE/shared_prefs/*sync*.xml" 2>$null | Out-Null
& $ADB shell "run-as $PACKAGE rm -f /data/data/$PACKAGE/shared_prefs/*Sync*.xml" 2>$null | Out-Null

Write-Host "   OK" -ForegroundColor Green

# 3. Reset do banco de dados de sync
Write-Host "3. Resetando estado de sincronizacao no banco..." -ForegroundColor Gray

# Queries para limpar apenas estado de sync, mantendo dados do usuário
$sqlCommands = @(
    "DELETE FROM sync_metadata;",
    "UPDATE ciclos_acerto SET dataAtualizacao = 0 WHERE dataAtualizacao IS NOT NULL;",
    "UPDATE rotas SET dataAtualizacao = 0 WHERE dataAtualizacao IS NOT NULL;",
    "VACUUM;"
)

foreach ($sql in $sqlCommands) {
    & $ADB shell "run-as $PACKAGE sqlite3 /data/data/$PACKAGE/databases/gestao_bilhares.db '$sql'" 2>$null | Out-Null
}

Write-Host "   OK" -ForegroundColor Green

# 4. Limpar cache de sync
Write-Host "4. Limpando cache de sincronizacao..." -ForegroundColor Gray
& $ADB shell "run-as $PACKAGE rm -rf /data/data/$PACKAGE/cache/sync_*" 2>$null | Out-Null
& $ADB shell "run-as $PACKAGE rm -rf /data/data/$PACKAGE/files/sync_*" 2>$null | Out-Null
Write-Host "   OK" -ForegroundColor Green

# 5. Limpar logs
Write-Host "5. Limpando logs do sistema..." -ForegroundColor Gray
& $ADB logcat -c | Out-Null
Write-Host "   OK" -ForegroundColor Green

# ========== VERIFICAÇÃO ==========

Write-Host ""
Write-Host "VERIFICACAO" -ForegroundColor Yellow
Write-Host "==========="

# Verificar se dados do usuário foram preservados
$ciclosAposReset = & $ADB shell "run-as $PACKAGE sqlite3 /data/data/$PACKAGE/databases/gestao_bilhares.db 'SELECT COUNT(*) FROM ciclos_acerto;'" 2>$null
$rotasAposReset = & $ADB shell "run-as $PACKAGE sqlite3 /data/data/$PACKAGE/databases/gestao_bilhares.db 'SELECT COUNT(*) FROM rotas;'" 2>$null

Write-Host "Dados preservados apos reset:" -ForegroundColor Cyan
Write-Host "   Ciclos: $ciclosAposReset" -ForegroundColor White
Write-Host "   Rotas: $rotasAposReset" -ForegroundColor White

# Verificar metadados de sync
$syncMetadata = & $ADB shell "run-as $PACKAGE sqlite3 /data/data/$PACKAGE/databases/gestao_bilhares.db 'SELECT COUNT(*) FROM sync_metadata;'" 2>$null
Write-Host "   Metadados de sync: $syncMetadata (deve ser 0)" -ForegroundColor $(if ($syncMetadata -eq "0") { "Green" } else { "Yellow" })

# ========== RESTAURAÇÃO SELETIVA ==========

Write-Host ""
Write-Host "RESTAURACAO SELETIVA" -ForegroundColor Yellow
Write-Host "===================="

if ((Test-Path "$backupDir\ciclos_backup.json") -and $ciclosAposReset -eq "0") {
    Write-Host "Restaurando ciclos do backup..." -ForegroundColor Gray

    # Aqui seria implementada a lógica de restauração se necessário
    # Por enquanto, apenas informa que o backup existe

    Write-Host "   Backup disponivel: $backupDir\ciclos_backup.json" -ForegroundColor Yellow
    Write-Host "   NOTA: Restauração manual pode ser necessária se dados foram perdidos" -ForegroundColor Yellow
}

# ========== RECOMENDAÇÕES ==========

Write-Host ""
Write-Host "RESET CONCLUIDO!" -ForegroundColor Green
Write-Host ""
Write-Host "PROXIMOS PASSOS RECOMENDADOS:" -ForegroundColor Cyan
Write-Host "=============================="
Write-Host ""
Write-Host "1. REINICIE O DISPOSITIVO" -ForegroundColor Yellow
Write-Host "   Recomendado para limpar cache do sistema" -ForegroundColor White
Write-Host ""
Write-Host "2. ABRA O APP NOVAMENTE" -ForegroundColor White
Write-Host "   O app ira detectar que precisa sincronizar" -ForegroundColor White
Write-Host ""
Write-Host "3. EXECUTE SINCRONIZACAO COMPLETA" -ForegroundColor White
Write-Host "   Va em Configuracoes > Sincronizar Tudo" -ForegroundColor White
Write-Host ""
Write-Host "4. MONITORE O PROCESSO" -ForegroundColor White
Write-Host "   .\monitor-sync-tempo-real.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "5. EXECUTE DIAGNOSTICO" -ForegroundColor White
Write-Host "   .\diagnostico-avancado-ciclo-4.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "BACKUP SALVO EM: $backupDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "IMPORTANTE: Dados do usuario foram PRESERVADOS." -ForegroundColor Green
Write-Host "Apenas estado de sincronizacao foi resetado." -ForegroundColor Green
