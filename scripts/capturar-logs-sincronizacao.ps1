# Script para capturar logs de sincronizacao, autenticacao e exclusao de despesas
# Foco: Debugar problemas de sincronizacao automatica, dialogo de sincronizacao, login e exclusao de despesas
# Versao: 2.0 - Atualizado para capturar todos os logs de DELETE e processamento da fila de sincronizacao

Write-Host "=== CAPTURA DE LOGS DE SINCRONIZACAO, LOGIN E EXCLUSAO ===" -ForegroundColor Yellow
Write-Host "Objetivo: Analisar o fluxo de verificacao de sincronizacao, dialogo, autenticacao e exclusao de despesas" -ForegroundColor Cyan
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
Write-Host "  - AppRepository (todos os niveis)" -ForegroundColor White
Write-Host "  - CycleExpensesViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - CycleExpensesFragment (todos os niveis)" -ForegroundColor White
Write-Host ""
Write-Host "Logs especificos capturados:" -ForegroundColor Cyan
Write-Host "  - Processamento da fila de sincronizacao (processSyncQueue)" -ForegroundColor White
Write-Host "  - Operacoes DELETE (inicio, execucao, verificacao pos-DELETE)" -ForegroundColor White
Write-Host "  - Enfileiramento de operacoes (AppRepository)" -ForegroundColor White
Write-Host "  - Mapeamento de entidades para colecoes Firestore" -ForegroundColor White
Write-Host "  - Monitoramento de Claims e Permissoes (Diagnostico 2025)" -ForegroundColor White
Write-Host "  - Auditoria de Caminhos Firestore (empresas/ID/entidades/...)" -ForegroundColor White
Write-Host "  - Espera ativa por custom claims (Wait & Retry)" -ForegroundColor White
Write-Host "  - Stack traces e excecoes" -ForegroundColor White
Write-Host ""
Write-Host "Aguardando eventos de sincronizacao, login e exclusao..." -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Gray
Write-Host ""

# Capturar logs com filtros especificos (usando o mesmo padrao do script que funcionava)
& $ADB logcat -v time -s RoutesViewModel:* RoutesFragment:* SyncRepository:* UserSessionManager:* AuthViewModel:* LoginFragment:* FirebaseAuth:* AppRepository:* CycleExpensesViewModel:* CycleExpensesFragment:* | ForEach-Object {
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
    # Novos logs de Diagnostico 2025 - Claims e CompanyId
    elseif ($line -match "DIAG:.*Waiting for companyId|DIAG:.*Verificando claims|DIAG:.*Claim 'companyId' confirmada|DIAG:.*Claims atuais no Token") {
        Write-Host $line -ForegroundColor Magenta -BackgroundColor DarkBlue
    }
    elseif ($line -match "DIAG:.*Empresa identificada:|DIAG:.*Current Company ID in Sync:") {
        Write-Host $line -ForegroundColor Yellow -BackgroundColor DarkCyan
    }
    elseif ($line -match "DIAG:.*getCollectionRef|DIAG:.*Full Path:") {
        Write-Host $line -ForegroundColor Cyan -BackgroundColor Black
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
    # Logs de processamento da fila de sincronizacao
    elseif ($line -match "SyncRepository.*PROCESSANDO FILA|SyncRepository.*processSyncQueue.*INICIADO|SyncRepository.*Fila de sincronizacao") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "SyncRepository.*Estado da fila|SyncRepository.*operacoes pendentes|SyncRepository.*operacoes falhadas") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "SyncRepository.*Buscando operacoes pendentes|SyncRepository.*Operacoes encontradas") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "SyncRepository.*Processando lote|SyncRepository.*lote.*operacoes") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "SyncRepository.*Fila completamente processada|SyncRepository.*total sucesso|SyncRepository.*total falhas") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "SyncRepository.*Nenhuma operacao pendente|SyncRepository.*Fila vazia") {
        Write-Host $line -ForegroundColor DarkGray
    }
    elseif ($line -match "SyncRepository.*Operacao.*tipo=|SyncRepository.*Operation ID:") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    # Logs de operacoes individuais de sincronizacao
    elseif ($line -match "SyncRepository.*Processando operacao unica|SyncRepository.*processSingleSyncOperation|SyncRepository.*Processando operacao:") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "SyncRepository.*Tipo:|SyncRepository.*Entidade:|SyncRepository.*Document ID:|SyncRepository.*Collection Path:|SyncRepository.*Collection Name:") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "SyncRepository.*Mapeamento de entidade|SyncRepository.*colecao") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    # Logs de DELETE especificos
    elseif ($line -match "SyncRepository.*INICIANDO DELETE|SyncRepository.*Executando DELETE|SyncRepository.*DELETE executado|SyncRepository.*deletar documento|SyncRepository.*DELETE FINALIZADO") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "SyncRepository.*Documento encontrado no Firestore|SyncRepository.*Documento.*nao existe no Firestore|SyncRepository.*Verificando existencia do documento") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "SyncRepository.*Confirmado: Documento foi deletado|SyncRepository.*Documento ainda existe apos DELETE") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "SyncRepository.*Erro ao deletar documento|SyncRepository.*DELETE.*falhou|SyncRepository.*Erro do Firestore ao deletar") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "SyncRepository.*Codigo:|SyncRepository.*Caminho:|SyncRepository.*Document Path completo") {
        Write-Host $line -ForegroundColor Yellow
    }
    # Logs de despesas - Push e Pull
    elseif ($line -match "SyncRepository.*Push.*Despesas|SyncRepository.*pushDespesas|SyncRepository.*Enviando despesas") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "SyncRepository.*Pull.*Despesas|SyncRepository.*pullDespesas|SyncRepository.*Importando despesas") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "SyncRepository.*Despesa.*sincronizada|SyncRepository.*Despesa inserida|SyncRepository.*Despesa atualizada") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "SyncRepository.*Processando despesa:|SyncRepository.*ID=.*Descricao=") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "SyncRepository.*ERRO.*despesa|SyncRepository.*Erro ao.*despesa|SyncRepository.*Despesa.*falhou") {
        Write-Host $line -ForegroundColor Red
    }
    # Logs do AppRepository - Operacoes DELETE
    elseif ($line -match "AppRepository.*Despesa deletada localmente|AppRepository.*Operacao DELETE enfileirada|AppRepository.*DELETE.*Despesa|AppRepository.*Operacao inserida na fila.*DELETE") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "AppRepository.*CRITICO.*SyncOperationDao|AppRepository.*Erro ao enfileirar DELETE|AppRepository.*DELETE.*falhou|AppRepository.*ERRO CRITICO") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "AppRepository.*deletarDespesa|AppRepository.*Deletar.*despesa") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "AppRepository.*OperationID=") {
        Write-Host $line -ForegroundColor Green
    }
    # Logs do CycleExpensesViewModel e Fragment - Exclusao de despesas
    elseif ($line -match "CycleExpensesViewModel.*removerDespesa|CycleExpensesViewModel.*deletarDespesa|CycleExpensesViewModel.*Excluir despesa") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "CycleExpensesViewModel.*Despesa removida|CycleExpensesViewModel.*Despesa deletada|CycleExpensesViewModel.*Exclusao.*sucesso") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "CycleExpensesViewModel.*ERRO.*despesa|CycleExpensesViewModel.*Erro ao.*despesa|CycleExpensesViewModel.*Exclusao.*falhou") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "CycleExpensesFragment.*excluir|CycleExpensesFragment.*Excluir|CycleExpensesFragment.*confirmarExclusao") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "CycleExpensesFragment.*ERRO|CycleExpensesFragment.*Erro|CycleExpensesFragment.*Exception") {
        Write-Host $line -ForegroundColor Red
    }
    # Logs de Firestore - Operacoes DELETE
    elseif ($line -match "Firestore.*delete|Firestore.*DELETE|Firestore.*deletar") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "Firestore.*PERMISSION_DENIED|Firestore.*permission denied") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "Firestore.*NOT_FOUND|Firestore.*not found|Firestore.*nao encontrado") {
        Write-Host $line -ForegroundColor Yellow
    }
    # Logs gerais de sincronizacao bidirecional
    elseif ($line -match "SyncRepository.*INICIANDO SINCRONIZACAO|SyncRepository.*SINCRONIZACAO.*CONCLUIDA|SyncRepository.*syncBidirectional|SyncRepository.*Push concluido|SyncRepository.*Pull concluido") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "SyncRepository.*Total enviado|SyncRepository.*Total sincronizado|SyncRepository.*Total de falhas|SyncRepository.*Timestamp:") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "SyncRepository.*Passo 1.*PUSH|SyncRepository.*Passo 2.*PULL") {
        Write-Host $line -ForegroundColor Cyan
    }
    # Logs de contagem e estado da fila
    elseif ($line -match "SyncRepository.*Total de operacoes pendentes|SyncRepository.*contarOperacoesSyncPendentes|SyncRepository.*operacoes na fila") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "SyncRepository.*Nenhuma operacao pendente na fila|SyncRepository.*encerrando processamento") {
        Write-Host $line -ForegroundColor DarkGray
    }
    # Logs de erros especificos do Firestore
    elseif ($line -match "SyncRepository.*FirebaseFirestoreException|SyncRepository.*PERMISSION_DENIED|SyncRepository.*permission denied") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "SyncRepository.*NOT_FOUND|SyncRepository.*not found|SyncRepository.*nao encontrado") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "SyncRepository.*Codigo do erro:|SyncRepository.*Mensagem do erro:|SyncRepository.*Firestore error code") {
        Write-Host $line -ForegroundColor Red
    }
    # Logs de dados da operacao
    elseif ($line -match "SyncRepository.*entityData:|SyncRepository.*Dados da operacao:|SyncRepository.*Keys do documento:") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    # Logs de retry e tentativas
    elseif ($line -match "SyncRepository.*tentativa|SyncRepository.*retry|SyncRepository.*maxRetries") {
        Write-Host $line -ForegroundColor Yellow
    }
    # Logs de verificacao pos-DELETE
    elseif ($line -match "SyncRepository.*Verificando apos DELETE|SyncRepository.*Documento ainda existe|SyncRepository.*Confirmado: Documento foi deletado") {
        Write-Host $line -ForegroundColor Green
    }
    # Logs de resolucao de colecao
    elseif ($line -match "SyncRepository.*resolveCollectionReference|SyncRepository.*Mapeando entidade|SyncRepository.*para colecao") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    # Logs de batch processing
    elseif ($line -match "SyncRepository.*Processando batch|SyncRepository.*batch.*sucesso|SyncRepository.*batch.*falha") {
        Write-Host $line -ForegroundColor Yellow
    }
    # Logs de AppRepository - Detalhes da operacao
    elseif ($line -match "AppRepository.*OperationID=|AppRepository.*Operacao inserida na fila|AppRepository.*Tipo=.*Entidade=") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "AppRepository.*CRITICO.*SyncOperationDao|AppRepository.*SyncOperationDao.*null") {
        Write-Host $line -ForegroundColor Red
    }
    # Logs de CycleExpensesViewModel - Detalhes
    elseif ($line -match "CycleExpensesViewModel.*ID da despesa|CycleExpensesViewModel.*Despesa.*ID=") {
        Write-Host $line -ForegroundColor Cyan
    }
    # Logs de stack traces e excecoes gerais
    elseif ($line -match ".*Stack trace:|.*Exception:|.*ERROR:|.*CRITICO:|.*FATAL:") {
        Write-Host $line -ForegroundColor Red
    }
    else {
        Write-Host $line
    }
}

