@echo off
echo 🔍 Testando exibição do ciclo 4...
echo Script executado em %DATE% %TIME%
echo.

REM Configurações
set PACKAGE_NAME=com.example.gestaobilhares
set ADB_COMMAND=adb

REM Verificar se ADB está disponível
echo 1. Verificando ADB...
%ADB_COMMAND% version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ADB não encontrado no PATH.
    echo    Verifique se o Android SDK está instalado.
    pause
    exit /b 1
)
echo ✅ ADB encontrado
echo.

REM Verificar dispositivos conectados
echo 2. Verificando dispositivos conectados...
%ADB_COMMAND% devices > temp_devices.txt 2>&1
findstr /C:"device" temp_devices.txt | findstr /V /C:"List" > temp_device_count.txt
for /f %%i in ('type temp_device_count.txt ^| find /c "device"') do set DEVICE_COUNT=%%i

if %DEVICE_COUNT% EQU 0 (
    echo ❌ Nenhum dispositivo Android conectado.
    echo    Conecte um dispositivo ou inicie um emulador.
    del temp_devices.txt temp_device_count.txt 2>nul
    pause
    exit /b 1
)
echo ✅ Dispositivo conectado
del temp_devices.txt temp_device_count.txt 2>nul
echo.

REM Verificar se o APK está instalado
echo 3. Verificando se o APK está instalado...
%ADB_COMMAND% shell pm list packages %PACKAGE_NAME% > temp_packages.txt 2>&1
findstr /C:"%PACKAGE_NAME%" temp_packages.txt >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ APK não está instalado.

    REM Procurar APK no diretório do projeto
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        echo 📦 Instalando APK...
        %ADB_COMMAND% install -r app\build\outputs\apk\debug\app-debug.apk
        if %ERRORLEVEL% EQU 0 (
            echo ✅ APK instalado com sucesso
        ) else (
            echo ❌ Falha na instalação do APK
            del temp_packages.txt 2>nul
            pause
            exit /b 1
        )
    ) else (
        echo ❌ APK não encontrado em app\build\outputs\apk\debug\app-debug.apk
        echo    Execute primeiro: gradlew assembleDebug
        del temp_packages.txt 2>nul
        pause
        exit /b 1
    )
) else (
    echo ✅ APK já está instalado
)
del temp_packages.txt 2>nul
echo.

REM Limpar logs anteriores
echo 4. Limpando logs anteriores...
%ADB_COMMAND% logcat -c
echo.

REM Executar sincronização
echo 5. Executando sincronização...
%ADB_COMMAND% shell am start -n "%PACKAGE_NAME%/.ui.auth.AuthActivity"
echo.

echo 6. Aguardando sincronização completar...
echo    Aguardando 10 segundos...
timeout /t 10 /nobreak >nul
echo.

REM Capturar logs de sincronização
echo 7. Capturando logs de sincronização...
%ADB_COMMAND% logcat -d -s SyncRepository RoutesViewModel > temp_logs.txt 2>&1
findstr /C:"Ciclo ID=4" temp_logs.txt > temp_relevant.txt 2>&1
findstr /C:"ciclo 4" temp_logs.txt >> temp_relevant.txt 2>&1
findstr /C:"numeroCiclo=4" temp_logs.txt >> temp_relevant.txt 2>&1
findstr /C:"Sincroniza" temp_logs.txt | findstr /C:"conclu" | findstr /C:"sucesso" >> temp_relevant.txt 2>&1
findstr /C:"Rota" temp_logs.txt | findstr /C:"atualizada" | findstr /C:"ciclo" >> temp_relevant.txt 2>&1

if exist temp_relevant.txt (
    for /f "tokens=*" %%i in (temp_relevant.txt) do (
        echo 📋 %%i
    )
) else (
    echo ⚠️  Nenhum log específico do ciclo 4 encontrado
)
echo.

REM Verificar dados no banco
echo 8. Verificando dados no banco...
echo    Ciclos de acerto (rota 1):
%ADB_COMMAND% shell "run-as %PACKAGE_NAME% sqlite3 -header -column /data/data/%PACKAGE_NAME%/databases/gestao_bilhares.db 'SELECT id, numero_ciclo, status FROM ciclos_acerto WHERE rota_id = 1 ORDER BY numero_ciclo DESC LIMIT 5;'" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo    ❌ Erro ao consultar banco ou tabela não existe
)
echo.

echo    Dados da rota 1:
%ADB_COMMAND% shell "run-as %PACKAGE_NAME% sqlite3 -header -column /data/data/%PACKAGE_NAME%/databases/gestao_bilhares.db 'SELECT id, nome, ciclo_acerto_atual, status_atual FROM rotas WHERE id = 1;'" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo    ❌ Erro ao consultar banco ou tabela não existe
)
echo.

REM Verificar processos em execução
echo 9. Verificando se o app está rodando...
%ADB_COMMAND% shell ps | findstr /C:"%PACKAGE_NAME%" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ App está em execução
) else (
    echo ❌ App não está em execução
)
echo.

echo ✅ Teste concluído!
echo 📝 Verifique os logs acima para confirmar se o ciclo 4 está sendo exibido corretamente.
echo.
echo 💡 Se o ciclo 4 ainda não aparecer:
echo    1. Reinicie o app completamente
echo    2. Execute sincronização manual
echo    3. Verifique conexão com internet
echo.

REM Limpar arquivos temporários
del temp_logs.txt temp_relevant.txt 2>nul

pause
