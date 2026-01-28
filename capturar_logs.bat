@echo off
echo Iniciando captura de logs do app Gestao Bilhares...
echo.
echo Dispositivo conectado:
"C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
echo.
echo Filtrando logs com tag: SETTLEMENT
echo Pressione Ctrl+C para parar
echo.
"C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat | findstr "SETTLEMENT"
pause
