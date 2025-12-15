# TESTE COMPLETO DO CICLO 4
# Script que combina instalação, teste e análise

Write-Host "🚀 TESTE COMPLETO - CICLO 4" -ForegroundColor Yellow
Write-Host "Data/Hora: $(Get-Date)" -ForegroundColor Gray
Write-Host ""

# Configurações
$packageName = "com.example.gestaobilhares"
$adbPath = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# ========== VERIFICAÇÕES INICIAIS ==========

Write-Host "1️⃣ VERIFICAÇÕES INICIAIS" -ForegroundColor Yellow
Write-Host "------------------------" -ForegroundColor Yellow

# Verificar ADB
if (!(Test-Path $adbPath)) {
    Write-Host "❌ ADB não encontrado" -ForegroundColor Red
    exit 1
}
Write-Host "✅ ADB encontrado" -ForegroundColor Green

# Verificar dispositivo
$devices = & $adbPath devices
if ($devices -notmatch "device$") {
    Write-Host "❌ Nenhum dispositivo conectado" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Dispositivo conectado" -ForegroundColor Green

# Verificar/instalar APK
Write-Host ""
Write-Host "2️⃣ VERIFICANDO APK" -ForegroundColor Yellow
Write-Host "------------------" -ForegroundColor Yellow

$checkInstalled = & $adbPath shell pm list packages $packageName 2>$null
if ($checkInstalled -notlike "*$packageName*") {
    Write-Host "❌ APK não instalado" -ForegroundColor Red

    # Procurar APK
    $apkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        Write-Host "📦 Instalando APK..." -ForegroundColor Yellow
        & $adbPath install -r $apkPath
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ APK instalado" -ForegroundColor Green
        } else {
            Write-Host "❌ Falha na instalação" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "❌ APK não encontrado. Execute: ./gradlew assembleDebug" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✅ APK já instalado" -ForegroundColor Green
}

# ========== LIMPAR DADOS ANTERIORES ==========

Write-Host ""
Write-Host "3️⃣ PREPARANDO AMBIENTE" -ForegroundColor Yellow
Write-Host "----------------------" -ForegroundColor Yellow

Write-Host "🧹 Limpando logs..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "🗑️  Limpando dados do app..." -ForegroundColor Yellow
& $adbPath shell pm clear $packageName

Write-Host "✅ Ambiente preparado" -ForegroundColor Green

# ========== EXECUTAR TESTE ==========

Write-Host ""
Write-Host "4️⃣ EXECUTANDO TESTE" -ForegroundColor Yellow
Write-Host "------------------" -ForegroundColor Yellow

# Nome do arquivo de log
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile = "teste_completo_ciclo_4_$timestamp.txt"

Write-Host "📊 Iniciando captura de logs..." -ForegroundColor Cyan
Write-Host "📄 Arquivo: $logFile" -ForegroundColor Gray

# Iniciar captura em background
$logJob = Start-Job -ScriptBlock {
    param($adbPath, $logFile)
    & $adbPath logcat -v time -s SyncRepository:* RoutesViewModel:* RoutesFragment:* | Out-File -FilePath $logFile -Encoding UTF8
} -ArgumentList $adbPath, $logFile

Start-Sleep -Seconds 2

Write-Host "🚀 Executando sincronização..." -ForegroundColor Green
& $adbPath shell am start -n "$packageName/.ui.auth.AuthActivity"

Write-Host "⏳ Aguardando sincronização (15s)..." -ForegroundColor Yellow
for ($i = 15; $i -gt 0; $i--) {
    Write-Host "   $i segundos restantes..." -ForegroundColor Gray
    Start-Sleep -Seconds 1
}

# Parar captura
Write-Host "🛑 Parando captura de logs..." -ForegroundColor Yellow
Stop-Job -Job $logJob
Remove-Job -Job $logJob

# ========== ANÁLISE AUTOMÁTICA ==========

Write-Host ""
Write-Host "5️⃣ ANÁLISE AUTOMÁTICA" -ForegroundColor Yellow
Write-Host "--------------------" -ForegroundColor Yellow

if (!(Test-Path $logFile)) {
    Write-Host "❌ Arquivo de log não encontrado" -ForegroundColor Red
    exit 1
}

Write-Host "📊 Analisando logs capturados..." -ForegroundColor Cyan

# Contadores
$ciclo4Logs = (Get-Content $logFile | Select-String -Pattern "Ciclo ID=4|numeroCiclo=4").Count
$rotaAtualizadaLogs = (Get-Content $logFile | Select-String -Pattern "atualizada.*ciclo 4|cicloAcertoAtual=4").Count
$syncLogs = (Get-Content $logFile | Select-String -Pattern "Sincroniza.*conclu|sync=4").Count
$errorLogs = (Get-Content $logFile | Select-String -Pattern "ERRO|ERROR|Exception").Count

# Verificar dados no banco
Write-Host ""
Write-Host "6️⃣ VERIFICANDO BANCO DE DADOS" -ForegroundColor Yellow
Write-Host "-----------------------------" -ForegroundColor Yellow

Write-Host "🔍 Ciclos no banco:" -ForegroundColor Cyan
$dbCiclos = & $adbPath shell "run-as $packageName sqlite3 -header -column /data/data/$packageName/databases/gestao_bilhares.db 'SELECT id, numero_ciclo, status FROM ciclos_acerto WHERE rota_id = 1 ORDER BY numero_ciclo DESC LIMIT 5;'" 2>$null
if ($dbCiclos) {
    $dbCiclos | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
    $ciclo4NoBanco = ($dbCiclos | Select-String -Pattern "4.*4.*EM_ANDAMENTO").Count -gt 0
} else {
    Write-Host "   ❌ Erro ao consultar banco" -ForegroundColor Red
    $ciclo4NoBanco = $false
}

Write-Host ""
Write-Host "🔍 Dados da rota:" -ForegroundColor Cyan
$dbRota = & $adbPath shell "run-as $packageName sqlite3 -header -column /data/data/$packageName/databases/gestao_bilhares.db 'SELECT id, nome, ciclo_acerto_atual, status_atual FROM rotas WHERE id = 1;'" 2>$null
if ($dbRota) {
    $dbRota | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
    $rotaComCiclo4 = ($dbRota | Select-String -Pattern "1.*.*4.*").Count -gt 0
} else {
    Write-Host "   ❌ Erro ao consultar banco" -ForegroundColor Red
    $rotaComCiclo4 = $false
}

# ========== RESULTADO FINAL ==========

Write-Host ""
Write-Host "🎯 RESULTADO FINAL" -ForegroundColor Yellow
Write-Host "=================" -ForegroundColor Yellow
Write-Host ""
Write-Host "📄 Arquivo de log: $logFile" -ForegroundColor White
Write-Host ""

# Tabela de resultados
Write-Host "📊 MÉTRICAS:" -ForegroundColor Cyan
Write-Host "   Ciclo 4 nos logs    : $(if ($ciclo4Logs -gt 0) { "✅ $ciclo4Logs" } else { "❌ 0" })" -ForegroundColor $(if ($ciclo4Logs -gt 0) { "Green" } else { "Red" })
Write-Host "   Rota atualizada     : $(if ($rotaAtualizadaLogs -gt 0) { "✅ $rotaAtualizadaLogs" } else { "❌ 0" })" -ForegroundColor $(if ($rotaAtualizadaLogs -gt 0) { "Green" } else { "Red" })
Write-Host "   Sync concluída      : $(if ($syncLogs -gt 0) { "✅ $syncLogs" } else { "❌ 0" })" -ForegroundColor $(if ($syncLogs -gt 0) { "Green" } else { "Red" })
Write-Host "   Erros encontrados   : $(if ($errorLogs -eq 0) { "✅ 0" } else { "❌ $errorLogs" })" -ForegroundColor $(if ($errorLogs -eq 0) { "Green" } else { "Red" })
Write-Host ""
Write-Host "💾 BANCO DE DADOS:" -ForegroundColor Cyan
Write-Host "   Ciclo 4 no banco    : $(if ($ciclo4NoBanco) { "✅ SIM" } else { "❌ NÃO" })" -ForegroundColor $(if ($ciclo4NoBanco) { "Green" } else { "Red" })
Write-Host "   Rota com ciclo 4    : $(if ($rotaComCiclo4) { "✅ SIM" } else { "❌ NÃO" })" -ForegroundColor $(if ($rotaComCiclo4) { "Green" } else { "Red" })

Write-Host ""

# Diagnóstico
$statusGeral = ($ciclo4Logs -gt 0) -and ($rotaAtualizadaLogs -gt 0) -and ($syncLogs -gt 0) -and ($errorLogs -eq 0) -and $ciclo4NoBanco -and $rotaComCiclo4

if ($statusGeral) {
    Write-Host "🎉 STATUS: CICLO 4 FUNCIONANDO PERFEITAMENTE!" -ForegroundColor Green
    Write-Host ""
    Write-Host "✅ Todas as verificações passaram" -ForegroundColor Green
    Write-Host "✅ O ciclo 4 está sendo exibido corretamente" -ForegroundColor Green
} else {
    Write-Host "⚠️  STATUS: PROBLEMAS DETECTADOS" -ForegroundColor Yellow
    Write-Host ""

    if ($ciclo4Logs -eq 0) {
        Write-Host "❌ Ciclo 4 não encontrado nos logs - verifique sincronização" -ForegroundColor Red
    }
    if ($rotaAtualizadaLogs -eq 0) {
        Write-Host "❌ Rota não foi atualizada - problema no refresh" -ForegroundColor Red
    }
    if ($errorLogs -gt 0) {
        Write-Host "❌ Erros encontrados - verifique arquivo de log" -ForegroundColor Red
    }
    if (!$ciclo4NoBanco) {
        Write-Host "❌ Ciclo 4 não está no banco - problema na sincronização" -ForegroundColor Red
    }
    if (!$rotaComCiclo4) {
        Write-Host "❌ Rota não tem ciclo 4 - problema no mapeamento" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "💡 PRÓXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "   📄 Analisar log completo: notepad '$logFile'" -ForegroundColor White
Write-Host "   🔍 Análise detalhada: .\analisar-logs-ciclo-4.bat '$logFile'" -ForegroundColor White
Write-Host "   🧪 Novo teste: .\teste-completo-ciclo-4.ps1" -ForegroundColor White

Write-Host ""
Write-Host "✅ TESTE COMPLETO CONCLUÍDO!" -ForegroundColor Green
