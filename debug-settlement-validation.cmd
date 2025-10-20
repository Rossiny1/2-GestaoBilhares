@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Descobre diretório do script
set SCRIPT_DIR=%~dp0

REM Caminho do PowerShell
set POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe

REM Script PS1
set PS1_FILE=%SCRIPT_DIR%scripts\debug-settlement-validation.ps1

if not exist "%PS1_FILE%" (
  echo Script nao encontrado: %PS1_FILE%
  exit /b 1
)

"%POWERSHELL%" -NoProfile -ExecutionPolicy Bypass -File "%PS1_FILE%"

endlocal
