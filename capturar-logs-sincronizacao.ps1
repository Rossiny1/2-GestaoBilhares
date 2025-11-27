# Script para capturar logs de sincronizacao
# Foco: Debugar problemas de sincronizacao automatica e dialogo de sincronizacao

Write-Host "=== CAPTURA DE LOGS DE SINCRONIZACAO ===" -ForegroundColor Yellow
Write-Host "Objetivo: Analisar o fluxo de verificacao de sincronizacao e dialogo" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB (mesmo padrao dos outros scripts)
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (!(Test-Path $ADB)) {
    Write-Host "ADB nao encontrado em: $ADB" -ForegroundColor Red
    Write-Host "Certifique-se de que o Android SDK esta instalado corretamente" -ForegroundColor Red
    exit 1
}

# Verificar se ha dispositivo conectado
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $ADB devices
if ($devices -match "device$") {
    Write-Host "Dispositivo encontrado!" -ForegroundColor Green
} else {
    Write-Host "Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo USB ou inicie um emulador" -ForegroundColor Yellow
    exit 1
}

# Limpar logs anteriores
Write-Host ""
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c

# Iniciar captura de logs
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CAPTURANDO LOGS DE SINCRONIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtros ativos:" -ForegroundColor Yellow
Write-Host "  - RoutesViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - RoutesFragment (todos os niveis)" -ForegroundColor White
Write-Host "  - SyncRepository (todos os niveis)" -ForegroundColor White
Write-Host "  - UserSessionManager (todos os niveis)" -ForegroundColor White
Write-Host ""
Write-Host "Aguardando eventos de sincronizacao..." -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Gray
Write-Host ""

# Capturar logs com filtros especificos (usando o mesmo padrao do script que funcionava)
& $ADB logcat -v time -s RoutesViewModel:* RoutesFragment:* SyncRepository:* UserSessionManager:* | ForEach-Object {
    $line = $_
    
    # Cores para diferentes tipos de logs
    if ($line -match "RoutesViewModel.*Verificando pendencias|RoutesViewModel.*Verificando sincronizacao") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "RoutesViewModel.*Mostrando dialogo|RoutesViewModel.*syncDialogState") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "RoutesViewModel.*Nao mostrando dialogo|RoutesViewModel.*syncDialogDismissed") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "RoutesViewModel.*Dados na nuvem encontrados|RoutesViewModel.*hasDataInCloud") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "RoutesViewModel.*Rotas locais|RoutesViewModel.*Banco local") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "RoutesViewModel.*Pendencias de sincronizacao|RoutesViewModel.*pending") {
        Write-Host $line -ForegroundColor Blue
    }
    elseif ($line -match "RoutesViewModel.*Erro|RoutesViewModel.*ERROR|RoutesViewModel.*Exception") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "SyncRepository.*hasDataInCloud|SyncRepository.*Verificando dados na nuvem") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "SyncRepository.*Dados na nuvem encontrados") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "SyncRepository.*Erro ao verificar|SyncRepository.*ERROR") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "RoutesFragment.*checkSyncPendencies|RoutesFragment.*Verificando sincronizacao") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "UserSessionManager.*getCurrentUserId|UserSessionManager.*startSession") {
        Write-Host $line -ForegroundColor DarkYellow
    }
    elseif ($line -match "RoutesViewModel.*Usuario logado|RoutesViewModel.*userId") {
        Write-Host $line -ForegroundColor DarkGreen
    }
    else {
        Write-Host $line
    }
}

