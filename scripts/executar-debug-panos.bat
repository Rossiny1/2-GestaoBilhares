@echo off
echo === DEBUG DE PANOS NO ESTOQUE ===
echo Executando script PowerShell...
echo.
powershell -ExecutionPolicy Bypass -File "%~dp0debug-panos-estoque.ps1"
pause
