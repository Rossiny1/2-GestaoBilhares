@echo off
REM Script batch para monitorar mudanças remotas e instalar automaticamente
REM Uso: scripts\watch-and-install.bat
REM Este script roda em loop verificando mudanças a cada 30 segundos

echo 👀 Monitorando mudanças remotas (Ctrl+C para parar)...
echo ⏱️  Verificando a cada 30 segundos...
echo.

:loop
git fetch origin >nul 2>&1
git pull origin >nul 2>&1

if errorlevel 1 (
    echo ❌ Erro ao sincronizar. Continuando monitoramento...
    timeout /t 30 /nobreak >nul
    goto loop
)

echo 🔨 Compilando e instalando app...
call gradlew.bat installDebug

if errorlevel 1 (
    echo ❌ Erro ao instalar. Continuando monitoramento...
) else (
    echo ✅ App atualizado e instalado com sucesso!
)

timeout /t 30 /nobreak >nul
goto loop
