# Script para testar se o ciclo 4 está sendo exibido corretamente após as correções
Write-Host "🔍 Testando exibição do ciclo 4..." -ForegroundColor Cyan
Write-Host "Script executado em: $(Get-Date)" -ForegroundColor Gray

# Configurações
$packageName = "com.example.gestaobilhares"
$adbPath = "adb"  # Assume que ADB está no PATH

# Verificar se ADB está disponível
Write-Host "1. Verificando ADB..." -ForegroundColor Yellow
try {
    $adbVersion = & $adbPath version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ ADB encontrado" -ForegroundColor Green
    } else {
        Write-Host "❌ ADB não encontrado no PATH. Verifique se o Android SDK está instalado." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Erro ao executar ADB: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Verificar dispositivos conectados
Write-Host "2. Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $adbPath devices 2>$null
$deviceCount = ($devices | Select-String -Pattern "^[a-zA-Z0-9]+\s+device$").Count

if ($deviceCount -eq 0) {
    Write-Host "❌ Nenhum dispositivo Android conectado." -ForegroundColor Red
    Write-Host "   Conecte um dispositivo ou inicie um emulador." -ForegroundColor Yellow
    exit 1
} elseif ($deviceCount -gt 1) {
    Write-Host "⚠️  Múltiplos dispositivos conectados. Usando o primeiro." -ForegroundColor Yellow
}

Write-Host "✅ Dispositivo conectado" -ForegroundColor Green

# Verificar se o APK está instalado
Write-Host "3. Verificando se o APK está instalado..." -ForegroundColor Yellow
$checkInstalled = & $adbPath shell pm list packages $packageName 2>$null
if ($checkInstalled -notlike "*$packageName*") {
    Write-Host "❌ APK não está instalado." -ForegroundColor Red

    # Procurar APK no diretório do projeto
    $apkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        Write-Host "📦 Instalando APK..." -ForegroundColor Yellow
        & $adbPath install -r $apkPath
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ APK instalado com sucesso" -ForegroundColor Green
        } else {
            Write-Host "❌ Falha na instalação do APK" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "❌ APK não encontrado em $apkPath" -ForegroundColor Red
        Write-Host "   Execute primeiro: ./gradlew assembleDebug" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "✅ APK já está instalado" -ForegroundColor Green
}

# Limpar logs anteriores
Write-Host "4. Limpando logs anteriores..." -ForegroundColor Yellow
& $adbPath logcat -c

# Executar sincronização
Write-Host "5. Executando sincronização..." -ForegroundColor Yellow
& $adbPath shell am start -n "$packageName/.ui.auth.AuthActivity"

Write-Host "6. Aguardando sincronização completar..." -ForegroundColor Yellow
for ($i = 10; $i -gt 0; $i--) {
    Write-Host "   Aguardando $i segundos..." -ForegroundColor Gray
    Start-Sleep -Seconds 1
}

# Capturar logs de sincronização
Write-Host "7. Capturando logs de sincronização..." -ForegroundColor Yellow
$logs = & $adbPath logcat -d -s SyncRepository RoutesViewModel 2>$null
$relevantLogs = $logs | Select-String -Pattern "Ciclo ID=4|ciclo 4|numeroCiclo=4|Sincroniza.*conclu.*sucesso|Rota.*atualizada.*ciclo"

if ($relevantLogs) {
    Write-Host "📋 Logs relevantes encontrados:" -ForegroundColor Cyan
    $relevantLogs | ForEach-Object { Write-Host "   $($_.Line)" -ForegroundColor White }
} else {
    Write-Host "⚠️  Nenhum log específico do ciclo 4 encontrado" -ForegroundColor Yellow
}

# Verificar dados no banco
Write-Host "8. Verificando dados no banco..." -ForegroundColor Yellow
Write-Host "   Ciclos de acerto (rota 1):" -ForegroundColor Cyan
$dbQuery1 = & $adbPath shell "run-as $packageName sqlite3 -header -column /data/data/$packageName/databases/gestao_bilhares.db 'SELECT id, numero_ciclo, status FROM ciclos_acerto WHERE rota_id = 1 ORDER BY numero_ciclo DESC LIMIT 5;' 2>$null"
if ($dbQuery1) {
    $dbQuery1 | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
} else {
    Write-Host "   ❌ Erro ao consultar banco ou tabela não existe" -ForegroundColor Red
}

Write-Host "   Dados da rota 1:" -ForegroundColor Cyan
$dbQuery2 = & $adbPath shell "run-as $packageName sqlite3 -header -column /data/data/$packageName/databases/gestao_bilhares.db 'SELECT id, nome, ciclo_acerto_atual, status_atual FROM rotas WHERE id = 1;' 2>$null"
if ($dbQuery2) {
    $dbQuery2 | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
} else {
    Write-Host "   ❌ Erro ao consultar banco ou tabela não existe" -ForegroundColor Red
}

# Verificar processos em execução
Write-Host "9. Verificando se o app está rodando..." -ForegroundColor Yellow
$appProcess = & $adbPath shell ps | Select-String -Pattern $packageName
if ($appProcess) {
    Write-Host "✅ App está em execução" -ForegroundColor Green
} else {
    Write-Host "❌ App não está em execução" -ForegroundColor Red
}

Write-Host "" -ForegroundColor White
Write-Host "✅ Teste concluído!" -ForegroundColor Green
Write-Host "📝 Verifique os logs acima para confirmar se o ciclo 4 está sendo exibido corretamente." -ForegroundColor Cyan
Write-Host "" -ForegroundColor White
Write-Host "💡 Se o ciclo 4 ainda não aparecer:" -ForegroundColor Yellow
Write-Host "   1. Reinicie o app completamente" -ForegroundColor White
Write-Host "   2. Execute sincronização manual" -ForegroundColor White
Write-Host "   3. Verifique conexão com internet" -ForegroundColor White
