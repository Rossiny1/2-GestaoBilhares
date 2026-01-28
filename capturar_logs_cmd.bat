@echo off
echo Iniciando captura de logs do Gestao Bilhares...
echo.

REM Verificar dispositivo
echo Dispositivos conectados:
"C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
echo.

REM Limpar logs
echo Limpando logs antigos...
"C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -c

echo.
echo Capturando logs com tag: SETTLEMENT
echo Pressione Ctrl+C para parar
echo.

REM Capturar logs
"C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat | findstr /i "SETTLEMENT"

pause
