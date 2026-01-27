@echo off
REM ========================================================================
REM Script para capturar logs do fluxo de troca de pano - Cards Acerto V14
REM Projeto: Gestão de Bilhares (com.example.gestaobilhares)
REM Objetivo: Identificar por que cards de Acerto não aparecem em "Reforma de Mesas"
REM ========================================================================

echo.
echo === CAPTURA DE LOGS - CARDS ACERTO V14 ===
echo.

REM Configuracao ADBPATH - AJUSTE SE NECESSARIO
REM Exemplos:
REM   set ADBPATH=C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\adb.exe
REM   set ADBPATH=c:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe
if "%ADBPATH%"=="" (
    echo ERRO: Variavel ADBPATH nao configurada!
    echo.
    echo Configure ADBPATH antes de executar:
    echo   set ADBPATH=C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\adb.exe
    echo   ou
    echo   set ADBPATH=c:\Users\Rossiny\Desktop\2-GestaoBilhares\android-sdk\platform-tools\adb.exe
    echo.
    pause
    exit /b 1
)

echo ADBPATH: %ADBPATH%
echo.

REM 1. Validar dispositivo conectado
echo [1/6] Verificando dispositivo conectado...
%ADBPATH% devices | findstr "device" >nul
if errorlevel 1 (
    echo ERRO: Nenhum dispositivo conectado!
    echo Conecte um dispositivo USB ou inicie um emulador.
    echo.
    pause
    exit /b 1
)
echo Dispositivo encontrado: OK
echo.

REM 2. Limpar logs anteriores
echo [2/6] Limpando logs anteriores...
%ADBPATH% logcat -c
echo Logs limpos: OK
echo.

REM 3. Gerar timestamp
echo [3/6] Gerando timestamp para arquivos...
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YYYY=%dt:~0,4%"
set "MM=%dt:~4,2%"
set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%"
set "Min=%dt:~10,2%"
set "Sec=%dt:~12,2%"
set "timestamp=%YYYY%%MM%%DD%_%HH%%Min%%Sec%"
echo Timestamp gerado: %timestamp%
echo.

REM 4. Iniciar captura de logs em tempo real
echo [4/6] Iniciando captura de logs em tempo real...
echo.
echo === FILTROS ATIVOS ===
echo   - DEBUG_CARDS (logs do fluxo de troca de pano)
echo   - Erros do app (BaseViewModel, AppRepository, SettlementViewModel)
echo   - Erros gerais (AndroidRuntime)
echo.
echo === INSTRUCOES ===
echo Execute os seguintes passos no app:
echo.
echo 1. Nova Reforma (baseline):
echo    - Abra o app
echo    - Va em Mesas ^> Nova Reforma
echo    - Selecione uma mesa (ex: M01)
echo    - Marque "Panos" e escolha um pano
echo    - Salve a reforma
echo.
echo 2. Acerto (problema):
echo    - Va em Acerto
echo    - Selecione um cliente
echo    - Adicione uma mesa (ex: M02)
echo    - Marque "Trocar Pano" e informe o pano
echo    - Salve o acerto
echo.
echo 3. Verificar resultado:
echo    - Abra a tela "Reforma de Mesas"
echo    - Verifique se ambos os cards aparecem
echo.
echo Pressione Ctrl+C para parar a captura quando terminar.
echo.
echo === CAPTURANDO LOGS (aguarde) ===
echo.

REM Capturar logs em tempo real ate Ctrl+C
%ADBPATH% logcat -v time DEBUG_CARDS:D BaseViewModel:E AppRepository:E SettlementViewModel:E AndroidRuntime:E *:S

echo.
echo [5/6] Captura interrompida. Salvando logs em arquivos...

REM 5. Salvar logs em arquivos com timestamp
echo Salvando DEBUG_CARDS...
%ADBPATH% logcat -d | findstr "DEBUG_CARDS" > logs_debug_cards_%timestamp%.txt

echo Salvando erros gerais...
%ADBPATH% logcat -d | findstr "E/" > logs_errors_%timestamp%.txt

echo Logs salvos:
echo   - logs_debug_cards_%timestamp%.txt
echo   - logs_errors_%timestamp%.txt
echo.

REM 6. (Opcional) Dump do banco Room
echo [6/6] Deseja fazer dump do banco Room? (S/N)
set /p dump_choice=Digite sua escolha: 
if /i "%dump_choice%"=="S" (
    echo.
    echo Fazendo dump do banco Room...
    
    echo Exportando mesas_reformadas...
    %ADBPATH% shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db \"SELECT mesa_id, numero_mesa, observacoes, numero_panos, data_reforma FROM mesas_reformadas ORDER BY data_reforma DESC LIMIT 20;\"" > query_mesas_reformadas_%timestamp%.txt
    
    echo Exportando historico_manutencao_mesa...
    %ADBPATH% shell "run-as com.example.gestaobilhares sqlite3 /data/data/com.example.gestaobilhares/databases/gestaobilhares.db \"SELECT mesa_id, numero_mesa, responsavel, descricao, data_manutencao FROM historico_manutencao_mesa ORDER BY data_manutencao DESC LIMIT 20;\"" > query_historico_manutencao_%timestamp%.txt
    
    echo Queries do banco salvas:
    echo   - query_mesas_reformadas_%timestamp%.txt
    echo   - query_historico_manutencao_%timestamp%.txt
)

echo.
echo === CAPTURA CONCLUIDA ===
echo.
echo Arquivos gerados:
echo   - logs_debug_cards_%timestamp%.txt
echo   - logs_errors_%timestamp%.txt
if /i "%dump_choice%"=="S" (
    echo   - query_mesas_reformadas_%timestamp%.txt
    echo   - query_historico_manutencao_%timestamp%.txt
)
echo.
echo Analise os arquivos para identificar o problema dos cards de Acerto.
echo.
pause
