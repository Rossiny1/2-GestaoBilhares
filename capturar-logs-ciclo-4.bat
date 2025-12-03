@echo off
echo === CAPTURA DE LOGS - CICLO 4 ===
echo Objetivo: Analisar exibicao do ciclo 4 na interface
echo Data/Hora: %DATE% %TIME%
echo.

REM Caminho do ADB
set ADB_PATH=C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\adb.exe

REM Verificar se o ADB existe
if not exist "%ADB_PATH%" (
    echo ❌ ADB nao encontrado em: %ADB_PATH%
    echo Certifique-se de que o Android SDK esta instalado corretamente
    pause
    exit /b 1
)

REM Verificar se ha dispositivo conectado
echo 🔍 Verificando dispositivos conectados...
%ADB_PATH% devices > temp_devices.txt 2>&1
findstr /C:"device" temp_devices.txt >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Dispositivo encontrado!
) else (
    echo ❌ Nenhum dispositivo conectado!
    echo 💡 Conecte um dispositivo USB ou inicie um emulador
    del temp_devices.txt 2>nul
    pause
    exit /b 1
)
del temp_devices.txt 2>nul

REM Limpar logs anteriores
echo.
echo 🧹 Limpando logs anteriores...
%ADB_PATH% logcat -c
echo ✅ Logs limpos

REM Iniciar captura de logs
echo.
echo ========================================
echo      CAPTURANDO LOGS DO CICLO 4
echo ========================================
echo.
echo 🎯 Filtros ativos para CICLO 4:
echo   • SyncRepository (todos os logs)
echo   • RoutesViewModel (todos os logs)
echo   • RoutesFragment (todos os logs)
echo   • Ciclo ID=4 especifico
echo   • numeroCiclo=4 especifico
echo   • rotaId=1 (rota padrao)
echo   • cicloAcertoAtual=4
echo.
echo ⏳ Aguardando eventos de sincronizacao...
echo 💡 Pressione Ctrl+C para parar a captura
echo.

REM Capturar logs com filtros especificos para o ciclo 4
%ADB_PATH% logcat -v time -s SyncRepository:* RoutesViewModel:* RoutesFragment:* > logs_ciclo_4_%DATE:~-4,4%%DATE:~-10,2%%DATE:~-7,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%.txt 2>&1

echo.
echo ✅ Captura concluida! Arquivo salvo como: logs_ciclo_4_%DATE:~-4,4%%DATE:~-10,2%%DATE:~-7,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%.txt
echo.
echo 📊 Para analisar os logs automaticamente, execute:
echo    analisar-logs-ciclo-4.bat "logs_ciclo_4_%DATE:~-4,4%%DATE:~-10,2%%DATE:~-7,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%.txt"
echo.
pause
