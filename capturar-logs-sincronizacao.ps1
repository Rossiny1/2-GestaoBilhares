# Script para capturar logs de sincronizacao e autenticacao
# Foco: Debugar problemas de sincronizacao automatica, dialogo de sincronizacao e login

Write-Host "=== CAPTURA DE LOGS DE SINCRONIZACAO E LOGIN ===" -ForegroundColor Yellow
Write-Host "Objetivo: Analisar o fluxo de verificacao de sincronizacao, dialogo e autenticacao" -ForegroundColor Cyan
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
Write-Host "  - AuthViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - LoginFragment (todos os niveis)" -ForegroundColor White
Write-Host "  - FirebaseAuth (todos os niveis)" -ForegroundColor White
Write-Host ""
Write-Host "Aguardando eventos de sincronizacao e login..." -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Gray
Write-Host ""

# Capturar logs com filtros especificos (usando o mesmo padrao do script que funcionava)
& $ADB logcat -v time -s RoutesViewModel:* RoutesFragment:* SyncRepository:* UserSessionManager:* AuthViewModel:* LoginFragment:* FirebaseAuth:* | ForEach-Object {
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
    # Logs de autenticacao e login
    elseif ($line -match "AuthViewModel.*INICIANDO LOGIN|AuthViewModel.*Tentando login") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "AuthViewModel.*LOGIN.*SUCESSO|AuthViewModel.*Sessao iniciada") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "AuthViewModel.*ERRO|AuthViewModel.*Erro|AuthViewModel.*ERROR|AuthViewModel.*nao encontrado|AuthViewModel.*nao foi encontrado") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "AuthViewModel.*Colaborador encontrado|AuthViewModel.*criarOuAtualizarColaborador|AuthViewModel.*buscarColaboradorNaNuvem") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "AuthViewModel.*Firebase|AuthViewModel.*firebaseAuth|AuthViewModel.*Firebase UID") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "AuthViewModel.*Email|AuthViewModel.*Senha|AuthViewModel.*Validacao") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "LoginFragment.*ONCREATE|LoginFragment.*ONVIEWCREATED|LoginFragment.*inicializado") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "LoginFragment.*ERRO|LoginFragment.*Erro|LoginFragment.*ERROR") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "FirebaseAuth.*signInWithEmail|FirebaseAuth.*authentication|FirebaseAuth.*user") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "FirebaseAuth.*ERROR|FirebaseAuth.*Exception|FirebaseAuth.*failed") {
        Write-Host $line -ForegroundColor Red
    }
    # Logs de colaboradores - Pull e processamento
    elseif ($line -match "SyncRepository.*Pull.*colaboradores|SyncRepository.*pullColaboradores") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "SyncRepository.*Pull COMPLETO de colaboradores|SyncRepository.*documentos recebidos") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "SyncRepository.*Documentos de colaboradores recebidos|SyncRepository.*Cache local de colaboradores") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "SyncRepository.*Processando.*colaboradores|SyncRepository.*processColaborador") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "SyncRepository.*Inserindo novo colaborador|SyncRepository.*Colaborador inserido com sucesso") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "SyncRepository.*Colaborador encontrado por email|SyncRepository.*Colaborador duplicado") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "SyncRepository.*Sincronizado.*Email|SyncRepository.*Resultado do processamento") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "SyncRepository.*ERRO.*colaborador|SyncRepository.*Erro ao.*colaborador") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "SyncRepository.*Pull de colaboradores concluido|SyncRepository.*sync=.*skipped=.*errors=") {
        Write-Host $line -ForegroundColor Green
    }
    else {
        Write-Host $line
    }
}

