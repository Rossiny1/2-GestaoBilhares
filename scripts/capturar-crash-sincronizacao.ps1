# Script para capturar crashes durante sincronizacao
# Foco: Capturar stack traces, exceptions e crashes que ocorrem durante o processo de sincronizacao
# Versao: 1.0 - Baseado no modelo capturar-logs-sincronizacao.ps1

Write-Host "=== CAPTURA DE CRASHES DE SINCRONIZACAO ===" -ForegroundColor Yellow
Write-Host "Objetivo: Monitorar e capturar crashes, exceptions e erros fatais durante sincronizacao" -ForegroundColor Cyan
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
Write-Host "  CAPTURANDO CRASHES DE SINCRONIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtros ativos:" -ForegroundColor Yellow
Write-Host "  - AndroidRuntime (crashes nativos)" -ForegroundColor White
Write-Host "  - SyncRepository (todos os niveis)" -ForegroundColor White
Write-Host "  - AppRepository (todos os niveis)" -ForegroundColor White
Write-Host "  - RoutesViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - SyncWorker (todos os niveis)" -ForegroundColor White
Write-Host "  - CrashlyticsCore (crashes Firebase)" -ForegroundColor White
Write-Host ""
Write-Host "Logs especificos capturados:" -ForegroundColor Cyan
Write-Host "  - Stack traces completos (FATAL EXCEPTION)" -ForegroundColor White
Write-Host "  - Exceptions nao tratadas (UncaughtException)" -ForegroundColor White
Write-Host "  - Erros do Firestore (PERMISSION_DENIED, UNAVAILABLE, etc.)" -ForegroundColor White
Write-Host "  - Crashes de sincronizacao (SyncRepository, AppRepository)" -ForegroundColor White
Write-Host "  - Erros de WorkManager (SyncWorker)" -ForegroundColor White
Write-Host "  - Erros de ViewModel (StateFlow, Coroutines)" -ForegroundColor White
Write-Host "  - NullPointerException, IllegalStateException, etc." -ForegroundColor White
Write-Host ""
Write-Host "Aguardando eventos de crash durante sincronizacao..." -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Gray
Write-Host ""

# Capturar logs com filtros especificos (crashes e exceptions)
& $ADB logcat -v time -s AndroidRuntime:* SyncRepository:* AppRepository:* RoutesViewModel:* SyncWorker:* CrashlyticsCore:* | ForEach-Object {
    $line = $_
    
    # Cores para diferentes tipos de crashes e exceptions
    
    # CRASHES FATAIS - AndroidRuntime
    if ($line -match "FATAL EXCEPTION|Fatal Exception|fatal exception") {
        Write-Host ""
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host "  CRASH FATAL DETECTADO!" -ForegroundColor Red
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "Process:.*died|Process.*crashed") {
        Write-Host $line -ForegroundColor Red
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host ""
    }
    # EXCEPTIONS NAO TRATADAS
    elseif ($line -match "UncaughtException|Uncaught exception|uncaught exception") {
        Write-Host ""
        Write-Host "================================================================" -ForegroundColor Magenta
        Write-Host "  EXCEPTION NAO TRATADA!" -ForegroundColor Magenta
        Write-Host "================================================================" -ForegroundColor Magenta
        Write-Host $line -ForegroundColor Magenta
    }
    # STACK TRACES
    elseif ($line -match "^\s+at\s+.*\(.*:\d+\)" -or $line -match "^\s+at\s+.*\(Native Method\)") {
        Write-Host $line -ForegroundColor DarkRed
    }
    elseif ($line -match "Caused by:|Suppressed:") {
        Write-Host $line -ForegroundColor Red
    }
    # EXCEPTIONS ESPECIFICAS
    elseif ($line -match "NullPointerException|NPE") {
        Write-Host ""
        Write-Host "*** NULL POINTER EXCEPTION DETECTADO ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "IllegalStateException|IllegalState") {
        Write-Host ""
        Write-Host "*** ILLEGAL STATE EXCEPTION DETECTADO ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "IllegalArgumentException|IllegalArgument") {
        Write-Host ""
        Write-Host "*** ILLEGAL ARGUMENT EXCEPTION DETECTADO ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "ConcurrentModificationException") {
        Write-Host ""
        Write-Host "*** CONCURRENT MODIFICATION EXCEPTION DETECTADO ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "IndexOutOfBoundsException") {
        Write-Host ""
        Write-Host "*** INDEX OUT OF BOUNDS EXCEPTION DETECTADO ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "ClassCastException") {
        Write-Host ""
        Write-Host "*** CLASS CAST EXCEPTION DETECTADO ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "OutOfMemoryError|OOM") {
        Write-Host ""
        Write-Host "*** OUT OF MEMORY ERROR DETECTADO ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    # ERROS DO FIRESTORE
    elseif ($line -match "FirebaseFirestoreException|Firestore.*Exception") {
        Write-Host ""
        Write-Host "*** FIRESTORE EXCEPTION DETECTADO ***" -ForegroundColor Magenta
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "PERMISSION_DENIED|permission denied") {
        Write-Host "*** FIRESTORE: PERMISSION DENIED ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "UNAVAILABLE|unavailable") {
        Write-Host "*** FIRESTORE: UNAVAILABLE ***" -ForegroundColor Yellow
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "DEADLINE_EXCEEDED|deadline exceeded") {
        Write-Host "*** FIRESTORE: TIMEOUT ***" -ForegroundColor Yellow
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "ALREADY_EXISTS|already exists") {
        Write-Host "*** FIRESTORE: ALREADY EXISTS ***" -ForegroundColor Yellow
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "NOT_FOUND|not found") {
        Write-Host "*** FIRESTORE: NOT FOUND ***" -ForegroundColor Yellow
        Write-Host $line -ForegroundColor Yellow
    }
    # ERROS DE SINCRONIZACAO - SyncRepository
    elseif ($line -match "SyncRepository.*ERRO|SyncRepository.*ERROR|SyncRepository.*Exception") {
        Write-Host "*** ERRO NO SYNC REPOSITORY ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "SyncRepository.*CRITICO|SyncRepository.*FATAL|SyncRepository.*CRITICAL") {
        Write-Host ""
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host "  ERRO CRITICO NO SYNC REPOSITORY!" -ForegroundColor Red
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "SyncRepository.*Falha ao processar|SyncRepository.*falhou") {
        Write-Host $line -ForegroundColor Red
    }
    # ERROS DE SINCRONIZACAO - AppRepository
    elseif ($line -match "AppRepository.*ERRO|AppRepository.*ERROR|AppRepository.*Exception") {
        Write-Host "*** ERRO NO APP REPOSITORY ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "AppRepository.*CRITICO|AppRepository.*FATAL|AppRepository.*CRITICAL") {
        Write-Host ""
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host "  ERRO CRITICO NO APP REPOSITORY!" -ForegroundColor Red
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "AppRepository.*SyncOperationDao.*null") {
        Write-Host "*** SYNC OPERATION DAO NULO! ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    # ERROS DE VIEWMODEL
    elseif ($line -match "ViewModel.*ERRO|ViewModel.*ERROR|ViewModel.*Exception") {
        Write-Host "*** ERRO NO VIEWMODEL ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "RoutesViewModel.*ERRO|RoutesViewModel.*ERROR|RoutesViewModel.*Exception") {
        Write-Host "*** ERRO NO ROUTES VIEWMODEL ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    # ERROS DE WORKMANAGER - SyncWorker
    elseif ($line -match "SyncWorker.*ERRO|SyncWorker.*ERROR|SyncWorker.*Exception") {
        Write-Host "*** ERRO NO SYNC WORKER ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "SyncWorker.*failed|SyncWorker.*falhou") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "WorkManager.*failed|WorkManager.*Exception") {
        Write-Host "*** ERRO NO WORKMANAGER ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    # ERROS DE COROUTINES
    elseif ($line -match "CoroutineExceptionHandler|Coroutine.*exception|Coroutine.*crashed") {
        Write-Host ""
        Write-Host "*** COROUTINE EXCEPTION DETECTADO ***" -ForegroundColor Magenta
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "kotlinx.coroutines.*Exception") {
        Write-Host $line -ForegroundColor Magenta
    }
    # ERROS DE DATABASE - Room
    elseif ($line -match "SQLiteException|DatabaseException") {
        Write-Host ""
        Write-Host "*** DATABASE EXCEPTION DETECTADO ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "FOREIGN KEY constraint|foreign key") {
        Write-Host "*** FOREIGN KEY VIOLATION ***" -ForegroundColor Red
        Write-Host $line -ForegroundColor Red
    }
    # CRASHLYTICS
    elseif ($line -match "CrashlyticsCore.*Crash") {
        Write-Host ""
        Write-Host "*** CRASHLYTICS REGISTROU UM CRASH ***" -ForegroundColor Magenta
        Write-Host $line -ForegroundColor Magenta
    }
    # ERROS GENERICOS
    elseif ($line -match "E/.*:.*Exception|E/.*:.*Error") {
        Write-Host $line -ForegroundColor Red
    }
    # WARNINGS IMPORTANTES
    elseif ($line -match "W/SyncRepository|W/AppRepository|W/RoutesViewModel|W/SyncWorker") {
        Write-Host $line -ForegroundColor Yellow
    }
    # MENSAGENS DE DEBUG IMPORTANTES
    elseif ($line -match "SyncRepository.*INICIANDO|SyncRepository.*Starting|SyncRepository.*Processando") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "SyncRepository.*CONCLUIDO|SyncRepository.*Completed|SyncRepository.*Finished") {
        Write-Host $line -ForegroundColor Green
    }
    # LOGS NORMAIS (sem destaque)
    else {
        Write-Host $line -ForegroundColor Gray
    }
}
